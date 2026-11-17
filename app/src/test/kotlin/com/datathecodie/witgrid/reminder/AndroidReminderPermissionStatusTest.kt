package com.datathecodie.witgrid.reminder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AndroidReminderPermissionStatusTest {
    @Test
    @Config(sdk = [32])
    fun notificationPermissionIsNotRequiredBeforeAndroid13() {
        val status = AndroidReminderPermissionStatus(RuntimeEnvironment.getApplication())

        assertTrue(status.isGranted())
    }

    @Test
    @Config(sdk = [33])
    fun notificationPermissionMustBeGrantedOnAndroid13AndLater() {
        val status = AndroidReminderPermissionStatus(RuntimeEnvironment.getApplication())

        assertFalse(status.isGranted())
    }
}
