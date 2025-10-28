package net.ib.mn.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import java.lang.ref.WeakReference

/**
 * Copyright 2015 Bartosz Lipinski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// 2023,.12.22 Deprecated되어 호환성 유지를 위해 껍데기만 남겨둠

class ControllableAppBarLayout : AppBarLayout {
    var state = State.COLLAPSED

    enum class State {
        COLLAPSED, EXPANDED
    }
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        state = if(expanded) State.EXPANDED else State.COLLAPSED
    }
}
