/*
 * Created by Muhammad Utsman on 13/12/20 11:32 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.data.model.dto.entity

import androidx.lifecycle.asFlow
import com.utsman.abstraction.extensions.getValueOf
import com.utsman.abstraction.extensions.logi
import com.utsman.data.di._appsRepository
import com.utsman.data.di._workManager
import com.utsman.data.model.dto.downloaded.AppStatus
import com.utsman.data.model.dto.downloaded.DownloadedApps
import com.utsman.data.model.dto.list.toAppsView
import com.utsman.data.utils.DownloadUtils
import com.utsman.network.toJson
import kotlinx.coroutines.flow.collect
import java.util.*

private val workManager = getValueOf(_workManager)
private val appsRepository = getValueOf(_appsRepository)

suspend fun CurrentDownloadEntity.toDownloadedApps(): DownloadedApps {
    val id = "id_${this.packageName}"
    val name = this.name
    val downloadId = this.downloadId
    val workInfo = workManager.getWorkInfoByIdLiveData(UUID.fromString(this.uuid))

    val appsFoundApiService = appsRepository.getSearchApps(this.packageName, 0)
    logi("apps found --> ${appsFoundApiService.toJson()}")
    val appsFound =
        appsFoundApiService.datalist?.list?.find { i -> i.`package` == this.packageName }
            ?.toAppsView()

    val appStatus: AppStatus = when {
        this.isRun -> {
            AppStatus.RUNNING
        }
        else -> if (DownloadUtils.checkAppIsInstalled(this.packageName)) {
            AppStatus.INSTALLED
        } else {
            AppStatus.DOWNLOADED
        }
    }

    return DownloadedApps(
        id = id,
        name = name,
        downloadId = downloadId,
        workInfoLiveData = workInfo,
        appsView = appsFound,
        isRun = this.isRun,
        appStatus = appStatus,
        fileName = this.fileName ?: ""
    )
}