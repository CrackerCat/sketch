package com.github.panpf.sketch.sample.item

import androidx.fragment.app.Fragment
import com.github.panpf.assemblyadapter.pager.FragmentItemFactory
import com.github.panpf.sketch.sample.bean.Image
import com.github.panpf.sketch.sample.ui.ImageFragment
import com.github.panpf.sketch.sample.ui.ImageFragmentArgs

class ImageFragmentItemFactory(
    private val loadingOptionsId: String? = null,
    private val showSmallMap: Boolean = false
) : FragmentItemFactory<Image>(Image::class) {

    override fun createFragment(
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: Image
    ): Fragment {
        return ImageFragment().apply {
            arguments = ImageFragmentArgs(
                data.normalQualityUrl,
                data.rawQualityUrl,
                loadingOptionsId,
                showSmallMap
            ).toBundle()
        }
    }
}