/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.quiz.BottomSheetQuizAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.QuizRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityQuizWriteBinding
import net.ib.mn.dialog.ProgressDialogFragment
import net.ib.mn.dialog.QuizIdolOptionDialogFragment
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.model.QuizCategoryModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.MediaStoreUtils
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONException
import javax.inject.Inject

@AndroidEntryPoint
class IdolQuizWriteActivity :
    BaseActivity(),
    View.OnClickListener,
    TextWatcher,
    QuizIdolOptionDialogFragment.onItemClickCallbackListener {

    private var binImage: ByteArray? = null
    private var useSquareImage = true
    private var flag: Boolean = true

    val modelList = ArrayList<IdolModel>()
    lateinit var typeList: ArrayList<QuizCategoryModel>

    private var mIdolId = 0
    private var mAnswer = 1
    private var mDifficulty = 1
    private var mType: String? = null
    private val idolDialog: Dialog? = null

    private var mAccount: IdolAccount? = null
    private lateinit var mGlideRequestManager: RequestManager
    @Inject
    lateinit var quizRepository: QuizRepositoryImpl
    @Inject
    lateinit var idolsRepository: IdolsRepository

    private lateinit var binding: ActivityQuizWriteBinding

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        checkWriteData()
    }

    override fun afterTextChanged(s: Editable) {
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Prevent the need to choose the idol again when the activity is restarted after selecting an image during quiz creation
        val idolName = binding.tvIdol.text.toString()
        outState.putString(KEY_IDOL_NAME, idolName)
        outState.putInt(KEY_IDOL_ID, mIdolId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionbar = supportActionBar
        actionbar?.title = getString(R.string.quiz_button_write)

        mGlideRequestManager = Glide.with(this)

        binding = ActivityQuizWriteBinding.inflate(layoutInflater)
        binding.svQuizWrite.applySystemBarInsets()
        setContentView(binding.root)

        typeList = intent.getSerializableExtra(PARAM_TYPE_LIST) as ArrayList<QuizCategoryModel>

        binding.clSelectIdol.setOnClickListener(this)
        binding.clAnswer.setOnClickListener(this)
        binding.clDifficulty.setOnClickListener(this)
        binding.btnSubmit.setOnClickListener(this)
        binding.liPhotoUpload.setOnClickListener(this)

        binding.etQuizContent.addTextChangedListener(this)
        binding.etQuizChoice1.addTextChangedListener(this)
        binding.etQuizChoice2.addTextChangedListener(this)
        binding.etQuizChoice3.addTextChangedListener(this)
        binding.etQuizChoice4.addTextChangedListener(this)
        binding.etDescription.addTextChangedListener(this)
        mAccount = IdolAccount.getAccount(this)
        getIdolGroupList(this)

        if(BuildConfig.CELEB) {
            binding.tvIdolLabel.text = getString(R.string.schedule_category)
        }

        savedInstanceState?.let {
            val idolName = it.getString(KEY_IDOL_NAME)
            val idolId = it.getInt(KEY_IDOL_ID)
            binding.tvIdol.text = idolName
            mIdolId = idolId
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.cl_select_idol -> showDialogSelecIdolOption()
            R.id.cl_answer -> showAnswerBottomSheetFragment()
            R.id.cl_difficulty -> showDifficultyBottomSheetFragment()
            R.id.li_photo_upload -> {
                if (flag) {
                    onArticlePhotoClick(null)
                } else {
                    flag = true
                    binImage = null
                    binding.ivPhotoUploadImg.visibility = View.VISIBLE
                    binding.tvPhoto.text = getString(R.string.quiz_write_image)
                    binding.eivPhoto.visibility = View.GONE
                }
            }
            R.id.btn_submit -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                tryUpload(this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PHOTO_SELECT_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    chooseInternalEditor(data?.data ?: return)
                }
            }
            PHOTO_CROP_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    mTempFileForCrop?.let {
                        onArticlePhotoSelected(Uri.fromFile(it))
                        it.deleteOnExit()
                    }
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    val resultUri: Uri? = result.uri
                    onArticlePhotoSelected(resultUri)
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    val error: Exception? = result.error
                }
            }
        }
    }

    private fun checkWriteData(): Boolean {
        val isEmpty = binding.etQuizContent.text.toString().isEmpty() ||
            binding.etQuizChoice1.text.toString().isEmpty() ||
            binding.etQuizChoice2.text.toString().isEmpty() ||
            binding.etQuizChoice3.text.toString().isEmpty() ||
            binding.etQuizChoice4.text.toString().isEmpty() ||
            binding.etDescription.text.toString().isEmpty() ||
            binding.tvAnswer.text.toString().isEmpty() ||
            binding.tvIdol.text.toString().isEmpty() ||
            binding.tvDifficulty.text.toString().isEmpty()

        binding.btnSubmit.background = if (isEmpty) {
            ContextCompat.getDrawable(this, R.drawable.bg_round_boarder_gray200)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bg_round_boarder_main)
        }

        return isEmpty
    }

    private fun checkDuplicate(): Boolean {
        val choice = binding.etQuizChoice1.text.toString().trim()
            .replace(System.lineSeparator().toString(), "")
        val choice2 = binding.etQuizChoice2.text.toString().trim()
            .replace(System.lineSeparator().toString(), "")
        val choice3 = binding.etQuizChoice3.text.toString().trim()
            .replace(System.lineSeparator().toString(), "")
        val choice4 = binding.etQuizChoice4.text.toString().trim()
            .replace(System.lineSeparator().toString(), "")

        val array = arrayOf(choice, choice2, choice3, choice4)
        val list = array.toList()

        val set = HashSet(list)
        return set.size != list.size
    }

    private fun showDialog(msg: String) {
        var view = currentFocus
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        // To prevent the issue of empty space in the dialog when the keyboard is open,
        // close the keyboard and execute the dialog after a delay of 0.1 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    msg,
                    View.OnClickListener {
                        Util.closeIdolDialog()
                        binding.btnSubmit.isEnabled = true
                    },
                )
            } catch (e: NullPointerException) {
                // Prevent potential crashes
                e.printStackTrace()
            }
        }, 100)
    }

    private fun tryUpload(context: Context) {
        synchronized(this) {
            if (!binding.btnSubmit.isEnabled) {
                return
            }
            binding.btnSubmit.isEnabled = false
        }
        if (checkWriteData()) {
            showDialog(getString(R.string.quiz_write_empty))
        } else if (checkDuplicate()) {
            showDialog(getString(R.string.quiz_duplicated_answer))
        } else {
            ProgressDialogFragment.show(
                this,
                "upload",
                getString(R.string.quiz_button_write) + "...",
            )
            val content = Util.BadWordsFilterToHeart(this, binding.etQuizContent.text.toString())
            val choice1 = Util.BadWordsFilterToHeart(this, binding.etQuizChoice1.text.toString())
            val choice2 = Util.BadWordsFilterToHeart(this, binding.etQuizChoice2.text.toString())
            val choice3 = Util.BadWordsFilterToHeart(this, binding.etQuizChoice3.text.toString())
            val choice4 = Util.BadWordsFilterToHeart(this, binding.etQuizChoice4.text.toString())
            val description = Util.BadWordsFilterToHeart(this, binding.etDescription.text.toString())

            MainScope().launch {
                quizRepository.writeQuiz(
                    content,
                    choice1,
                    choice2,
                    choice3,
                    choice4,
                    mAnswer,
                    description,
                    mIdolId,
                    mDifficulty,
                    mType,
                    binImage,
                    { response ->
                        if (response.optBoolean("success")) {
                            ProgressDialogFragment.hide(this@IdolQuizWriteActivity, "upload")
                            Util.showDefaultIdolDialogWithBtn1(
                                context,
                                null,
                                getString(R.string.quiz_write_done),
                                View.OnClickListener {
                                    Util.closeIdolDialog()
                                    finish()
                                },
                            )
                        } else {
                            ProgressDialogFragment.hide(this@IdolQuizWriteActivity, "upload")
                            UtilK.handleCommonError(context, response)
                            binding.btnSubmit.isEnabled = true
                        }
                    },
                    { throwable ->
                        Toast.makeText(
                            context,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT,
                        ).show()
                        if (Util.is_log()) {
                            showMessage(throwable.message)
                        }
                        ProgressDialogFragment.hide(this@IdolQuizWriteActivity, "upload")
                        binding.btnSubmit.isEnabled = true
                    }
                )
            }
        }
    }

    private fun onArticlePhotoSelected(uri: Uri?) {
        flag = false
        binding.ivPhotoUploadImg.visibility = View.GONE
        binding.tvPhoto.text = getString(R.string.quiz_write_delete_image)

        if (binding.eivPhoto.visibility == View.GONE) {
            binding.eivPhoto.visibility = View.VISIBLE
        }

        mGlideRequestManager
            .load(uri)
            .into(binding.eivPhoto)

        if (uri != null) {
            ImageUtil.onArticlePhotoSelected(this, uri,
                photoSetCallback = { scaledBitmap ->
                    // setImageUri doesn't work on some devices.
                    binding.eivPhoto.setImageBitmap(scaledBitmap)
                },
                byteArrayCallback = { stream ->
                    if (Const.USE_MULTIPART_FORM_DATA) {
                        binImage = stream.toByteArray()
                    }
                })
        }
    }

    private fun onArticlePhotoClick(uri: Uri?) {
        if (uri != null) {
            chooseInternalEditor(uri)
        } else {
            val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)
            val packageManager = packageManager
            if (photoPickIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(photoPickIntent, PHOTO_SELECT_REQUEST)
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(R.string.cropper_not_found),
                    View.OnClickListener { Util.closeIdolDialog() },
                )
            }
        }
    }

    private fun chooseInternalEditor(uri: Uri) {
        ImageUtil.chooseInternalEditor(this, uri, useSquareImage){ file ->
            mTempFileForCrop = file
        }
    }

    private fun getIdolGroupList(context: Context) {
        lifecycleScope.launch {
            idolsRepository.getGroupsForQuiz(
                { response ->
                    val gson = IdolGson.getInstance()
                    if (response.optBoolean("success")) {
                        try {
                            val array = response.getJSONArray("objects")
                            var most: IdolModel? = null
                            for (i in 0 until array.length()) {
                                val model = gson.fromJson(
                                    array.getJSONObject(i).toString(),
                                    IdolModel::class.java,
                                )
                                // model.setLocalizedName(context)
                                if (mAccount?.most == null || model.groupId != mAccount?.most?.groupId) {
                                    modelList.add(model)
                                } else {
                                    most = model
                                }
                            }

                            modelList.sortWith { lhs, rhs ->
                                lhs.getName(context).compareTo(rhs.getName(context))
                            }

                            if (mAccount?.most != null && !mAccount?.most?.type.equals(
                                    "B",
                                    ignoreCase = true,
                                ) && most != null
                            ) {
                                modelList.add(0, most)
                            }
                        } catch (e: JSONException) {
                        }
                    } else {
                        UtilK.handleCommonError(context, response)
                    }
                }, {}
            )
        }
    }

    override fun onItemClickCallback() {
        binding.tvIdol.text = QuizIdolOptionDialogFragment.selectedIdolName
        checkWriteData()
        mIdolId = QuizIdolOptionDialogFragment.selectedIdolId
        if (BuildConfig.CELEB) {
            mType = QuizIdolOptionDialogFragment.selectedType
        }
    }

    fun showDialogSelecIdolOption() {
        QuizIdolOptionDialogFragment.getInstance(modelList, typeList)
            .show(supportFragmentManager, "idol_option")
    }

    private fun showAnswerBottomSheetFragment() {
        val answerSheet = BottomSheetFragment.newInstance(
            BottomSheetFragment.FLAG_QUIZ_ANSWER,
            BottomSheetQuizAdapter.BOTTOM_SHEET_ANSWER,
        )

        val tag = "answer"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            answerSheet.show(supportFragmentManager, tag)
        }
    }

    fun setAnswer(answer: Int) {
        binding.tvAnswer.text = answer.toString()
        checkWriteData()
        mAnswer = answer
    }

    private fun showDifficultyBottomSheetFragment() {
        val difficultySheet = BottomSheetFragment.newInstance(
            BottomSheetFragment.FLAG_QUIZ_DIFFICULTY,
            BottomSheetQuizAdapter.BOTTOM_SHEET_DIFFICULTY,
        )

        val tag = "difficulty"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            difficultySheet.show(supportFragmentManager, tag)
        }
    }

    fun setDifficulty(difficulty: String, difficultyValue: Int) {
        binding.tvDifficulty.text = difficulty
        checkWriteData()
        mDifficulty = difficultyValue
    }

    companion object {
        private const val PHOTO_SELECT_REQUEST = 8000
        private const val PHOTO_CROP_REQUEST = 7000
        private const val KEY_IDOL_NAME = "idol_name"
        private const val KEY_IDOL_ID = "idol_id"
        private const val PARAM_TYPE_LIST = "type_list"

        fun createIntent(context: Context, typeList: ArrayList<QuizCategoryModel>): Intent {
            val intent = Intent(context, IdolQuizWriteActivity::class.java)
            intent.putExtra(PARAM_TYPE_LIST, typeList)
            return intent
        }
    }
}