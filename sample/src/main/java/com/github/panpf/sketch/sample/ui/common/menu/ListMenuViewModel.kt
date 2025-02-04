package com.github.panpf.sketch.sample.ui.common.menu

import android.app.Application
import android.view.MenuItem
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.panpf.sketch.sample.R
import com.github.panpf.sketch.sample.prefsService
import com.github.panpf.sketch.sample.ui.base.LifecycleAndroidViewModel
import com.github.panpf.sketch.sample.model.DialogFragmentItemInfo
import com.github.panpf.sketch.sample.model.LayoutMode
import com.github.panpf.sketch.sample.model.MenuItemInfoGroup
import com.github.panpf.sketch.sample.model.SwitchMenuItemInfo
import com.github.panpf.sketch.sample.ui.setting.SettingsDialogFragment

class ListMenuViewModel(
    application1: Application,
    val showLayoutModeMenu: Boolean,
    val showPlayMenu: Boolean
) : LifecycleAndroidViewModel(application1) {

    class Factory(
        val application1: Application,
        val showLayoutModeMenu: Boolean,
        val showPlayMenu: Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListMenuViewModel(application1, showLayoutModeMenu, showPlayMenu) as T
        }
    }

    val menuList = MutableLiveData<List<MenuItemInfoGroup>>()

    init {
        menuList.postValue(assembleMenuList())
    }

    private fun assembleMenuList(): List<MenuItemInfoGroup> = buildList {
        add(MenuItemInfoGroup(buildList {
            if (showPlayMenu) {
                add(SwitchMenuItemInfo(
                    values = arrayOf(true, false),
                    initValue = application1.prefsService.disallowAnimatedImageInList.value,
                    titles = null,
                    iconResIds = arrayOf(
                        R.drawable.ic_pause,
                        R.drawable.ic_play,
                    ),
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                ) { _, newValue ->
                    application1.prefsService.disallowAnimatedImageInList.value = newValue
                    menuList.postValue(menuList.value)
                })
            }

            if (showLayoutModeMenu) {
                add(SwitchMenuItemInfo(
                    values = arrayOf(
                        LayoutMode.GRID,
                        LayoutMode.STAGGERED_GRID,
                    ),
                    initValue = application1.prefsService.photoListLayoutMode.value,
                    titles = null,
                    iconResIds = arrayOf(
                        R.drawable.ic_layout_grid,
                        R.drawable.ic_layout_grid_staggered,
                    ),
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                ) { _, newValue ->
                    application1.prefsService.photoListLayoutMode.value = newValue.toString()
                    menuList.postValue(menuList.value)
                })
            }

            add(
                DialogFragmentItemInfo(
                    title = "Settings",
                    iconResId = R.drawable.ic_settings,
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS,
                    fragment = SettingsDialogFragment()
                )
            )
        }))
    }
}