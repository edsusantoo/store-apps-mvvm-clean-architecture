/*
 * Created by Muhammad Utsman on 28/11/20 3:54 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.storeapps

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import com.utsman.abstraction.base.GlideApp
import com.utsman.data.di.*
import com.utsman.data.repository.database.DownloadedRepository
import com.utsman.data.repository.list.AppsRepository
import com.utsman.data.repository.list.InstalledAppsRepository
import com.utsman.network.di._jsonBeautifier
import com.utsman.network.di._moshi
import com.utsman.network.utils.JsonBeautifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var jsonBeautifier: JsonBeautifier

    @Inject
    lateinit var downloadManager: DownloadManager

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var appsRepository: AppsRepository

    @Inject
    lateinit var downloadedRepository: DownloadedRepository

    @Inject
    lateinit var installedAppsRepository: InstalledAppsRepository

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // provide manual di
        _moshi.value = moshi
        _jsonBeautifier.value = jsonBeautifier
        _context.value = this
        _dataStore.value = provideDataStore(this)
        _downloadManager.value = downloadManager
        _workManager.value = workManager
        _appsRepository.value = appsRepository
        _downloadedRepository.value = downloadedRepository
        _installedAppsRepository.value = installedAppsRepository
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GlideApp.get(this).onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        GlideApp.get(this).onTrimMemory(level)
    }
}