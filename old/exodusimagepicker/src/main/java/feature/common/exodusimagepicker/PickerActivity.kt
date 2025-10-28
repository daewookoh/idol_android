package feature.common.exodusimagepicker

import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import feature.common.exodusimagepicker.adapter.FolderRvAdapter
import feature.common.exodusimagepicker.base.ImagePickerBaseActivity
import feature.common.exodusimagepicker.databinding.ActivityPickerBinding
import feature.common.exodusimagepicker.enum.MediaFolderType
import feature.common.exodusimagepicker.model.FolderModel
import feature.common.exodusimagepicker.repository.FilePagingRepository
import feature.common.exodusimagepicker.repository.FilePagingRepositoryImpl
import feature.common.exodusimagepicker.util.Const
import feature.common.exodusimagepicker.util.GlobalVariable
import feature.common.exodusimagepicker.util.Util
import feature.common.exodusimagepicker.util.livedata.SingleEventObserver
import feature.common.exodusimagepicker.util.setAllOnClickListener
import feature.common.exodusimagepicker.viewmodel.PickerViewModel
import feature.common.exodusimagepicker.viewmodel.VideoPickerViewModel
import feature.common.exodusimagepicker.viewmodel.factory.PickerViewModelFactory
import java.util.Locale

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 외부 모듈에서  피커 화면 호출시 실행되는 엑티비티
 * 현재는 비디오만 사용중.
 *
 * @see getMediaFiles 미디어 파일 새로가져올때 -> 폴더 변경 되었을때 사용될 예정
 * */
class PickerActivity : ImagePickerBaseActivity<ActivityPickerBinding>(R.layout.activity_picker) {

    private lateinit var navController: NavController

    private var pickerType = 0
    private var maxDuration = 60
    private var locale: String? = ""

    private val filePagingRepository: FilePagingRepository by lazy {
        FilePagingRepositoryImpl(application)
    }

    private val pickerViewModel: PickerViewModel by lazy {
        ViewModelProvider(
            this,
            PickerViewModelFactory(filePagingRepository),
        )[PickerViewModel::class.java]
    }

    private val videoPickerViewModel: VideoPickerViewModel by viewModels()

    private lateinit var folderRvAdapter: FolderRvAdapter

    override fun ActivityPickerBinding.onCreate() {
        binding.apply {
            lifecycleOwner = this@PickerActivity
            vm = pickerViewModel
        }

        binding.clPicker.applySystemBarInsetsAndRequest()

        initSet()
        setListenerEvent()
        getDataFromVm()
    }

    fun View.applySystemBarInsetsAndRequest() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        // attach 직후 한 프레임 뒤 요청하면 더 안전
        if (isAttachedToWindow) {
            ViewCompat.requestApplyInsets(this)
        } else {
            doOnAttach { ViewCompat.requestApplyInsets(it) }
        }
    }

    // 초기 세팅
    private fun initSet() {
        pickerType = intent.getIntExtra(Const.PARAM_PICKER_TYPE, -1)
        maxDuration = intent.getIntExtra(Const.PARAM_VIDEO_EDIT_MAX_DURATION, 60)
        locale = intent.getStringExtra(Const.PARAM_LOCALE)

        locale?.let { pickerViewModel.setLocale(this, it) }

        // TODO: 현재는 비디오만 사용중.
        pickerViewModel.mediaFolderType = if (pickerType == Const.videoPickerType) {
            MediaFolderType.VIDEO_TYPE // 타입 적용
        } else {
            MediaFolderType.IMAGE_TYPE // 타입 적용
        }

        // 폴더리스트용 rcy 어뎁터 세팅
        folderRvAdapter = FolderRvAdapter()
        binding.rvAlbumList.apply {
            adapter = folderRvAdapter
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_picker_fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        when (pickerType) {
            Const.singleImagePickerType -> {
                navController.setGraph(R.navigation.single_img_pick_nav_graph)
            }

            Const.bannerImagePickerType -> {
                navController.setGraph(R.navigation.banner_img_pick_nav_graph)
            }

            Const.multiImagePickerType -> {
                navController.setGraph(R.navigation.multi_img_pick_nav_graph)
            }

            Const.videoPickerType -> { // 비디오 피커 타입일때
                binding.tvFinish.text = getString(R.string.btn_complete_trim_video)
                navController.setGraph(R.navigation.video_pick_nav_graph)
            }

            Const.singleImageCropType -> {
                navController.setGraph(R.navigation.single_img_crop_nav_graph)
            }
        }

        setPickerScreen()
    }

    // 리스너 이벤트 모음
    private fun setListenerEvent() {
        // 폴더 아이템 클릭시
        folderRvAdapter.setItemClickListener(object : FolderRvAdapter.ItemClickListener {
            override fun onItemClickListener(position: Int) {
                getMediaFiles(
                    mediaFolderList = pickerViewModel.mediaFolderList,
                    selectedFolderIndex = position,
                )
                pickerViewModel.setFolderListShown(false) // 폴더는 닫았음을 알려  뷰를  업데이트 해준다.
                binding.rvAlbumList.hideAlbumFolder()
            }
        })

        // 종료 버튼 클릭시
        binding.tvFinish.setOnClickListener {
            if (pickerType != Const.videoPickerType) {
                videoPickerViewModel.removePlayer()
                pickerViewModel.finishBtnClick()
                return@setOnClickListener
            }

            // 비디오 피커 타입일때는  편진 화면으로 이동한다.
            if (videoPickerViewModel.isOverMaxVideoDuration()) {
                showSnackBar(getString(R.string.over_maximum_video_duration))
            } else if (videoPickerViewModel.getVideoDuration() <= 0) {
                showSnackBar(getString(R.string.msg_error_ok))
            } else {
                // 편집화면으로 갈때는  썸네일 리스트를 추출한후에 이동한다.
                binding.clToolbar.visibility = View.GONE
                binding.clToolbarVideoEdit.visibility = View.VISIBLE
                binding.tvVideoEditTitle.text = getString(R.string.title_trim_video)

                // 편집화면 넘어가기전에 썸네일 리스트를 추출한다.
                videoPickerViewModel.getThumbnailList(context = this)
            }
        }

        binding.ivBack.setOnClickListener {
            // 뒤로돌아감으로 선택한 파일들의 아이디 제거해줌.
            GlobalVariable.selectedFileIds.clear()
            finish()
        }

        // 폴더 목록 버튼 눌렀을떄 처리 -> pickerviewmodel
        binding.cgFolderList.setAllOnClickListener {
            if (!pickerViewModel.isFolderListShownCheck) {
                pickerViewModel.setFolderListShown(true)
                binding.rvAlbumList.showAlbumFolder()
                return@setAllOnClickListener
            }

            pickerViewModel.setFolderListShown(false)
            binding.rvAlbumList.hideAlbumFolder()
        }

        binding.tvVideoEditFinish.text = getString(R.string.btn_complete_trim_video)

        // 편집 완료시 완전 종료
        binding.tvVideoEditFinish.setOnClickListener {
            pickerViewModel.finishBtnClick()
        }

        // 비디오 편집화면에서 뒤로가기
        binding.ivVideoEditBack.setOnClickListener {
            binding.clToolbar.visibility = View.VISIBLE
            binding.clToolbarVideoEdit.visibility = View.GONE
            navController.popBackStack()
        }
    }

    // 앨범 폴더  숨기기
    private fun View.hideAlbumFolder() {
        visibility = View.GONE
        this.startAnimation(AnimationUtils.loadAnimation(this@PickerActivity, R.anim.bottom_to_top))
    }

    // 앨범 폴더 보여주기
    private fun View.showAlbumFolder() {
        visibility = View.VISIBLE
        this.startAnimation(AnimationUtils.loadAnimation(this@PickerActivity, R.anim.top_to_bottom))
    }

    // 뒤로가기 할때  폴더리스트 열려있으면  닫아줌.
    override fun onBackPressed() {
        if (!pickerViewModel.isFolderListShownCheck) {
            // 뒤로돌아감으로 선택한 파일들의 아이디 제거해줌.
            GlobalVariable.selectedFileIds.clear()
            finish()
            return
        }

        // 뒤로돌아감으로 선택한 파일들의 아이디 제거해줌.
        GlobalVariable.selectedFileIds.clear()
        finish()
    }

    // 미디어 파일 새로 가져옴.
    private fun getMediaFiles(mediaFolderList: List<FolderModel>, selectedFolderIndex: Int) {
        pickerViewModel.folderIndex = selectedFolderIndex
        pickerViewModel.folderId = mediaFolderList[pickerViewModel.folderIndex].folderId
        binding.tvTitle.text = mediaFolderList[pickerViewModel.folderIndex].folderName // 폴더제목에
        pickerViewModel.getMediaFiles()
    }

    private fun getDataFromVm() {
        // 썸네일 추출 여부 뷰모델로부터 받아옴.
        videoPickerViewModel.isGetVideoThumbnailFinish.observe(
            this,
            SingleEventObserver { isSuccess ->
                videoPickerViewModel.removeLoadingDialog()
                if (!isSuccess) {
                    showSnackBar(getString(R.string.uploading_thumb_error_msg))
                    return@SingleEventObserver
                }

                // 썸네일 추출  성공했으면 편집화면으로  navigation 넘김.
                val isThumbnailVisible =
                    intent.getBooleanExtra(Const.PARAM_VIDEO_THUMBNAIL_VISIBLE, true)
                navController.navigate(
                    R.id.action_videoFilePickerFragment_to_videoFileEditFragment,
                    bundleOf(
                        Const.PARAM_VIDEO_THUMBNAIL_VISIBLE to isThumbnailVisible,
                        Const.PARAM_VIDEO_EDIT_MAX_DURATION to maxDuration
                    ),
                )
            },
        )

        // 비디오 인코딩시 에러가 나왔으므로, 다시 툴바를 돌려줌.
        videoPickerViewModel.videoEncodingError.observe(
            this,
            SingleEventObserver {
                binding.clToolbar.visibility = View.VISIBLE
                binding.clToolbarVideoEdit.visibility = View.GONE
                showSnackBar(it)
            },
        )
    }

    // 미디어 파일 리스트를 불러오는등  피커 화면을 세팅 한다.
    private fun setPickerScreen() {
        // 폴더 리스트 받아옴.
        pickerViewModel.mediaFolderList =
            getMediaFolders(pickerViewModel.mediaFolderType) as MutableList<FolderModel>

        folderRvAdapter.submitList(pickerViewModel.mediaFolderList)
        binding.tvTitle.text =
            pickerViewModel.mediaFolderList[pickerViewModel.folderIndex].folderName

        pickerViewModel.getMediaFiles() // paging source에 미디어 파일 요청
    }

    // 파일들이 담긴 각각의 폴더리스트를 가져온다.
    // 각각 폴더안에는  폴더이름, 폴더 id ,각 폴더의  파일수,  폴더의 대표 이미지가 보여진다.
    private fun getMediaFolders(mediaCheck: MediaFolderType): List<FolderModel> {
        val mediaFolderAllFileList = mutableListOf<FolderModel>() // 각 파일을 폴더 정보를  매칭해서 저장하는 용도.
        val mediaFolderList = mutableSetOf<FolderModel>() // 폴더 종류를 저장할 용도로 사용.

        // 각  미디어별  폴더 이름,  폴더 id,  파일 data를  쿼리한다. media 종류는 이미 사진인지 비디오인지로 나누어져있음
        val projection =
            arrayOf(mediaCheck.bucketDisplayName, mediaCheck.bucketDisplayId, mediaCheck.data)

        // TODO: 2022/02/22 우리 앱은  gif를 안처리하므로, 모든  파일을 체크할때 gif는 배제한다.  -> 나중에 추가시 아래 두개 변수는  null로 바꿔줘야됨
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
        val selectionArgs =
            arrayOf(MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp4").toString())

        // content resolver 쿼리를 날려 cursor를 가져온다.
        val cursor =
            contentResolver.query(mediaCheck.mediaUri, projection, selection, selectionArgs, null)

        val allFileCount = cursor?.count ?: 0 // 전체행의 해당하는 갯수니까 전체 파일수가 됨.
        var offsetCount = 0
        var allFileLatestData = "" // 모든 사진 or 비디오에서 사용될 대표이미지 -> 가장 최근 사진 또는 비디오 기준으로 사용됨

        cursor?.use {
            // 각각 데이터별  이름,  data index 가져옴.
            val folderNameColumnIndex = cursor.getColumnIndexOrThrow(projection[0])
            val folderIdColumnIndex = cursor.getColumnIndexOrThrow(projection[1])
            val fileDataColumnIndex = cursor.getColumnIndexOrThrow(projection[2])

            while (it.moveToNext()) { // 다음 행으로 이동하면서 값을  받아온다.
                val folderName = cursor.getString(folderNameColumnIndex) // 데이터가 속한 폴더 이름
                val folderId =
                    cursor.getLong(folderIdColumnIndex) // 데이터가 속한 폴더 아이디 -> 이름이 겹치는경우가 있으므로, id도 함께 처리한다.
                val fileData = cursor.getString(fileDataColumnIndex) // 파일 상대경로

                if (!fileData.endsWith("mp4", ignoreCase = true)) {
                    offsetCount++
                    return@use
                }

                allFileLatestData = fileData // 계속  넣어주면  마지막 들어가는 데이터가  가장  최근 데이터 임

                // 나중에 체크하기 위해  폴더별  파일  개수를  체크할, data값을 저장
                mediaFolderAllFileList.add(
                    0,
                    FolderModel(
                        folderName = folderName ?: "",
                        folderId = folderId,
                        fileCount = 0,
                        fileData,
                    ),
                )

                // mutableSetOf를 사용해서 중복되는 애들은  사라짐. ->  이름 아이디별 폴더 리스트가  추가된다.
                mediaFolderList.add(
                    FolderModel(
                        folderName = folderName ?: "",
                        folderId = folderId,
                        fileCount = 0,
                        "",
                    ),
                )
            }
        }

        // 최종 적용될  folderlist이다. mediaFolderAllFileList에서  각각 폴더의 맞는 파일의 갯수를 체크하고, 가장 최근 fildata를  넣어준다.
        val finalMediaFolderList = mediaFolderList.toMutableList()

        // 가장 첫번째 폴덤는  모든 파일을 포함한  폴더여야하는데  이는 cursor로 따로 받아올수 없으므로,  이렇게 추가시켜줌. -> 비디오인지 사진인지에 폴더명  수정
        // 가장 첫번째 폴더는 folder  id를 null로 지정한다. (pagingsource에서  null값으로 받아야  전체 파일을 받아올수 있음)
        // fileCount는 혹시 null이면 0으로 처리
        finalMediaFolderList.add(
            0,
            FolderModel(
                if (mediaCheck.ordinal == MediaFolderType.IMAGE_TYPE.ordinal) { // 미디어 타입  사진인 경우,
                    getString(R.string.all_images)
                } else { // 미디어 타입 비디오인 경우
                    getString(R.string.all_videos)
                },
                null,
                fileCount = allFileCount - offsetCount,
                folderThumbNail = allFileLatestData,
            ),
        )

        finalMediaFolderList.map { finalFolder ->
            if (mediaFolderAllFileList.any { finalFolder.folderName == it.folderName && finalFolder.folderId == it.folderId }) {
                finalFolder.fileCount =
                    mediaFolderAllFileList.count { finalFolder.folderName == it.folderName && finalFolder.folderId == it.folderId } // 각폴더별 file 수적용
                finalFolder.folderThumbNail =
                    mediaFolderAllFileList.first { finalFolder.folderName == it.folderName && finalFolder.folderId == it.folderId }.folderThumbNail // 각 폴더별 대표이미지 uri 적용
            }
        }

        return finalMediaFolderList
    }

    companion object {
        const val KEY_SELECTED_FOLDER = "selected_folder" // 선택된 포지션
    }
}