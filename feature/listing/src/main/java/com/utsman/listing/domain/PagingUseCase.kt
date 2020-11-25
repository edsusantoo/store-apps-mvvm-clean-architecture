package com.utsman.listing.domain

import androidx.lifecycle.MutableLiveData
import androidx.paging.*
import com.utsman.data.model.dto.AppsSealedView.AppsView
import com.utsman.data.model.dto.toAppsView
import com.utsman.data.repository.PagingAppRepository
import com.utsman.data.source.AppsPagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PagingUseCase(private val pagingAppRepository: PagingAppRepository) {
    val pagingData = MutableLiveData<PagingData<AppsView>>()

    fun searchApps(scope: CoroutineScope, query: String? = null, isSearch: Boolean) = scope.launch {
        Pager(PagingConfig(pageSize = 10)) {
            AppsPagingSource(query, isSearch, pagingAppRepository)
        }.flow
            .cachedIn(this)
            .collect {
                val appsViewPaging = it.mapSync { ap ->
                    ap.toAppsView()
                }
                pagingData.postValue(appsViewPaging)
            }
    }

    fun restartState(scope: CoroutineScope) = scope.run {
        pagingData.postValue(PagingData.empty())
    }
}