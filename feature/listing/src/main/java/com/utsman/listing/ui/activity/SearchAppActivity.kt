package com.utsman.listing.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.utsman.abstraction.base.PagingStateAdapter
import com.utsman.abstraction.ext.initialLoadState
import com.utsman.abstraction.ext.logi
import com.utsman.listing.R
import com.utsman.listing.databinding.ActivityListBinding
import com.utsman.listing.ui.adapter.PagingListAdapter
import com.utsman.listing.viewmodel.SearchPagingViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchAppActivity : AppCompatActivity() {

    private val binding: ActivityListBinding by viewBinding()
    private val viewModel: SearchPagingViewModel by viewModel()
    private var searchView: SearchView? = null

    private val pagingListAdapter = PagingListAdapter {

    }

    private val pagingStateAdapter = PagingStateAdapter {
        pagingListAdapter.retry()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        viewModel.restartState()

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
        }

        val gridLayout = GridLayoutManager(this, 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (pagingStateAdapter.loadState) {
                        is LoadState.NotLoading -> 1
                        is LoadState.Loading -> if (position == pagingListAdapter.itemCount) 3 else 1
                        is LoadState.Error -> if (position == pagingListAdapter.itemCount) 3 else 1
                        else -> 1
                    }
                }
            }
        }

        binding.rvList.run {
            layoutManager = gridLayout
            adapter = pagingListAdapter.withLoadStateFooter(pagingStateAdapter)
        }

        viewModel.pagingData.observe(this, Observer { pagingData ->
            GlobalScope.launch {
                pagingListAdapter.submitData(pagingData)
            }
        })

        pagingListAdapter.addLoadStateListener { combinedLoadStates ->
            binding.layoutProgress.initialLoadState(combinedLoadStates.refresh) {
                pagingListAdapter.retry()
            }

            logi("state is -> $combinedLoadStates")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        menu?.findItem(R.id.search_action)?.also { searchMenu ->
            searchMenu.expandActionView()
            searchView = searchMenu.actionView as SearchView
            searchView?.run {
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        if (!query.isNullOrBlank()) {
                            searchMenu.collapseActionView()
                            clearFocus()
                            supportActionBar?.title = query
                            viewModel.restartState()
                            lifecycleScope.launch {
                                delay(300)
                                viewModel.searchApps(query)
                            }
                        }
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return true
                    }
                })
                setOnCloseListener {
                    super.onBackPressed()
                    true
                }
            }
        }
        return super.onCreateOptionsMenu(menu)
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

    /*override fun onBackPressed() {
        if (searchView?.isIconified == false) {
            super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }*/
}