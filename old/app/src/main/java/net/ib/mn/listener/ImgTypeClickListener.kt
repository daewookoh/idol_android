package net.ib.mn.listener

interface ImgTypeClickListener {
    fun verticalImgClickListener()
    fun gridImgClickListener()
    fun wallpaperClickListener(wallpaperOnly: Boolean)
}