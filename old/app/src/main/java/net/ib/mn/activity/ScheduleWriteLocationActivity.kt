package net.ib.mn.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import net.ib.mn.R
import net.ib.mn.databinding.ActivityScheduleWriteLocationBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.getDecodedKey
import net.ib.mn.utils.ext.applySystemBarInsets
import java.io.IOException
import java.util.Arrays

class ScheduleWriteLocationActivity : BaseActivity(), OnMapReadyCallback, OnMapClickListener,
    View.OnClickListener {
    var mMap: GoogleMap? = null

    var address: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    private var resultIntent: Intent? = null
    var startLatitude: Double? = null
    var startLongitude: Double? = null

    private lateinit var binding: ActivityScheduleWriteLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleWriteLocationBinding.inflate(layoutInflater)
        binding.flContainer.applySystemBarInsets()
        setContentView(binding.root)
        
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(true)
        actionbar.setTitle(R.string.schedule_location)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getDecodedKey(Const.GOOGLE_PLACE_KEY))
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        binding.searchBtn.setOnClickListener(this)
        binding.searchSave.setOnClickListener(this)
        binding.search.setVisibility(View.GONE)

        resultIntent = Intent()

        startLatitude = intent.getDoubleExtra("latitude", 37.5192336)
        startLongitude = intent.getDoubleExtra("longitude", 127.1250279)
        address = ""
        //        SupportPlaceAutocompleteFragment autocompleteFragment = (SupportPlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment?
        //아래 seletedListenr에서 return받고싶은 필드 등록해주세요 https://developers.google.com/maps/documentation/places/android-sdk/place-details.
        autocompleteFragment!!.setPlaceFields(
            Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                address = place.name.toString()
                latitude = place.latLng.latitude
                longitude = place.latLng.longitude
                mMap!!.clear()
                val location = LatLng(
                    latitude!!, longitude!!
                )

                val makerOptions = MarkerOptions()
                makerOptions.position(location).title(null) //주소 검색했을 때

                mMap!!.addMarker(makerOptions)
                mMap!!.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        location,
                        mMap!!.cameraPosition.zoom
                    )
                )
            }

            override fun onError(status: Status) {
                makeText(
                    this@ScheduleWriteLocationActivity,
                    status.statusMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.search_btn -> {
                val geocoder = Geocoder(this)
                var list: List<Address>? = null
                try {
                    list = geocoder.getFromLocationName(binding.searchInput.text.toString(), 1)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (list == null || list.size == 0) return
                address = binding.searchInput.text.toString()
                Util.log(address)
                latitude = list[0].latitude
                longitude = list[0].longitude

                mMap!!.clear()
                val location = LatLng(
                    latitude!!, longitude!!
                )

                val makerOptions = MarkerOptions()
                makerOptions.position(location).title(null)

                mMap!!.addMarker(makerOptions)
                mMap!!.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        location,
                        mMap!!.cameraPosition.zoom
                    )
                )
            }

            R.id.search_save -> {
                if (address!!.isEmpty()) return
                resultIntent!!.putExtra("address", address)
                resultIntent!!.putExtra("latitude", latitude!!)
                resultIntent!!.putExtra("longitude", longitude!!)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    //맵 처음 열었을 때
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.setOnMapClickListener(this)

        val seoul = LatLng(
            startLatitude!!, startLongitude!!
        )
        val makerOptions = MarkerOptions()
        makerOptions.position(seoul).title(null)
        makerOptions.visible(false) //처음 들어갔을 때 마크 없앰
        mMap!!.addMarker(makerOptions)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 16f))
    }

    //맵 터치 했을 때
    override fun onMapClick(point: LatLng) {
        mMap!!.clear()
        val location = LatLng(point.latitude, point.longitude)

        val makerOptions = MarkerOptions()
        makerOptions.position(location).title(null)

        mMap!!.addMarker(makerOptions)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, mMap!!.cameraPosition.zoom))
        mMap!!.uiSettings.isMapToolbarEnabled = false //컨트롤러 없앰
        val geocoder = Geocoder(this)
        var list: List<Address>? = null

        latitude = point.latitude
        longitude = point.longitude

        try {
            list = geocoder.getFromLocation(point.latitude, point.longitude, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (list == null || list.size == 0) return
        address = list[0].getAddressLine(0)
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            val intent = Intent(
                context,
                ScheduleWriteLocationActivity::class.java
            )
            return intent
        }

        fun createIntent(context: Context?, latitude: String, longitude: String): Intent {
            val intent = Intent(
                context,
                ScheduleWriteLocationActivity::class.java
            )
            intent.putExtra("latitude", latitude.toDouble())
            intent.putExtra("longitude", longitude.toDouble())
            return intent
        }
    }
}
