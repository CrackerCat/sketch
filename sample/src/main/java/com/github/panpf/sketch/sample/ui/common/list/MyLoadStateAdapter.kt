package com.github.panpf.sketch.sample.ui.common.list

import android.annotation.SuppressLint
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import com.github.panpf.assemblyadapter.recycler.paging.AssemblyLoadStateAdapter

class MyLoadStateAdapter(
    alwaysShowWhenEndOfPaginationReached: Boolean = true
) : AssemblyLoadStateAdapter(LoadStateItemFactory(), alwaysShowWhenEndOfPaginationReached) {

    var disableDisplay = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var pagingDataAdapter: PagingDataAdapter<*, *>? = null

    fun noDisplayLoadStateWhenPagingEmpty(pagingDataAdapter: PagingDataAdapter<*, *>) {
        this.pagingDataAdapter = pagingDataAdapter
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        if (disableDisplay) {
            return false
        }
        val pagingDataAdapter = pagingDataAdapter
        if (pagingDataAdapter != null && loadState is LoadState.NotLoading && pagingDataAdapter.itemCount == 0) {
            return false
        }
        return super.displayLoadStateAsItem(loadState)
    }
}