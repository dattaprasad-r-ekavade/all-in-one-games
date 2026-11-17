plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(17)
}

// Release signing and the version code come from the owner's ~/.gradle/gradle.properties.
// Nothing secret is stored in this repository. Without all four signing properties, the release bundle is unsigned.
val uploadStoreFile = providers.gradleProperty("witgridUploadStoreFile").orNull?.takeIf(String::isNotBlank)
val uploadStorePassword = providers.gradleProperty("witgridUploadStorePassword").orNull?.takeIf(String::isNotBlank)
val uploadKeyAlias = providers.gradleProperty("witgridUploadKeyAlias").orNull?.takeIf(String::isNotBlank)
val uploadKeyPassword = providers.gradleProperty("witgridUploadKeyPassword").orNull?.takeIf(String::isNotBlank)
val releaseVersionCode: Int = providers.gradleProperty("witgridVersionCode").orNull?.toInt() ?: 1

android {
    namespace = "com.datathecodie.witgrid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.datathecodie.witgrid"
        minSdk = 24
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadStoreFile != null &&
            uploadStorePassword != null &&
            uploadKeyAlias != null &&
            uploadKeyPassword != null
        ) {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.gma.next.gen)
    implementation(libs.ump)
    implementation(libs.billing)
    ksp(libs.room.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}
