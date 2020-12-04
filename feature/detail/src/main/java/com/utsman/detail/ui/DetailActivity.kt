/*
 * Created by Muhammad Utsman on 28/11/20 5:08 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.detail.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.utsman.abstraction.dto.listenOn
import com.utsman.abstraction.ext.*
import com.utsman.abstraction.listener.IResultState
import com.utsman.data.model.dto.detail.DetailView
import com.utsman.detail.databinding.ActivityDetailBinding
import com.utsman.detail.viewmodel.DetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private val binding: ActivityDetailBinding by viewBinding()
    private val packageApps by stringExtras("package_name")
    private val viewModel: DetailViewModel by viewModels()

    private val resultListener = object : IResultState<DetailView> {
        override fun onIdle() {
            logi("idle...")
        }

        override fun onLoading() {
            logi("loading...")
        }

        override fun onSuccess(data: DetailView) {
            logi("success...")
            setupView(data)
        }

        override fun onError(throwable: Throwable) {
            loge("error --> ${throwable.localizedMessage}")
            throwable.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupView(data: DetailView) = binding.run {
        imgDetail.loadUrl(data.icon, data.id.toString())
        txtTitle.text = data.name
        txtVersion.text = "Version ${data.appVersion.apiName}"
        txtDeveloper.text = data.developer.name
        txtDesc.text = data.description

        val size = data.file.size.bytesToString()
        val isUpdate = data.appVersion.run {
            code != 0L && apiCode > code
        }

        val isInstalled = data.appVersion.run {
            apiCode == code
        }

        val downloadTitle = when {
            isUpdate -> {
                "Update ($size)"
            }
            isInstalled -> {
                "Open"
            }
            else -> {
                "Download ($size)"
            }
        }

        btnDownload.text = downloadTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        viewModel.getDetailView(packageApps)
        viewModel.detailView.observe(this, Observer {
            it.listenOn(resultListener)
            it.bindToProgressView(binding.layoutProgress, binding.parentView) {
                viewModel.getDetailView(packageApps)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}