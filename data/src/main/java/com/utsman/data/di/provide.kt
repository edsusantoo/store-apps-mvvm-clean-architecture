package com.utsman.data.di

import com.utsman.abstraction.di.Module
import com.utsman.data.repository.*
import com.utsman.data.route.Services
import com.utsman.network.Network

fun provideServices(): Module<Services> {
    val data = Network.builder("http://ws75.aptoide.com/")
        .create(Services::class.java)
    return Module(data)
}

fun provideAppsRepository(services: Services): Module<AppsRepository> =
    Module(AppsRepositoryImpl(services))

fun provideCategoriesRepository(services: Services): Module<CategoriesRepository> =
    Module(CategoriesRepositoryImpl(services))

fun providePagingRepository(services: Services): Module<PagingRepository> =
    Module(PagingRepositoryImpl(services))