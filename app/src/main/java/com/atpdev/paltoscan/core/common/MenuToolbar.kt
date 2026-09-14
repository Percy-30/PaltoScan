package com.atpdev.paltoscan.core.common

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
// import android.widget.Toolbar
import com.atpdev.paltoscan.R

class MenuToolbar(
    private val context: Context,
    private val onHistoryClick: () -> Unit = {},
    private val onAboutClick: () -> Unit = {},
    private val onShareClick: () -> Unit = {},
    private val onExitClick: () -> Unit = {},
) {
    fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu, menu)
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_history -> {
                onHistoryClick()
                true
            }
            R.id.menu_about -> {
                onAboutClick()
                true
            }
            R.id.menu_share -> {
                onShareClick()
                true
            }
            R.id.menu_exit -> {
                onExitClick()
                true
            }
            else -> false
        }
    }
}
