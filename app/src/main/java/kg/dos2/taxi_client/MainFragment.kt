package kg.dos2.taxi_client

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import kg.dos2.taxi_client.ImageUtils.DownloadImageTask
import kg.dos2.taxi_client.LocationUtils.MyLocationListener
import kg.dos2.taxi_client.Prefs.URL_TRF_ICON
import kg.dos2.taxi_client.Prefs.getIntDef
import kg.dos2.taxi_client.Prefs.getStringDef
import kotlinx.android.synthetic.main.bottom_sheet_content.*
import kotlinx.android.synthetic.main.custom_marker_item.*
import kotlinx.android.synthetic.main.drsynclay.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.myd.*
import kotlinx.android.synthetic.main.search_input_expanded.*
import org.json.JSONArray
import org.json.JSONException
import java.text.DecimalFormat
import java.util.*

class MainFragment : Fragment(), View.OnClickListener, MyMapListener, MyLocationListener {
    private var mLastLocation: Location? = null
    private var gps: GPSTracker? = null
    private var m_value = 0
    private var step = 0
    private var currentTariff = 0
    private var distanceTravelled = 0f
    private val speed = 0f
    private var block_steps = false
    private var free_ride = false
    private var timer: Timer? = null
    var dialog: AlertDialog? = null
    private var callBack: FragmentsListener? = null
    fun setCallBack(callBack: FragmentsListener?) {
        this.callBack = callBack
    }

    private var street1: String? = null
    private var street2: String? = null
    private var maputils: Maputils? = null
    private var locationUtils: LocationUtils? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(context!!, "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A")
    }

    override fun onResume() {
        super.onResume()
        locationUtils!!.onResume()
        maputils!!.onResume()
        callBack!!.curFrag(MainActivity.FRAGMENT_MAIN)
        if (MainActivity.getState(context) == MainActivity.STATE_ON || MainActivity.getState(context) == MainActivity.STATE_OFF) {
            if (getStep() == 0) {
                firstStep()
            } else if (getStep() == 1) {
                secondStep()
            } else if (getStep() == 2) {
                thirdStep(0, Bundle())
            }
        }
    }

    fun CheckGpsStatus() {
        val locationManager = (context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        val GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (GpsStatus) {
            gps = GPSTracker(context)
        } else {
            Toast.makeText(context, "Включите GPS", Toast.LENGTH_LONG).show()
            val intent1 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent1)
        }
    }

    fun startTimer() {
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (activity != null) {
                        activity!!.runOnUiThread {
                            val d = Date()
                            if (d.seconds % 240 == 0) {
                            }
                            if (d.seconds % 5 == 0) {
                                if (MainActivity.getState(context) == MainActivity.STATE_ON_WAIT_ORDER_SYNC) {
                                    callBack!!.drvSearch()
                                }
                                if (MainActivity.getState(context) == MainActivity.STATE_ON_WAIT_ORDER_SYNC
                                        || MainActivity.getState(context) == MainActivity.STATE_ON_RIDE
                                        || MainActivity.getState(context) == MainActivity.STATE_ON_DRIVER_WAIT) {
                                    callBack!!.ordInfo()
                                }
                                if (MainActivity.getState(context) == MainActivity.STATE_ON_DRIVER_WAIT ||
                                        MainActivity.getState(context) == MainActivity.STATE_ON_RIDE) {
                                    callBack!!.drvInfo(getIntDef(context!!, Prefs.DRIVER_ID, 0))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOG_TAG", "MainFragmentTimerException $e")
                }
            }
        }, 10, 1000L)
    }

    override fun onStart() {
        super.onStart()
        locationUtils!!.onStart()
        maputils!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        locationUtils!!.onStop()
        maputils!!.onStop()
    }

    override fun onPause() {
        super.onPause()
        maputils!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        maputils!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        maputils!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        maputils!!.onSaveInstanceState(outState)
    }

    var t1 = 0.0
    var t2 = 0.0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootv = inflater.inflate(R.layout.fragment_main, container, false)
        checkPermission()
        CheckGpsStatus()
        timer = Timer()
        locationUtils = LocationUtils(context, this)
        val mapv = rootv.findViewById<MapView>(R.id.mapBox)
        maputils = Maputils(context, this, mapv, savedInstanceState)

        return rootv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ph = getStringDef(context!!, Prefs.USER_PHONE, "")
        ed_my_phone.setText(ph)
        slidingPanel.addPaneListener(object : SlidingUpPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float, slidePixels: Int) {
                val edmyloctext = ed_my_loc.getText().toString()
                val edtoloctext = ed_to_loc.getText().toString()
                val startpointtext = (view!!.findViewById<View>(R.id.inputStartPoint) as EditText)
                        .text.toString()
                val endpointtext = (view!!.findViewById<View>(R.id.inputEndPoint) as EditText)
                        .text.toString()
                if (slideOffset == 0f) {
                    if (!edmyloctext.isEmpty() || !edtoloctext.isEmpty()) {
                        (view!!.findViewById<View>(R.id.inputStartPoint) as EditText).setText(edmyloctext)
                        (view!!.findViewById<View>(R.id.inputEndPoint) as EditText).setText(edtoloctext)
                    }
                } else if (slideOffset == 1f) {
                    if (!startpointtext.isEmpty() || !endpointtext.isEmpty()) {
                        ed_my_loc.setText(startpointtext)
                        ed_to_loc.setText(endpointtext)
                    }
                }
            }

            override fun onPanelCollapsed(panel: View) {}
            override fun onPanelExpanded(panel: View) {}
            override fun onPanelAnchored(panel: View) {}
            override fun onPanelHidden(panel: View) {}
        })
        slidingPanel.setOnClickListener(View.OnClickListener { slidingPanel.setState(SlidingUpPaneLayout.State.EXPANDED) })

        edit_my_loc.setOnClickListener(this)
        edit_to_loc.setOnClickListener(this)
        edit_my_phone.setOnClickListener(this)
        ch_wsh_lay.setOnClickListener(this)
        footer_btn.setOnClickListener(this)
        fab_minus.setOnClickListener(this)
        fab_plus.setOnClickListener(this)
        fab_my_place.setOnClickListener(this)
        myd_close_icon.setOnClickListener(this)
        res_bt_ok.setOnClickListener(this)
        startPointLay.setOnClickListener(this)
        endPointLay.setOnClickListener(this)

        bottom_sheet_head_icon.setOnClickListener(this)
        order_button.setOnClickListener(this)
        if (gps == null) gps = GPSTracker(context)
        t1 = gps!!.getLatitude()
        t2 = gps!!.getLongitude()
        // callBack.curFrag(FRAGMENT_MAIN);

        showTariffesList()
        startTimer()
    }

    fun showTariffesList() {
        trfListLay.removeAllViews()
        for (i in MainActivity.tariffList.indices) {
            val trfView = layoutInflater.inflate(R.layout.trf_item_lay, trfListLay, false)
            val div1 = trfView.findViewById<View>(R.id.divider10)
            div1.tag = "div1"
            val div2 = trfView.findViewById<View>(R.id.divider11)
            div2.tag = "div2"
            if (currentTariff == i) {
                div1.setBackgroundColor(resources.getColor(R.color.colorAccent))
                div2.setBackgroundColor(resources.getColor(R.color.colorAccent))
            } else {
                div1.setBackgroundColor(resources.getColor(R.color.color_grey))
                div2.setBackgroundColor(resources.getColor(R.color.color_grey))
            }
            val text = trfView.findViewById<TextView>(R.id.textView4)
            text.text = MainActivity.tariffList[i].name
            val image = trfView.findViewById<ImageView>(R.id.imageView12)
            DownloadImageTask(image).execute(URL_TRF_ICON + "?id=" + MainActivity.tariffList[i].id)
            trfView.setOnClickListener {
                Log.e(MainActivity.LOG_TAG, "currTrf " + currentTariff + "; step " + getStep())
                val v = trfListLay.getChildAt(currentTariff).findViewWithTag<View>("div1")
                val v1 = trfListLay.getChildAt(currentTariff).findViewWithTag<View>("div2")
                v.setBackgroundColor(resources.getColor(R.color.color_grey))
                v1.setBackgroundColor(resources.getColor(R.color.color_grey))
                currentTariff = i
                div1.setBackgroundColor(resources.getColor(R.color.colorAccent))
                div2.setBackgroundColor(resources.getColor(R.color.colorAccent))
                if (getStep() == 2) {
                    m_value = ((distanceTravelled * MainActivity.tariffList[i].trf_km).toInt()
                            + MainActivity.tariffList[i].lnd)
                    Log.e(MainActivity.LOG_TAG, "change tariff " + MainActivity.tariffList[i].trf_km +
                            "; " + MainActivity.tariffList[i].lnd + "; " + m_value)
                    btmTotals(true, distanceTravelled, m_value)
                }
            }
            trfListLay.addView(trfView)
        }
    }

    fun complRide() {
        if (getStep() == 4) {
            changeInfo(5)
            try {
                mapLayout.visibility = View.GONE
                info_lay.visibility = View.GONE
                footer_details.visibility = View.GONE
                /// getView().findViewById(R.id.dragView).setVisibility(View.GONE);
                myd_container.visibility = View.GONE
                map_nav_btn_lay.visibility = View.GONE
                topNavigationInfoCardView.visibility = View.GONE
                search_item_expand_container.visibility = View.GONE
                if (free_ride) {
                    res_pay.text = "Бесп. поездка"
                }
                res_dist.text = String.format(Locale.getDefault(), "%sкм", Prefs.currOrder!!.getFloat("dist"))
                val temp_trf = Prefs.currOrder!!.getInt("trf", 1)
                if (currentTariff != temp_trf) {
                    for (i in MainActivity.tariffList.indices) {
                        if (temp_trf == MainActivity.tariffList[i].id) {
                            currentTariff = i
                        }
                    }
                }
                if (currentTariff <= MainActivity.tariffList.size) {
                    val resdist = MainActivity.tariffList[currentTariff].trf_km * Prefs.currOrder!!.getFloat("dist", 0f).toInt()
                    res_w_km.text = String.format(Locale.getDefault(), "%dсом", resdist)
                    res_w_land.text = String.format(Locale.getDefault(), "%dсом",
                            MainActivity.tariffList[currentTariff].lnd)
                    val reswt = MainActivity.tariffList[currentTariff].wt * Prefs.currOrder!!.getInt("wtime", 0)
                    res_waiting_trf.text = String.format(Locale.getDefault(), "%dсом", reswt)
                }
                val res_w = view!!.findViewById<TextView>(R.id.res_w)
                if (!free_ride) {
                    res_w.text = String.format(Locale.getDefault(), "%dсом", Prefs.currOrder!!.getInt("price", 0))
                }
                view!!.findViewById<View>(R.id.res_lay).visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e(MainActivity.LOG_TAG, "MainFragment complRideException $e")
            }
        }
    }

    fun startRide(): Boolean {
        if (getStep() != 4) {
            return false
        }
        changeInfo(4)
        footer_btn.isEnabled = false
        return true
    }

    fun freeRide() {
        val frc = getIntDef(context!!, Prefs.FREE_RIDE_C, 0)
        if (frc != 0) {
            free_ride = true
            val inf = "Вам доступна бесплатная поездка!"
            showOrCloseInfo(inf)
        }
    }

    fun showOrCloseInfo(inf: String?) {
        if (myd_container.visibility == View.VISIBLE) {
            myd_container.visibility = View.GONE
        } else if (myd_container.visibility == View.GONE) {
            tv_pods.text = inf
            myd_container.visibility = View.VISIBLE
        }
    }

    fun finishRide() {
        Prefs.currOrder = null
        Prefs.currDrv = null
        MainActivity.setState(context, MainActivity.STATE_ON)
        activity!!.finish()
        startActivity(activity!!.intent)
    }

    fun driverCancel(bundle: Bundle) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Info!")
        builder.setMessage("Похоже водитель отказалься от вашего заказа! Найти другого водителя?")
        builder.setPositiveButton("Да") { dialogInterface, i ->
            if (driverSyncWaitState(bundle)) {
                callBack!!.newOrder(bundle)
            }
        }
        builder.setNegativeButton("Отменить") { dialogInterface, i ->
            callBack!!.cancOrd()
            dialogInterface.cancel()
        }
        builder.show()
    }

    fun cancRide() {
        Toast.makeText(context, "Вы отменили поездку", Toast.LENGTH_SHORT).show()
        finishRide()
    }

    fun ordFromFav(bund: Bundle?) {
        if (res_lay.visibility == View.VISIBLE) {
            res_lay.visibility = View.GONE
        }
        thirdStep(1, bund)
    }

    override fun btmTotals(vis: Boolean, dist: Float, price: Int) {
        if (vis) {
            ordPriceLay.visibility = View.VISIBLE
            view!!.findViewById<View>(R.id.ch_km_rpl).visibility = View.VISIBLE
            val formattedDouble = DecimalFormat("#0.00").format(dist.toDouble())
            btm_km.text = String.format("%s КМ", formattedDouble)
            if (!free_ride) {
                tvOrdPrice.text = String.format(Locale.getDefault(), "%dСОМ", price)
            } else {
                tvOrdPrice.text = "Бесплатно"
            }
        } else {
            ordPriceLay.visibility = View.GONE
            view!!.findViewById<View>(R.id.ch_km_rpl).visibility = View.GONE
        }
    }

    override fun afterLoadGeoJSON() {
        order_button.isEnabled = true
        block_steps = false
        btmBodyVisisbility(3)
    }

    override fun getStreet(street: String) {
        if (getStep() == 0) {
            ed_my_loc.setText(street)
            inputStartPoint.setText(street)
        } else if (getStep() == 1) {
            ed_to_loc.setText(street)
            inputEndPoint.setText(street)
        }
    }

    override fun getDistance(distance: Double) {
        distanceTravelled = (distance / 1000).toFloat()
        if (currentTariff <= MainActivity.tariffList.size) {
            m_value = ((distanceTravelled * MainActivity.tariffList[currentTariff].trf_km).toInt()
                    + MainActivity.tariffList[currentTariff].lnd)
            btmTotals(true, distanceTravelled, m_value)
        }
    }

    fun cmarkervis(vis: Boolean) {
        if (vis && customMarker.visibility == View.GONE)
            customMarker.visibility = View.VISIBLE
        else if (!vis && customMarker.visibility == View.VISIBLE)
            customMarker.visibility = View.GONE
    }

    fun updStartEndPoint(a: Int) {
        if (a == 0) {
            startPointIcon1.setImageResource(R.drawable.ic_loc)
            startPointIcon2.setImageResource(R.drawable.ic_my_loc)
            endPointIcon1.setImageResource(R.drawable.ic_directions_grey)
            endPointIcon2.setImageResource(R.drawable.ic_my_loc_grey)
            inputStartPoint.setHintTextColor(resources.getColor(R.color.colorPrimary))
            inputEndPoint.setHintTextColor(resources.getColor(R.color.color_grey))
        } else if (a == 1) {
            startPointIcon1.setImageResource(R.drawable.ic_loc_grey)
            startPointIcon2.setImageResource(R.drawable.ic_my_loc_grey)
            endPointIcon1.setImageResource(R.drawable.ic_diections)
            endPointIcon2.setImageResource(R.drawable.ic_my_loc)
            inputStartPoint.setHintTextColor(resources.getColor(R.color.color_grey))
            inputEndPoint.setHintTextColor(resources.getColor(R.color.colorPrimary))
        }
    }

    fun fadeOutInViews() {
        fadeOutIn(customMarker, 300)
        fadeOutIn(search_item_expand_container, 300)
    }

    fun firstStep() {
        fadeOutInViews()
        setStep(0)
        changeInfo(5)
        cmarkervis(true)
        btmTotals(false, distanceTravelled, m_value)
        footerDetVis(1)
        maputils!!.removeRouts()
        pinTextView.text = "1"
        updStartEndPoint(0)
        order_button.text = "Далее"
        order_button.isEnabled = true
    }

    fun secondStep() {
        fadeOutInViews()
        setStep(1)
        changeInfo(0)
        cmarkervis(true)
        btmTotals(false, distanceTravelled, m_value)
        maputils!!.removeRouts()
        footerDetVis(1)
        pinTextView.text = "2"
        updStartEndPoint(1)
        order_button.text = "Далее"
        order_button.isEnabled = true
    }

    fun thirdStep(code: Int, bund: Bundle?) {
        fadeOutInViews()
        setStep(2)
        changeInfo(1)
        cmarkervis(false)
        footerDetVis(1)
        if (code != 0) {
            street1 = bund!!.getString("addr1", "")
            street2 = bund.getString("addr2", "")
            distanceTravelled = bund.getFloat("dist")
            m_value = bund.getInt("price", 0)
            block_steps = false
            order_button.isEnabled = true
            inputStartPoint.setText(street1)
            inputEndPoint.setText(street2)
            btmTotals(true, distanceTravelled, m_value)
            btmBodyVisisbility(3)
        } else {
            order_button.isEnabled = false
            block_steps = true
            maputils!!.loadGeoJson()
            // loadGeoJson(lon_1, lat_1, lon_2, lat_2);
        }
        ed_my_loc.setText(street1)
        ed_to_loc.setText(street2)
        btmBodyVisisbility(2)
        order_button.text = "Заказать"
    }

    fun stepper() {
        if (!block_steps) {
            when (getStep()) {
                0 -> {
                    changeInfo(getStep())
                    secondStep()
                }
                1 -> if (maputils!!.lat2 != 0.0 && maputils!!.lon2 != 0.0) {
                    changeInfo(getStep())
                    thirdStep(0, null)
                } else {
                    Toast.makeText(context, "Укажите на карте точку В!", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    changeInfo(getStep())
                    val bundle = Bundle()
                    bundle.putString("addr1", ed_my_loc.text.toString())
                    bundle.putString("addr2", ed_to_loc.text.toString())
                    bundle.putString("phone", ed_my_phone.text.toString())
                    bundle.putDouble("lat1", maputils!!.lat1)
                    bundle.putDouble("lon1", maputils!!.lon1)
                    bundle.putDouble("lat2", maputils!!.lat2)
                    bundle.putDouble("lon2", maputils!!.lon2)
                    bundle.putFloat("dist", distanceTravelled)
                    bundle.putInt("price", m_value)
                    bundle.putInt("trf", MainActivity.tariffList[currentTariff].id)
                    var desc = ""
                    var i = 0
                    while (i < MainActivity.wishList.size) {
                        if (MainActivity.wishList[i].getInt("isCheck", 0) == 1)
                            desc += MainActivity.wishList[i].getString("name", "") + "; "
                        i++
                    }
                    bundle.putString("desc", desc)
                    if (free_ride) {
                        bundle.putString("ifr", "1")
                    } else {
                        bundle.putString("ifr", "0")
                    }
                    if (driverSyncWaitState(bundle)) {
                        callBack!!.newOrder(bundle)
                    }
                }
                3, 4 -> cancOrdDialog("Отменить заказ?",
                        "Вы действительно хотите отменить заказ?")
            }
        }
    }

    fun cancOrdDialog(title: String?, mess: String?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(mess)
        builder.setPositiveButton("Да") { dialogInterface, i -> callBack!!.cancOrd() }
        builder.setNegativeButton("Нет") { dialogInterface, i -> dialogInterface.cancel() }
        builder.show()
    }

    fun backStepper() {
        if (!block_steps) {
            when (getStep()) {
                1 -> firstStep()
                2 -> secondStep()
                3 -> cancOrdDialog("Отменить?",
                        "Вы действительно хотите отменить?")
            }
        }
    }

    fun driverSyncWaitState(bundle: Bundle): Boolean {
        val adr1 = bundle.getString("addr1", "")
        val adr2 = bundle.getString("addr2", "")
        var phone = bundle.getString("phone", "")
        if (!adr1.isEmpty()) {
            if (!adr2.isEmpty()) {
                if (!phone!!.isEmpty()) {
                    phone = getStringDef(context!!, Prefs.USER_PHONE, "")
                    ed_my_phone.setText(phone)
                }
                ed_my_loc.isEnabled = false
                ed_to_loc.isEnabled = false
                ed_my_phone.isEnabled = false
                order_button.text = "Отмена"
                btmBodyVisisbility(2)
                MainActivity.setState(context, MainActivity.STATE_ON_WAIT_ORDER_SYNC)
                // Prefs.setCurrOrder(phone, adr1, adr2, lat_1, lon_1, lat_2, lon_2,
                // m_value, distanceTravelled);
                setStep(3)
                changeInfo(2)
                block_steps = false
                customMarker.visibility = View.GONE
                search_item_expand_container.visibility = View.GONE
                tvStartPoint.text = adr1
                tvEndPoint.text = adr2
                topNavigationInfoCardView.visibility = View.VISIBLE
                return true
            } else {
                btmBodyVisisbility(2)
                ed_to_loc.error = "Заполните это поле"
            }
        } else {
            btmBodyVisisbility(2)
            ed_my_loc.error = "Заполните это поле"
        }
        return false
    }

    override fun getStep(): Int {
        return step
    }

    override fun getAct(): Activity {
        return activity!!
    }

    fun setStep(step: Int) {
        this.step = step
    }

    fun btmBodyVisisbility(a: Int) {
        if (a == 0) {
            if (slidingPanel!!.isExpanded) {
                slidingPanel!!.state = SlidingUpPaneLayout.State.COLLAPSED
            } else {
                slidingPanel!!.state = SlidingUpPaneLayout.State.EXPANDED
            }
        } else if (a == 1) {
            slidingPanel!!.state = SlidingUpPaneLayout.State.COLLAPSED
        } else if (a == 2) {
            slidingPanel!!.state = SlidingUpPaneLayout.State.EXPANDED
        }
    }

    fun changeInfo(step: Int) {
        when (step) {
            0 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.GONE
                info_smaill_taxt.text = "Шаг 2"
                info_bold_text.text = "Укажите адрес посадки"
                info_details.visibility = View.GONE
            }
            1 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.GONE
                info_smaill_taxt.text = "Шаг 3"
                info_bold_text.text = "Заполните необходимые поля"
                info_details.visibility = View.GONE
            }
            2 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.VISIBLE
                info_smaill_taxt.text = "Шаг 4"
                info_bold_text.text = "Поиск водителя"
                info_details.visibility = View.GONE
            }
            3 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.VISIBLE
                info_smaill_taxt.text = "Шаг 5"
                info_bold_text.text = "Водитель уже в пути"
                info_details.visibility = View.VISIBLE
            }
            4 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.VISIBLE
                info_smaill_taxt.text = "Шаг 6"
                info_bold_text.text = "В пути"
                info_details.visibility = View.VISIBLE
            }
            5 -> {
                view!!.findViewById<View>(R.id.info_lay).visibility = View.GONE
                info_smaill_taxt.text = "Шаг 1"
                info_bold_text.text = "Укажите место подачи"
                info_details.visibility = View.GONE
            }
        }
    }

    fun footerDetVis(vis: Int) {
        if (vis == 0) {
            btmBodyVisisbility(1)
            dragView.visibility = View.GONE
            orderbt_price_cont.visibility = View.GONE
            myd_container.visibility = View.GONE
            footer_details.visibility = View.VISIBLE
        } else if (vis == 1) {
            dragView.visibility = View.VISIBLE
            orderbt_price_cont.visibility = View.VISIBLE
            footer_details.visibility = View.GONE
        }
    }

    fun changeChatCount(c: Int) {
        try {
            if (c == 0) {
                footer_chat_count.text = "0"
                footer_chat_count.visibility = View.GONE
            } else {
                val temp = footer_chat_count.text.toString().toInt() + 1
                footer_chat_count.text = temp.toString()
                footer_chat_count.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
        }
    }

    fun driverConnect(bundle: Bundle) {
        Prefs.currDrv = bundle
        if (getStep() != 3) {
            try {
                cmarkervis(false)
                maputils!!.removeMarkers(1)
                maputils!!.removeMarkers(2)
            } catch (e: Exception) {
            }
        }
        block_steps = false
        Log.e(MainActivity.LOG_TAG, "driverConnect $bundle")
        footerDetVis(0)
        if (!bundle.getString("dvimg", "").isEmpty()) {
            DownloadImageTask(footer_dv_im).execute(Prefs.BASE_URL.replace("/api/", "")
                    + "/img/drivers/" + bundle.getString("dvimg"))
        }
        if (!bundle.getString("aimg", "").isEmpty()) {
            DownloadImageTask(fai).execute(Prefs.BASE_URL.replace("/api/", "")
                    + "/img/cars/" + bundle.getString("aimg"))
        } else {
            fai.visibility = View.GONE
        }
        tv_taksist_text.text = String.format("%s %s", bundle.getString("fname"), bundle.getString("lname"))
        if (!bundle.getString("carinf", "").isEmpty()) {
            tv_mashina_text.text = bundle.getString("carinf")
        } else {
            mashina_ly.visibility = View.GONE
        }
        tv_nomer_text.text = bundle.getString("anum")
        tv_footer_point_a.text = Prefs.currOrder!!.getString("addr1", "")
        tv_footer_point_b.text = Prefs.currOrder!!.getString("addr2", "")
        taxi_call_fl.setOnClickListener(this)
        taxi_chat_fl.setOnClickListener(this)
        footer_details_close.setOnClickListener(this)
        search_item_expand_container.visibility = View.GONE
        if (Prefs.currOrder != null) {
            tvStartPoint.text = Prefs.currOrder!!.getString("addr1", "")
            tvEndPoint.text = Prefs.currOrder!!.getString("addr2", "")
        }
        topNavigationInfoCardView.visibility = View.VISIBLE
        changeInfo(3)
        setStep(4)
        maputils!!.clearDriversMarker()
        maputils!!.addDriverMarker(bundle.getDouble("lat"), bundle.getDouble("lng"))
    }

    fun checkDriverLockOnRide(b: Bundle) {
        maputils!!.setDriverMarkerPosition(b.getDouble("lat"), b.getDouble("lng"))
    }

    fun slideInUpAnimation(view: View, duration: Int) {
        YoYo.with(Techniques.SlideInUp).duration(duration.toLong())
                .onStart { view.visibility = View.VISIBLE }
                .playOn(view)
    }

    fun slideOutDownAnimation(view: View, duration: Int) {
        YoYo.with(Techniques.SlideOutDown).duration(duration.toLong())
                .onEnd { view.visibility = View.GONE }
                .playOn(view)
    }

    fun fadeOutIn(view: View?, duration: Int) {
        if (view != null) YoYo.with(Techniques.FadeOut).duration(duration.toLong())
                .onEnd { YoYo.with(Techniques.FadeIn).duration(duration.toLong()).playOn(view) }
                .playOn(view)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.taxi_call_fl -> {
                val number = Prefs.currDrv!!.getString("phone")
                val call = Uri.parse("tel:$number")
                val surf = Intent(Intent.ACTION_DIAL, call)
                startActivity(surf)
            }
            R.id.taxi_chat_fl -> callBack!!.openDriverChat()
            R.id.footer_details_close -> {
                slideOutDownAnimation(footer_details, 500)
                callBack!!.footerDetailsShowOrHide(false)
            }
            R.id.info_details -> if (getStep() == 4) {
                slideInUpAnimation(footer_details, 500)
                callBack!!.footerDetailsShowOrHide(true)
            }
            R.id.bottom_sheet_head_icon -> btmBodyVisisbility(0)
            R.id.order_button, R.id.footer_btn -> stepper()
            R.id.fab_my_place -> {
                gps = GPSTracker(context)
                if (gps!!.canGetLocation()) {
                    val lt = gps!!.getLatitude()
                    val ln = gps!!.getLongitude()
                    maputils!!.toMyLocation(lt, ln)
                } else {
                    //       gps.showSettingsAlert();
                }
            }
            R.id.fab_minus -> maputils!!.zoomOut()
            R.id.fab_plus -> maputils!!.zoomIn()
            R.id.myd_close_icon -> showOrCloseInfo("")
            R.id.edit_my_loc -> {
                Log.e(MainActivity.LOG_TAG, "block_steps: " + block_steps + "; step: " + getStep())
                if (!block_steps && getStep() == 1) {
                    firstStep()
                    btmBodyVisisbility(2)
                }
            }
            R.id.edit_to_loc -> if (!block_steps && getStep() == 2) {
                secondStep()
                btmBodyVisisbility(2)
            }
            R.id.edit_my_phone -> {
            }
            R.id.res_bt_ok -> finishRide()
            R.id.ch_wsh_lay -> openChDialog(3)
            R.id.startPointLay -> firstStep()
            R.id.endPointLay -> secondStep()
        }
    }

    fun openChDialog(m: Int) {
        val builder = AlertDialog.Builder(context)
        val frm = FrameLayout(context!!)
        frm.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        val mView = layoutInflater.inflate(R.layout.ch_dialog_lay, frm, false)
        val tv = mView.findViewById<TextView>(R.id.dlg_title)
        val imClose = mView.findViewById<ImageView>(R.id.dlg_close)
        imClose.setOnClickListener {
            if (dialog != null) {
                dialog!!.cancel()
            }
        }
        val lay = mView.findViewById<LinearLayout>(R.id.dlg_scr_ly)
        val btOk = mView.findViewById<Button>(R.id.ok_bt)
        val btCnc = mView.findViewById<Button>(R.id.cnc_bt)
        btCnc.setOnClickListener {
            if (dialog != null) {
                dialog!!.cancel()
            }
        }
        if (m == 1) {
            tv.text = "Тарифы"
            val radioGroup = RadioGroup(context)
            radioGroup.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            val params = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(10, 0, 10, 0)
            for (i in MainActivity.tariffList.indices) {
                val radioButton = RadioButton(context)
                if (currentTariff == i) radioButton.isChecked = true
                radioButton.text = MainActivity.tariffList[i].name
                radioGroup.addView(radioButton, params)
            }
            val prms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            prms.setMargins(0, 10, 0, 10)
            lay.addView(radioGroup, prms)
            btOk.setOnClickListener {
                for (i in 0 until radioGroup.childCount) {
                    if ((radioGroup.getChildAt(i) as RadioButton).isChecked) {
                        currentTariff = i
                        if (currentTariff <= MainActivity.tariffList.size) {
                            if (getStep() == 2 && !block_steps) {
                                m_value = ((distanceTravelled * MainActivity.tariffList[currentTariff].trf_km).toInt()
                                        + MainActivity.tariffList[currentTariff].lnd)
                                btmTotals(true, distanceTravelled, m_value)
                            }
                        }
                        if (dialog != null) dialog!!.cancel()
                        break
                    }
                }
            }
        } else if (m == 2) {
            tv.text = "Оплата"
            val radioGroup = RadioGroup(context)
            radioGroup.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            val params = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(10, 0, 10, 0)
            val arr = arrayOf("Наличными")
            for (i in arr.indices) {
                val radioButton = RadioButton(context)
                radioButton.text = arr[i]
                radioGroup.addView(radioButton, params)
            }
            val prms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            prms.setMargins(0, 10, 0, 10)
            lay.addView(radioGroup, prms)
            btOk.setOnClickListener { if (dialog != null) dialog!!.cancel() }
        } else if (m == 3) {
            tv.text = "Пожелания"
            val params = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(2, 2, 2, 2)
            for (i in MainActivity.wishList.indices) {
                val checkBox = CheckBox(context)
                checkBox.text = MainActivity.wishList[i].getString("name", "")
                if (MainActivity.wishList[i].getInt("isCheck", 0) == 1) checkBox.isChecked = true
                checkBox.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        MainActivity.wishList[i].putInt("isCheck", 1)
                    } else {
                        MainActivity.wishList[i].putInt("isCheck", 0)
                    }
                }
                lay.addView(checkBox, params)
            }
            btOk.setOnClickListener { if (dialog != null) dialog!!.cancel() }
        }
        builder.setView(mView)
        dialog = builder.show()
    }

    fun driverMarkerUpdate(bundle: Bundle) {
        val temp = bundle.getInt("temp", 0)
        Log.e(MainActivity.LOG_TAG, "driverMarkerUpdate " + (temp == 0 || temp == 5))
        if ((temp == 0 || temp == 5)) {
            try {
                val lat = bundle.getDouble("lat")
                val lng = bundle.getDouble("lng")
                if ( maputils != null )
                    maputils!!.setDriverMarkerPosition(lat, lng)
            } catch (e: Exception) {
                Log.e(MainActivity.LOG_TAG, "drvMarker.setPosition exception " + e.message)
            }
        } else {
            try {
                setStep(3)
                customMarker.visibility = View.GONE
                maputils!!.pmrk()
                Log.e(MainActivity.LOG_TAG, "driverMarkerUpdate drvcon")
                driverConnect(bundle)
            } catch (exc: Exception) {
                Log.e(MainActivity.LOG_TAG, "driverMarkerUpdateException $exc")
            }
        }
    }

    fun checkPermission() { // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                    2)
        } else { // Permission has already been granted
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onLocationChanged(location: Location) { // Assign the new location
        var l_1 = 0.0
        var l_2 = 0.0
        try {
            mLastLocation = location
            l_1 = location.latitude
            l_2 = location.longitude
            val coordinate = LatLng(l_2, l_1)
            val yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 16.0)
            // mMap.animateCamera(yourLocation);
        } catch (e: Exception) {
            Log.e(MainActivity.LOG_TAG, e.toString())
        }
    }

    fun showDriversOnMap(json: String) {
        try {
            maputils!!.clearDriversMarker()
            val arr = JSONArray(json)
            // Log.e(MainActivity.LOG_TAG, "warn response - $json")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id")
                val state = obj.getInt("state")
                val fname = obj.getString("fname")
                val lname = obj.getString("lname")
                val phone = obj.getString("phone")
                //t1 += t1 + 0.00001
                //t2 += t2 + 0.00001

                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lng")
                maputils!!.addDriversMarker(lat, lon, i)
            }
        } catch (jse: JSONException) {
            Log.e(MainActivity.LOG_TAG, "showDriversOnmap JSONException " + jse.toString())
        } catch (e: Exception) {
            Log.e(MainActivity.LOG_TAG, "showDriversOnmap Exception " + e.toString())
        }
    }

    fun onBackPressed(): Boolean {
        if (slidingPanel.isExpanded) {
            slidingPanel.state = SlidingUpPaneLayout.State.COLLAPSED
        } else {
            if (getStep() == 0) {
                return true
            }
            backStepper()
        }
        return false
    }

    companion object {
        fun newInstance(bundle: Bundle): MainFragment {
            val fragment = MainFragment()
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }
            var width = drawable.intrinsicWidth
            width = if (width > 0) width else 1
            var height = drawable.intrinsicHeight
            height = if (height > 0) height else 1
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}