package com.github.panpf.sketch.sample.ui.huge

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.github.panpf.assemblyadapter.pager.FragmentItemFactory
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.sample.databinding.HugeImageViewerFragmentBinding
import com.github.panpf.sketch.sample.prefsService
import com.github.panpf.sketch.sample.ui.base.BindingFragment
import com.github.panpf.sketch.sample.ui.setting.ImageInfoDialogFragment
import com.github.panpf.sketch.sample.util.observeWithFragmentView
import com.github.panpf.sketch.viewability.showRingProgressIndicator

class HugeImageViewerFragment : BindingFragment<HugeImageViewerFragmentBinding>() {

    private val args by navArgs<HugeImageViewerFragmentArgs>()

    override fun onViewCreated(
        binding: HugeImageViewerFragmentBinding,
        savedInstanceState: Bundle?
    ) {
        binding.hugeImageViewerZoomImage.apply {
            showRingProgressIndicator()
            prefsService.readModeEnabled.stateFlow.observeWithFragmentView(this@HugeImageViewerFragment) {
                zoomAbility.readModeEnabled = it
            }
            prefsService.showTileBoundsInHugeImagePage.stateFlow.observeWithFragmentView(this@HugeImageViewerFragment) {
                zoomAbility.showTileBounds = it
            }
            displayImage(args.imageUri) {
                lifecycle(viewLifecycleOwner.lifecycle)
            }
            setOnLongClickListener {
                findNavController().navigate(
                    ImageInfoDialogFragment.createDirectionsFromImageView(this, null)
                )
                true
            }
        }

        binding.hugeImageViewerTileMap.apply {
            setZoomImageView(binding.hugeImageViewerZoomImage)
            displayImage(args.imageUri)
        }
    }

    class ItemFactory : FragmentItemFactory<String>(String::class) {

        override fun createFragment(
            bindingAdapterPosition: Int,
            absoluteAdapterPosition: Int,
            data: String
        ): Fragment = HugeImageViewerFragment().apply {
            arguments = HugeImageViewerFragmentArgs(data).toBundle()
        }
    }
}