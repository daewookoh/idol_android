package net.ib.mn.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.adapter.HallAggHistoryAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityHalloffametopBinding
import net.ib.mn.databinding.ChartHeaderBinding
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.getAppName
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import javax.inject.Inject

/**
 * 기록실-어워즈-누적순위변화
 */
@AndroidEntryPoint
class GaonHistoryActivity : BaseActivity() {
    private var mAdapter: HallAggHistoryAdapter? = null
    private var mIdolModel: IdolModel? = null

    private var top = 999
    private var bottom = 1

    // java.lang.IllegalStateException: Cannot add header view to list -- setAdapter has already been called. 방지
    private var headerView: View? = null

    // gaon
    var type: String? = null // G or null
    var category: String? = null // M/F
    var event: String? = null // null or gaon2017, ...
    var chartCode: String? = null
    var sourceApp: String? = null
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var binding: ActivityHalloffametopBinding
    private lateinit var chartHeaderBinding: ChartHeaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHalloffametopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainScreen.applySystemBarInsets()

        type = intent.getStringExtra(Const.EXTRA_GAON_TYPE)
        category = intent.getStringExtra(Const.EXTRA_GAON_CATEGORY)
        event = intent.getStringExtra(Const.EXTRA_GAON_EVENT)

        //hda2023에서만 chartCode, sourceApp 사용해야함. 그래서 Intent로 받아오도록 처리
        chartCode = intent.getStringExtra(Const.KEY_CHART_CODE)
        sourceApp = intent.getStringExtra(Const.KEY_SOURCE_APP)

        val mIntent = intent
        mIdolModel = mIntent.getSerializableExtra("idol") as IdolModel?
        setCommunityTitle2(mIdolModel, getString(R.string.title_rank_history))

        // 차트
        chartHeaderBinding = ChartHeaderBinding.inflate(layoutInflater)
        chartHeaderBinding.llContainer.applySystemBarInsets()
        headerView = chartHeaderBinding.root
        binding.halloffameTopList.addHeaderView(headerView)
        chartHeaderBinding.chart.setNoDataText(resources.getString(R.string.loading))

        chartHeaderBinding.chart.setMinimumHeight(Util.convertDpToPixel(this, 120f).toInt())
        chartHeaderBinding.tvVoteAverage.setVisibility(View.GONE)
        chartHeaderBinding.tvVoteAverageTitle.setVisibility(View.GONE)


        mAdapter = HallAggHistoryAdapter(this, Glide.with(this), sourceApp)
        binding.halloffameTopList.setAdapter(mAdapter)
        binding.halloffameTopList.setOnItemClickListener(null)
        Util.showProgress(this)
        MainScope().launch {
            trendsRepository.awardRecent(
                type = type,
                category = category,
                event = event,
                idolId = mIdolModel!!.getId(),
                chartCode = chartCode,
                sourceApp = sourceApp,
                { response ->
                    Util.closeProgress()
                    val items: MutableList<HallAggHistoryModel> = ArrayList()
                    val array: JSONArray
                    try {
                        array = response.getJSONArray("objects")
                        val gson = getInstance(true)
                        for (i in 0 until array.length()) {
                            items.add(
                                gson.fromJson(
                                    array.getJSONObject(i)
                                        .toString(), HallAggHistoryModel::class.java
                                )
                            )
                        }

                        if (items.isEmpty()) {
                            binding.halloffameTopList.setVisibility(View.GONE)
                            binding.tvEmpty.setVisibility(View.VISIBLE)
                            binding.tvEmpty.setText(getString(R.string.no_data))
                        } else {
                            binding.halloffameTopList.setVisibility(View.VISIBLE)
                            binding.tvEmpty.setVisibility(View.GONE)
                        }

                        mAdapter?.clear()
                        mAdapter?.addAll(items)
                        mAdapter?.notifyDataSetChanged()
                        headerView?.post(Runnable { drawChart() })
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }, { throwable ->
                    Util.closeProgress()
                    makeText(
                        this@GaonHistoryActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun drawChart() {
        chartHeaderBinding.chart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 8f
            xAxis.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

            val desc = Description()
            desc.text = ""
            description = desc

            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setDrawGridBackground(false)

            legend.isEnabled = false

            // y축 숨기기
            axisLeft.isEnabled = false

            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)

            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)

            isDragEnabled = true

            if (mAdapter!!.count == 0) {
                setNoDataText("No data")
                invalidate()
                return
            }

            // 맨 위는 최고 순위, 맨 아래는 최저 순위
            for (i in 0 until mAdapter!!.count) {
                val model = mAdapter!!.getItem(i) ?: continue
                if (model.rank < top) top = model.rank
                if (model.rank > bottom) bottom = model.rank
            }

            axisLeft.axisMinimum = (top - 2).toFloat()
            axisLeft.axisMaximum = bottom * 1.1f

            // x축 값 표시 제거
            xAxis.valueFormatter = object : IndexAxisValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return ""
                }
            }

            val entries = ArrayList<Entry>()
            for (index in 0 until mAdapter!!.count) {
                val rank = mAdapter!!.getItem(mAdapter!!.count - 1 - index)?.rank ?: 0
                val rankCount = NumberFormat.getNumberInstance(getAppLocale(this@GaonHistoryActivity)).format(rank.toLong())
                entries.add(
                    Entry(
                        index.toFloat(),
                        (bottom - rank + top).toFloat(),
                        rankCount
                    )
                )
            }

            val set = LineDataSet(entries, "")
            set.color = ContextCompat.getColor(this@GaonHistoryActivity, R.color.main)
            set.lineWidth = 1f
            //        set.setDrawCircles(true);
//        setLux.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setDrawValues(true)
            set.valueTextSize = 10f
            set.valueTextColor = -0x6d6d6e
            //        set.setHighlightEnabled(false);
            set.setDrawCircleHole(false)
            set.setCircleColor(ContextCompat.getColor(this@GaonHistoryActivity, R.color.main))
            set.circleRadius = 3f

            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(set) // add the datasets

            // create a data object with the datasets
            val d = LineData(dataSets)
            d.setValueFormatter(object : ValueFormatter() {
                override fun getPointLabel(entry: Entry): String {
                    return entry.data as String
                }
            })

            data = d
            notifyDataSetChanged()
            invalidate()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.action_capture) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE
                )
            } else {
                checkConfirmUseForCapture()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkConfirmUseForCapture() {
        lifecycleScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    if (response.optString("active") == Const.RESPONSE_Y) {
                        captureScreen(true)
                    } else {
                        captureScreen(false)
                    }
                }, { error ->
                    captureScreen(true)
                }
            )
        }
    }

    private fun captureScreen(useable_time: Boolean) {
        var fos: FileOutputStream? = null
        var info = getAppName(this) + " "

        if (!Util.isSdPresent()) {
            makeText(
                this, getString(R.string.msg_unable_use_capture_1),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rootView = binding.mainScreen.rootView
        rootView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        val captureView = Bitmap.createBitmap(
            rootView.measuredWidth,
            rootView.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val screenShotCanvas = Canvas(captureView) // Canvas
        // ??그려지???�용?�?captureView???�용?�다.
        rootView.draw(screenShotCanvas) // rootView ???�용??canvas ??그린??

        val filename = (Const.DOWNLOAD_FILE_PATH_PREFIX
                + Util.getFileDate("yyyy-MM-dd-HH-mm-ss"))

        val f = File(Const.FILE_DIR)
        if (!f.exists()) {
            f.mkdirs()
        }
        val captureFile = File(Const.FILE_DIR, "$filename.jpeg")

        try {
            fos = FileOutputStream(captureFile)

            val p2 = Paint(Paint.ANTI_ALIAS_FLAG)
            val textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15f, resources
                    .displayMetrics
            )
            p2.textSize = textSize
            p2.color = Color.argb(255, 255, 120, 30)
            p2.style = Paint.Style.FILL
            p2.setTypeface(Typeface.DEFAULT)
            p2.isAntiAlias = true
            val account = getAccount(this)
            val username = if (account != null) " [" + account.userName + "]" else ""
            info += if (useable_time) {
                Util.getFileDate("yyyy.MM.dd HH:mm") + username
            } else {
                (Util.getFileDate("yyyy.MM.dd") + " "
                        + getString(R.string.lable_final_result) + username)
            }

            val x = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f,
                resources.displayMetrics
            )
            val y = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20f, resources.displayMetrics
            )
            screenShotCanvas.drawText(info, x, y, p2)

            captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos)

            if (captureFile.exists()) {
                if (captureFile.length() != 0L) {
                    this.sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .fromFile(captureFile)
                        )
                    )
                    val mSaveImageUri = Uri.fromFile(captureFile)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.setType("image/jpg")
                    intent.putExtra(Intent.EXTRA_STREAM, mSaveImageUri)
                    startActivity(Intent.createChooser(intent, "Choose"))
                } else {
                    makeText(
                        this,
                        getString(R.string.msg_unable_use_capture_2),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                makeText(
                    this,
                    getString(R.string.msg_unable_use_capture_2),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            makeText(
                this, getString(R.string.msg_unable_use_capture_2),
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent = Intent(context, GaonHistoryActivity::class.java)
            intent.putExtra("idol", idol as Parcelable?)
            return intent
        }
    }
}
