package net.ib.mn.utils.ext

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

fun Toolbar.setCustomActionBar(context: AppCompatActivity, titleRes: Int) {
    context.setSupportActionBar(this)
    context.supportActionBar?.setTitle(titleRes)
    context.supportActionBar?.setDisplayHomeAsUpEnabled(true)
}