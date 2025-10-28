/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 리사이클러뷰 아이템 비어있을때 보여주는 뷰입니다.
 *
 * */

package net.ib.mn.smalltalk.viewholder

import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.databinding.ItemEmptyViewBinding

class EmptyVH(
    val binding: ItemEmptyViewBinding
) : RecyclerView.ViewHolder(binding.root)