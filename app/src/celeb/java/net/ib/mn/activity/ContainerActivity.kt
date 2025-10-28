package net.ib.mn.activity

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.fragment.FavoritIdolFragment

@AndroidEntryPoint
class ContainerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = findViewById<android.view.View>(R.id.fragment_container)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(container.rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())

            val typedValue = android.util.TypedValue()
            var actionBarHeight = 0
            if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                actionBarHeight = android.util.TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
            }

            container.setPadding(systemBars.left, systemBars.top + actionBarHeight, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.apply {
            title = getString(R.string.support_filter_favorites)
            show()
        }

        // FavoritIdolFragment를 생성하여 액티비티에 추가하는 코드
        val favoritIdolFragment = FavoritIdolFragment()

        // FragmentTransaction을 사용하여 Fragment를 추가함
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                favoritIdolFragment
            )
            .commit()
    }
}