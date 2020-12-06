/*
 * Created by Muhammad Utsman on 28/11/20 3:54 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.home.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.PagingData
import com.utsman.abstraction.interactor.ResultState
import com.utsman.data.model.dto.list.AppsSealedView.AppsView
import com.utsman.data.model.dto.list.CategorySealedView
import com.utsman.home.domain.HomeUseCase
import kotlinx.coroutines.launch

class HomeViewModel @ViewModelInject constructor(
    private val homeUseCase: HomeUseCase
) : ViewModel() {

    private val _randomList = homeUseCase.randomList
    private val _categories = homeUseCase.categories

    val randomList: LiveData<ResultState<List<AppsView>>>
        get() = _randomList.asLiveData(viewModelScope.coroutineContext)

    val categories: LiveData<ResultState<List<CategorySealedView>>>
        get() = _categories.asLiveData(viewModelScope.coroutineContext)

    val pagingCategories: LiveData<PagingData<CategorySealedView>> get() = homeUseCase.pagingCategories

    fun getRandomApps() = viewModelScope.launch {
        homeUseCase.getRandomApps(this)
    }

    fun getCategories() = viewModelScope.launch {
        homeUseCase.getCategories(this)
    }

    fun getPagingCategories() = viewModelScope.launch {
        homeUseCase.getPagingCategories(this)
    }

    fun restartState() = viewModelScope.launch {
        homeUseCase.restartState(this)
    }
}