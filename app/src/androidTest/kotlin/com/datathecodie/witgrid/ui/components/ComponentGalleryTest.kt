package com.datathecodie.witgrid.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ComponentGalleryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun capturesEveryPreviewInLightAndDarkAtAllFontScales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "u3-components").apply { mkdirs() }
        var sample by mutableStateOf(ComponentSample.CARD)
        var dark by mutableStateOf(false)
        var fontScale by mutableStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                WitgridTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.width(360.dp).testTag("sample"),
                            color = WitgridTheme.colors.paper,
                            contentColor = WitgridTheme.colors.ink,
                        ) {
                            ComponentSampleContent(sample, Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
        listOf(1f, 1.3f, 2f).forEach { scale ->
            listOf(false, true).forEach { darkTheme ->
                ComponentSample.entries.forEach { component ->
                    composeRule.runOnIdle {
                        sample = component
                        dark = darkTheme
                        fontScale = scale
                    }
                    composeRule.waitForIdle()
                    val bitmap = composeRule.onNodeWithTag("sample").captureToImage().asAndroidBitmap()
                    assertEquals(with(composeRule.density) { 360.dp.roundToPx() }, bitmap.width)
                    val name = "${component.name.lowercase()}-${if (darkTheme) "dark" else "light"}-$scale.png"
                    File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        }
        assertEquals(60, directory.listFiles()?.count { it.extension == "png" && !it.name.startsWith("components-") })
        listOf(1f, 1.3f, 2f).forEach { scale ->
            createSheet(directory, scale)
            publishSheet(context, File(directory, "components-$scale.png"))
        }
    }

    @Test
    fun switchRowIsOneAccessibleTargetAndSegmentsUpdateSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var checked by mutableStateOf(false)
        var selected by mutableStateOf(0)
        composeRule.setContent {
            WitgridTheme {
                Column {
                    ListRow(stringResource(R.string.settings_haptics), checked = checked, onCheckedChange = { checked = it })
                    SegmentedControl(
                        listOf(stringResource(R.string.settings_theme_light), stringResource(R.string.settings_theme_dark)),
                        selected, { selected = it },
                    )
                }
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.settings_haptics)).assertIsOff().performClick().assertIsOn()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsSelected()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_dark)).performClick().assertIsSelected()
    }

    @Test
    fun disabledToolDoesNotSendAnEventAndPrimaryTargetIsAtLeast54Dp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var clicks = 0
        composeRule.setContent {
            WitgridTheme {
                Column {
                    PrimaryButton(stringResource(R.string.button_next), { clicks++ }, Modifier.testTag("primary"))
                    ToolBarItem(painterResource(R.drawable.ic_back), stringResource(R.string.button_undo), { clicks++ }, enabled = false)
                }
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.button_undo)).assertIsNotEnabled().performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(0, clicks) }
        val primary = composeRule.onNodeWithTag("primary").assertHasClickAction()
        val minimumHeight = with(composeRule.density) { 54.dp.toPx() }
        check(primary.fetchSemanticsNode().boundsInRoot.height >= minimumHeight)
        primary.performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    private fun createSheet(directory: File, fontScale: Float) {
        val gap = 24
        val headingHeight = 64
        val samples = ComponentSample.entries.map { sample ->
            listOf("light", "dark").map { theme ->
                BitmapFactory.decodeFile(File(directory, "${sample.name.lowercase()}-$theme-$fontScale.png").path)
            }
        }
        val columnWidth = samples.first().first().width
        val height = headingHeight + samples.sumOf { row -> row.maxOf { it.height } + headingHeight + gap }
        val sheet = Bitmap.createBitmap(columnWidth * 2 + gap * 3, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(225, 225, 225))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 30f }
        canvas.drawText("Light / dark - 360 dp - font $fontScale", gap.toFloat(), 42f, paint)
        var top = headingHeight
        samples.forEachIndexed { index, row ->
            canvas.drawText(ComponentSample.entries[index].name, gap.toFloat(), top + 42f, paint)
            top += headingHeight
            row.forEachIndexed { column, bitmap ->
                canvas.drawBitmap(bitmap, (gap + column * (columnWidth + gap)).toFloat(), top.toFloat(), null)
            }
            top += row.maxOf { it.height } + gap
            row.forEach(Bitmap::recycle)
        }
        File(directory, "components-$fontScale.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sheet.recycle()
    }

    @RequiresApi(29)
    private fun publishSheet(context: Context, file: File) {
        val directory = "Pictures/WitgridU3Previews/"
        val resolver = context.contentResolver
        resolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(file.name, directory),
        )
        val uri = checkNotNull(resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            },
        ))
        checkNotNull(resolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    }
}
