package com.datathecodie.witgrid

import android.app.Application
import com.datathecodie.witgrid.ads.GoogleAdService
import com.datathecodie.witgrid.billing.PlayBillingService
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import com.datathecodie.witgrid.nav.AppNavigator
import com.datathecodie.witgrid.ui.screens.play.PlayPersistenceScope
import kotlinx.coroutines.Dispatchers

/** Process-scoped navigator, storage, ads and billing so rotation does not rebuild them. */
class WitgridApp : Application() {
    val navigator by lazy { AppNavigator() }
    val playerDataRepository by lazy { DataStorePlayerDataRepository(this) }
    val database by lazy { WitgridDatabase.create(this) }
    val billingService by lazy { PlayBillingService(this, playerDataRepository) }
    val adService by lazy { GoogleAdService(this) }
    val playPersistence by lazy { PlayPersistenceScope(Dispatchers.Main.immediate) }
}
