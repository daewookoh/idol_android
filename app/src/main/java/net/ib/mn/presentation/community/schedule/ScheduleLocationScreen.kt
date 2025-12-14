package net.ib.mn.presentation.community.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.ib.mn.ui.theme.ExoTypo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.Constants
import net.ib.mn.util.logE
import java.io.IOException
import java.util.Locale

private const val TAG = "ScheduleLocationScreen"

/**
 * 스케줄 장소 선택 화면
 * Old 프로젝트의 ScheduleWriteLocationActivity를 Compose로 구현
 */
@Composable
fun ScheduleLocationScreen(
    initialLatitude: Double = 37.5192336,
    initialLongitude: Double = 127.1250279,
    onLocationSelected: (address: String, latitude: String, longitude: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Places SDK 초기화
    var placesClient by remember { mutableStateOf<PlacesClient?>(null) }

    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, Constants.getDecodedKey(Constants.GOOGLE_PLACE_KEY))
        }
        placesClient = Places.createClient(context)
    }

    // 검색 상태
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showPredictions by remember { mutableStateOf(false) }

    // 선택된 위치 상태
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("") }

    // 카메라 위치 상태
    val initialPosition = LatLng(initialLatitude, initialLongitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 16f)
    }

    // 마커 상태
    val markerState = remember { MarkerState(position = initialPosition) }

    // 지도 UI 설정 (old 프로젝트와 동일)
    val uiSettings = remember {
        MapUiSettings(
            mapToolbarEnabled = false,
            zoomControlsEnabled = false
        )
    }

    // 지도 속성
    val properties = remember {
        MapProperties()
    }

    // 위치 권한 상태
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 위치 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // 권한 요청
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Geocoder를 사용하여 좌표를 주소로 변환
    fun getAddressFromLatLng(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: ""
            } else {
                ""
            }
        } catch (e: IOException) {
            ""
        }
    }

    // 검색어 변경 시 자동완성 검색
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 && placesClient != null) {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(searchQuery)
                    .build()
                val response = placesClient!!.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions
                showPredictions = predictions.isNotEmpty()
            } catch (e: Exception) {
                logE(TAG, "Autocomplete error", e)
                predictions = emptyList()
                showPredictions = false
            }
        } else {
            predictions = emptyList()
            showPredictions = false
        }
    }

    // 장소 선택 시 위치 업데이트
    fun selectPlace(prediction: AutocompletePrediction) {
        scope.launch {
            try {
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.builder(prediction.placeId, placeFields).build()
                val response = placesClient?.fetchPlace(request)?.await()
                val place = response?.place

                place?.latLng?.let { latLng ->
                    selectedLocation = latLng
                    markerState.position = latLng
                    selectedAddress = place.name ?: prediction.getPrimaryText(null).toString()
                    searchQuery = selectedAddress
                    showPredictions = false

                    // 선택한 위치로 카메라 이동
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                    )
                }
            } catch (e: Exception) {
                logE(TAG, "Fetch place error", e)
            }
        }
    }

    ExoScaffold(
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.schedule_location),
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            // 이 장소로 설정 버튼
            Button(
                onClick = {
                    selectedLocation?.let { latLng ->
                        onLocationSelected(
                            selectedAddress,
                            latLng.latitude.toString(),
                            latLng.longitude.toString()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(50.dp),
                enabled = selectedLocation != null && selectedAddress.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorPalette.main,
                    disabledContainerColor = ColorPalette.gray200
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_use_location),
                    style = ExoTypo.typo16Bold.copy(color = Color.White)
                )
            }
        },
        containerColor = ColorPalette.background100
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 지도
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    markerState.position = latLng
                    selectedAddress = getAddressFromLatLng(latLng)
                    searchQuery = selectedAddress
                    showPredictions = false

                    // 클릭한 위치로 카메라 이동
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLng(latLng)
                    )
                }
            ) {
                // 선택된 위치에 마커 표시
                selectedLocation?.let {
                    Marker(
                        state = markerState,
                        title = selectedAddress.ifEmpty { null }
                    )
                }
            }

            // 검색창 (상단)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 검색 입력 필드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorPalette.gray300
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = ExoTypo.typo15.copy(color = ColorPalette.textDefault),
                            cursorBrush = SolidColor(ColorPalette.main),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.hint_search),
                                            style = ExoTypo.typo15.copy(color = ColorPalette.textDimmed)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // X 버튼 (검색어가 있을 때)
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.btn_popup_close),
                                contentDescription = "Clear",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        searchQuery = ""
                                        showPredictions = false
                                    },
                                tint = ColorPalette.gray300
                            )
                        }
                    }
                }

                // 자동완성 결과 목록
                if (showPredictions && predictions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyColumn {
                            items(predictions) { prediction ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectPlace(prediction) }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = prediction.getPrimaryText(null).toString(),
                                        style = ExoTypo.typo14.copy(color = ColorPalette.textDefault)
                                    )
                                    Text(
                                        text = prediction.getSecondaryText(null).toString(),
                                        style = ExoTypo.typo12.copy(color = ColorPalette.textDimmed)
                                    )
                                }
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = ColorPalette.gray110
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
