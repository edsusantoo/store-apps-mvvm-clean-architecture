package com.utsman.home.ui.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.utsman.abstraction.ext.bytesToString
import com.utsman.abstraction.ext.loadUrl
import com.utsman.data.model.dto.AppsSealedView
import com.utsman.home.databinding.ItemAppsBinding

class AppsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val binding = ItemAppsBinding.bind(view)

    fun bind(item: AppsSealedView.AppsView) = binding.run {
        txtTitle.text = item.name
        txtSize.text = item.size.bytesToString()
        imgItem.loadUrl(item.icon, item.id.toString())
    }
}