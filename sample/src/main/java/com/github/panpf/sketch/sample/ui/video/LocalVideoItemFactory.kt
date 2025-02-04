package com.github.panpf.sketch.sample.ui.video

import android.content.Context
import com.github.panpf.sketch.displayImage
import com.github.panpf.sketch.request.updateDisplayImageOptions
import com.github.panpf.sketch.request.videoFramePercentDuration
import com.github.panpf.sketch.sample.R
import com.github.panpf.sketch.sample.databinding.VideoItemBinding
import com.github.panpf.sketch.sample.model.VideoInfo
import com.github.panpf.sketch.sample.ui.common.list.MyBindingItemFactory
import com.github.panpf.sketch.stateimage.pauseLoadWhenScrollingError
import com.github.panpf.sketch.stateimage.saveCellularTrafficError

class LocalVideoItemFactory :
    MyBindingItemFactory<VideoInfo, VideoItemBinding>(VideoInfo::class) {

    override fun initItem(
        context: Context,
        binding: VideoItemBinding,
        item: BindingItem<VideoInfo, VideoItemBinding>
    ) {
        binding.videoItemIconImage.updateDisplayImageOptions {
            placeholder(R.drawable.im_placeholder)
            error(R.drawable.im_error) {
                saveCellularTrafficError(R.drawable.im_save_cellular_traffic)
                pauseLoadWhenScrollingError()
            }
            videoFramePercentDuration(0.5f)
        }
    }

    override fun bindItemData(
        context: Context,
        binding: VideoItemBinding,
        item: BindingItem<VideoInfo, VideoItemBinding>,
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: VideoInfo
    ) {
        binding.videoItemIconImage.displayImage(data.path)
        binding.videoItemNameText.text = data.title
        binding.videoItemSizeText.text = data.getTempFormattedSize(context)
        binding.videoItemDateText.text = data.tempFormattedDate
        binding.videoItemDurationText.text = data.tempFormattedDuration
    }
}
