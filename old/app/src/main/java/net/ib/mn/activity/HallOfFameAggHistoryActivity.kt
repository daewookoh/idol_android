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
import android.widget.AdapterView
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
import net.ib.mn.feature.halloffame.HallOfFameTopHistoryActivity.Companion.createIntent
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONArray
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import javax.inject.Inject

/**
 * 명예전당 - 누적순위변화 (셀럽)
 */
@AndroidEntryPoint
class HallOfFameAggHistoryActivity : BaseActivity() {
    private var mAdapter: HallAggHistoryAdapter? = null
    private var mIdolModel: IdolModel? = null

    private var top = 999
    private var bottom = 1
    private var totalVoteCount: Long = 0 //누적순위 더할때 int 범위 넘는 경우 있어서 long 적용

    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl
    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var binding: ActivityHalloffametopBinding
    private lateinit var headerBinding: ChartHeaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHalloffametopBinding.inflate(layoutInflater)
        binding.mainScreen.applySystemBarInsets()
        setContentView(binding.root)

        val mIntent = intent
        mIdolModel = mIntent.getSerializableExtra("idol") as IdolModel?

        try {
            setCommunityTitle2(mIdolModel, getString(R.string.title_rank_history))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 차트
        headerBinding = ChartHeaderBinding.inflate(layoutInflater)
        val headerView = headerBinding.root
        binding.halloffameTopList.addHeaderView(headerView)
        headerBinding.chart.setNoDataText(resources.getString(R.string.loading))

        mAdapter = HallAggHistoryAdapter(this, Glide.with(this), null)
        binding.halloffameTopList.setAdapter(mAdapter)
        binding.halloffameTopList.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            val model = binding.halloffameTopList.getAdapter()?.getItem(position) as HallAggHistoryModel
            if (model.refdate == null) {
                return@OnItemClickListener
            }
            startActivity(
                createIntent(
                    this, model.type, model.getRefdate(this), model.refdate, mIdolModel!!.category
                )
            )
        })
        Util.showProgress(this)
        MainScope().launch {
            trendsRepository.recent(
                mIdolModel!!.getId(),
                null,
                null,
                { response ->
                    Util.closeProgress()
                    val items: MutableList<HallAggHistoryModel> = ArrayList()
                    var item: HallAggHistoryModel
                    val array: JSONArray
                    try {
                        array = response.getJSONArray("objects")
                        val gson = getInstance(true)
                        for (i in 0 until array.length()) {
                            item = gson.fromJson(
                                array.getJSONObject(i).toString(),
                                HallAggHistoryModel::class.java
                            )
                            totalVoteCount += item.heart //각 누적순위 model 의 heart수를 더 해줌.
                            items.add(item)
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
                        headerView.post(Runnable { drawChart() })


                        //반올림 적용을 위해  double로 캐스팅해서  평균값을 내어  Math.round로  반올림을 적용한다.
                        totalVoteCount = Math.round(totalVoteCount.toDouble() / items.size)

                        val voteCountComma = NumberFormat.getNumberInstance(
                            getAppLocale(
                                this@HallOfFameAggHistoryActivity
                            )
                        ).format(totalVoteCount)
                        val voteCountText = String.format(
                            resources.getString(R.string.vote_count_format),
                            voteCountComma
                        )

                        //퍙균값 text 적용
                        headerBinding.tvVoteAverage.setText(voteCountText)


                        val dayCount = String.format(
                            resources.getString(R.string.rank_history_30_average),
                            items.size.toString()
                        )

                        //날짜  데이터 수로  평균일  title  날짜수값  지정
                        headerBinding.tvVoteAverageTitle.setText(dayCount)
                    } catch (_: Exception) {
                    }
                }, { throwable ->
                    Util.closeProgress()
                    makeText(
                        this@HallOfFameAggHistoryActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun drawChart() {
        val chart = headerBinding.chart ?: return

        if ((mAdapter?.count ?: 0) == 0) {
            chart.setNoDataText("")
            chart.invalidate()
            return
        }

        //mChart.getXAxis().setLabelsToSkip(0);
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textSize = 8f
        //        mChart.getXAxis().setTextColor(ContextCompat.getColor(this, R.color.text_chart));
        chart.xAxis.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

        val description = Description()
        description.text = ""
        chart.description = description

        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setDrawGridBackground(false)

        chart.legend.isEnabled = false

        // y축 숨기기
        chart.axisLeft.isEnabled = false

        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)

        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)

        chart.isDragEnabled = true

        // 맨 위는 최고 순위, 맨 아래는 최저 순위
        for (i in 0 until mAdapter!!.count) {
            val model = mAdapter!!.getItem(i) ?: continue
            if (model.rank < top) top = model.rank
            if (model.rank > bottom) bottom = model.rank
        }

        chart.axisLeft.axisMinimum = (top - 2).toFloat()
        chart.axisLeft.axisMaximum = bottom * 1.1f

        // x축 값 표시 제거
        chart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ""
            }
        }

        val entries = ArrayList<Entry>()
        for (index in 0 until mAdapter!!.count) {
            val rank = mAdapter!!.getItem(mAdapter!!.count - 1 - index)?.rank ?: 0
            val rankCount = NumberFormat.getNumberInstance(getAppLocale(this)).format(rank.toLong())
            entries.add(
                Entry(
                    index.toFloat(),
                    (bottom - rank + top).toFloat(),
                    if (rank == 999) "-" else rankCount
                )
            )
        }


        val set = LineDataSet(entries, "")
        set.color = ContextCompat.getColor(this, R.color.main)
        set.lineWidth = 1f
        set.setDrawValues(true)
        set.valueTextSize = 10f
        set.valueTextColor = -0x6d6d6e
        set.setDrawCircleHole(false)
        set.setCircleColor(ContextCompat.getColor(this, R.color.main))
        set.circleRadius = 3f

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(set) // add the datasets

        // create a data object with the datasets
        val data = LineData(dataSets)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getPointLabel(entry: Entry): String {
                return entry.data as String
            }
        })

        chart.data = data
        chart.notifyDataSetChanged()
        chart.invalidate()
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
                    captureScreen(response.optString("active") == Const.RESPONSE_Y)
                }, {
                    captureScreen(true)
                }
            )
        }
    }

    private fun captureScreen(useable_time: Boolean) {
        var fos: FileOutputStream? = null
        val info: String

        if (!Util.isSdPresent()) {
            makeText(
                this, getString(R.string.msg_unable_use_capture_1),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rootView = binding.mainScreen.rootView
        // rootView.setDrawingCacheEnabled(true);
        // rootView.buildDrawingCache(true);
        rootView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        val captureView = Bitmap.createBitmap(
            rootView.measuredWidth,
            rootView.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val screenShotCanvas = Canvas(captureView) // Canvas
        rootView.draw(screenShotCanvas)

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
            info = if (useable_time) {
                (getString(R.string.actor_app_name) + " "
                        + Util.getFileDate("yyyy.MM.dd HH:mm") + username)
            } else {
                (getString(R.string.actor_app_name) + " "
                        + Util.getFileDate("yyyy.MM.dd") + " "
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
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent = Intent(context, HallOfFameAggHistoryActivity::class.java)
            intent.putExtra("idol", idol as Parcelable?)
            return intent
        }
    }
}
