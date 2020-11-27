package com.utsman.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.paging.PagingData
import com.nhaarman.mockitokotlin2.verify
import com.utsman.abstraction.dto.ResultState
import com.utsman.data.model.dto.list.AppsSealedView
import com.utsman.data.model.dto.CategoryView
import com.utsman.data.repository.AppsRepository
import com.utsman.data.repository.AppsRepositoryImpl
import com.utsman.data.repository.CategoriesRepository
import com.utsman.data.repository.CategoriesRepositoryImpl
import com.utsman.data.route.Services
import com.utsman.home.domain.HomeUseCase
import com.utsman.home.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations


class HomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val services = mock(Services::class.java)
    private lateinit var categoriesRepository: CategoriesRepository
    private lateinit var appsRepository: AppsRepository

    private lateinit var homeUseCase: HomeUseCase
    private lateinit var homeViewModel: HomeViewModel

    private val dataRandom = listOf<AppsSealedView.AppsView>()
    private val dataCategory = listOf<CategoryView>()
    private val dataPagingCategory = PagingData.from(emptyList<CategoryView>())

    private val returnRandomSuccessValue = ResultState.Success(dataRandom)
    private val returnRandomFailedValue = ResultState.Error<List<AppsSealedView.AppsView>>(th = Throwable("Error"))

    private val returnCategorySuccessValue = ResultState.Success(dataCategory)
    private val returnCategoryFailedValue = ResultState.Error<List<CategoryView>>(th = Throwable("Error"))

    private val schedulerProvider = Dispatchers.Unconfined

    @Mock
    private lateinit var observerRandomApps:  Observer<in ResultState<List<AppsSealedView.AppsView>>>

    @Mock
    private lateinit var observerCategory:  Observer<in ResultState<List<CategoryView>>>

    @Mock
    private lateinit var observerPagingCategoryView: Observer<PagingData<CategoryView>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(schedulerProvider)

        categoriesRepository = CategoriesRepositoryImpl(services)
        appsRepository = AppsRepositoryImpl(services)
        homeUseCase = HomeUseCase(appsRepository, categoriesRepository)
        homeViewModel = HomeViewModel(homeUseCase)
    }

    @Test
    fun appsRandomSuccess() = runBlockingTest {
        homeUseCase.randomList.value = returnRandomSuccessValue
        homeViewModel.randomList.observeForever(observerRandomApps)
        verify(observerRandomApps).onChanged(returnRandomSuccessValue)
    }

    @Test
    fun appsRandomFailed() = runBlockingTest {
        homeUseCase.randomList.value = returnRandomFailedValue
        homeViewModel.randomList.observeForever(observerRandomApps)
        verify(observerRandomApps).onChanged(returnRandomFailedValue)
    }

    @Test
    fun appsCategorySuccess() = runBlockingTest {
        homeUseCase.categories.value = returnCategorySuccessValue
        homeViewModel.categories.observeForever(observerCategory)
        verify(observerCategory).onChanged(returnCategorySuccessValue)

    }

    @Test
    fun appsCategoryFailed() = runBlockingTest {
        homeUseCase.categories.value = returnCategoryFailedValue
        homeViewModel.categories.observeForever(observerCategory)
        verify(observerCategory).onChanged(returnCategoryFailedValue)
    }

    @Test
    fun appsPagingCategorySuccess() = runBlockingTest {
        homeUseCase.pagingCategories.postValue(dataPagingCategory)
        homeViewModel.pagingCategories.observeForever(observerPagingCategoryView)
        verify(observerPagingCategoryView).onChanged(dataPagingCategory)
    }

    @Test
    fun appsPagingCategoryFailed() = runBlockingTest {
        homeUseCase.pagingCategories.postValue(null)
        homeViewModel.pagingCategories.observeForever(observerPagingCategoryView)
        verify(observerPagingCategoryView).onChanged(null)
    }

}