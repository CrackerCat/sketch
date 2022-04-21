package com.github.panpf.sketch.sample.ui.test.fetcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.github.panpf.assemblyadapter.pager2.AssemblyFragmentStateAdapter
import com.github.panpf.sketch.sample.databinding.FragmentPager2TabBinding
import com.github.panpf.sketch.sample.model.ImageDetail
import com.github.panpf.sketch.sample.ui.base.ToolbarBindingFragment
import com.github.panpf.sketch.sample.ui.view.ImageFragmentItemFactory
import com.google.android.material.tabs.TabLayoutMediator

class FetcherTestFragment : ToolbarBindingFragment<FragmentPager2TabBinding>() {

    private val viewModel by viewModels<FetcherTestViewModel>()

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = FragmentPager2TabBinding.inflate(inflater, parent, false)

    override fun onInitData(
        toolbar: Toolbar,
        binding: FragmentPager2TabBinding,
        savedInstanceState: Bundle?
    ) {
        toolbar.title = "Fetcher"

        viewModel.data.observe(viewLifecycleOwner) { data ->
            val imageFromData = data ?: return@observe
            val images = imageFromData.uris.map {
                ImageDetail(it, it, null)
            }
            val titles = imageFromData.titles

            binding.tabPagerPager.adapter = AssemblyFragmentStateAdapter(
                this,
                listOf(ImageFragmentItemFactory()),
                images
            )

            TabLayoutMediator(binding.tabPagerTabLayout, binding.tabPagerPager) { tab, position ->
                tab.text = titles[position]
            }.attach()
        }
    }
}