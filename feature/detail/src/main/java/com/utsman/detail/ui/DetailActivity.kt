/*
 * Created by Muhammad Utsman on 28/11/20 5:08 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.detail.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.Operation
import com.utsman.abstraction.interactor.listenOn
import com.utsman.abstraction.ext.*
import com.utsman.abstraction.listener.ResultStateListener
import com.utsman.data.model.dto.detail.DetailView
import com.utsman.data.model.dto.worker.FileDownload
import com.utsman.data.model.dto.worker.WorkInfoResult
import com.utsman.detail.databinding.ActivityDetailBinding
import com.utsman.detail.viewmodel.DetailViewModel
import com.utsman.network.toJson
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private val binding: ActivityDetailBinding by viewBinding()
    private val packageApps by stringExtras("package_name")
    private val viewModel: DetailViewModel by viewModels()

    private val resultListener = object : ResultStateListener<DetailView> {
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

        btnDownload.setOnClickListener {
            val permissions = listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            withPermissions(permissions) { _, deniedList ->
                if (deniedList.isEmpty()) {
                    val fileName = "${data.packageName}-${data.appVersion.apiCode}"
                    val fileDownload = FileDownload.simple {
                        this.name = data.name
                        this.fileName = fileName
                        this.packageName = data.packageName
                        this.url = data.file.url
                    }
                    viewModel.requestDownload(fileDownload)
                } else {
                    toast("permission denied")
                }
            }
        }

        viewModel.saveApps.observe(this@DetailActivity, Observer {
            logi("hmmmm -> ${it.toJson()}")
        })

        viewModel.observerWorkInfo(packageApps)
        viewModel.workStateResult.observe(this@DetailActivity, Observer { result ->
            when (result) {
                is WorkInfoResult.Stopped -> {
                    btnDownload.text = downloadTitle
                }
                is WorkInfoResult.Downloading -> {
                    val workInfo = result.workData
                    if (workInfo != null) {
                        val progress = workInfo.progress.getString("data")
                        val doneData = workInfo.outputData.getBoolean("done", false)
                        if (progress != null) btnDownload.text = progress
                        logi("done data is -> $doneData")
                        btnDownload.text = "Downloading..."

                    } else {
                        btnDownload.text = downloadTitle
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        viewModel.workerState.observe(this, Observer { state ->
            //logi("worker state is -> $state")
            when (state) {
                is Operation.State.IN_PROGRESS -> {
                    binding.txtDeveloper.text = "in progress"
                }
                is Operation.State.FAILURE -> {
                    val throwable = state.throwable
                    binding.txtDeveloper.text = throwable.localizedMessage
                }
                is Operation.State.SUCCESS -> {
                    binding.txtDeveloper.text = "success"
                }
            }
        })

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