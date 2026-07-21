package com.github.musicyou.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.utils.plus

class ItemsPageViewModel<T : Innertube.Item> : ViewModel() {
    var itemsMap: MutableMap<String, Innertube.ItemsPage<T>?> = mutableStateMapOf()

    fun setItems(
        tag: String,
        items: Innertube.ItemsPage<T>?
    ) {
        if (!itemsMap.containsKey(tag)) itemsMap[tag] = items
        else itemsMap[tag].plus(items!!)
    }
}