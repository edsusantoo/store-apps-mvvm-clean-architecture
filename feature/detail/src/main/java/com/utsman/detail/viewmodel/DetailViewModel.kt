/*
 * Created by Muhammad Utsman on 28/11/20 5:00 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.detail.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Operation
import com.utsman.abstraction.interactor.ResultState
import com.utsman.data.model.dto.detail.DetailView
import com.utsman.data.model.dto.worker.FileDownload
import com.utsman.data.model.dto.worker.WorkerAppsMap
import com.utsman.data.store.CurrentWorkerPreferences
import com.utsman.detail.domain.DetailUseCase
import kotlinx.coroutines.launch

class DetailViewModel @ViewModelInject constructor(
    private val detailUseCase: DetailUseCase
) :
    ViewModel() {

    private val _detailView = detailUseCase.detailView

    val detailView: LiveData<ResultState<DetailView>>
        get() = _detailView.asLiveData(viewModelScope.coroutineContext)

    val workerState: LiveData<Operation.State?>
        get() = detailUseCase.workerState.asLiveData(viewModelScope.coroutineContext)

    val saveApps: LiveData<List<WorkerAppsMap>>
        get() = detailUseCase.currentApps.asLiveData(viewModelScope.coroutineContext)

    val workStateResult
        get() = detailUseCase.workInfoState.asLiveData(viewModelScope.coroutineContext)

    fun getDetailView(packageName: String) = viewModelScope.launch {
        detailUseCase.getDetail(this, packageName)
    }

    fun requestDownload(fileDownload: FileDownload) = viewModelScope.launch {
        detailUseCase.requestDownload(viewModelScope, fileDownload)
    }

    fun observerWorkInfo(packageName: String) = viewModelScope.launch {
        detailUseCase.observerWorkInfoResult(this, packageName)
    }

    fun restartState() = viewModelScope.launch {
        detailUseCase.restartState(this)
    }
}