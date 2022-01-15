package com.github.panpf.sketch.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.github.panpf.assemblyadapter.pager2.AssemblyFragmentStateAdapter
import com.github.panpf.sketch.sample.base.ToolbarBindingFragment
import com.github.panpf.sketch.sample.databinding.FragmentPager2TabBinding
import com.github.panpf.sketch.sample.item.ImageFragmentItemFactory
import com.github.panpf.sketch.sample.vm.ImageFormatViewModel
import com.google.android.material.tabs.TabLayoutMediator

class ImageFormatFragment : ToolbarBindingFragment<FragmentPager2TabBinding>() {

    private val viewModel by viewModels<ImageFormatViewModel>()

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = FragmentPager2TabBinding.inflate(inflater, parent, false)

    override fun onInitData(
        toolbar: Toolbar,
        binding: FragmentPager2TabBinding,
        savedInstanceState: Bundle?
    ) {
        toolbar.title = "Image Format"

        viewModel.data.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.tabPagerPager.adapter = AssemblyFragmentStateAdapter(
                this,
                listOf(ImageFragmentItemFactory()),
                it.second
            )

            TabLayoutMediator(binding.tabPagerTabLayout, binding.tabPagerPager) { tab, position ->
                tab.text = it.first[position]
            }.attach()
        }
    }
}
