package com.utsman.listing.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.utsman.abstraction.ext.bytesToString
import com.utsman.abstraction.ext.inflate
import com.utsman.abstraction.ext.loadUrl
import com.utsman.data.model.AppsViewDiffUtil
import com.utsman.data.model.dto.AppsView
import com.utsman.listing.R
import com.utsman.listing.databinding.ItemListAppsBinding

class PagingListAdapter(private val onClick: (AppsView) -> Unit) : PagingDataAdapter<AppsView, PagingListAdapter.ListViewHolder>(AppsViewDiffUtil()) {

    class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemListAppsBinding.bind(view)

        fun bind(item: AppsView, click: (AppsView) -> Unit) = binding.run {
            txtTitle.text = item.name
            txtSize.text = item.size.bytesToString()
            imgItem.loadUrl(item.icon, item.id.toString())

            root.setOnClickListener {
                click.invoke(item)
            }
        }
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) holder.bind(item, onClick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = parent.inflate(R.layout.item_list_apps)
        return ListViewHolder(view)
    }

}