package kg.dos2.taxi_client.lib

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.firebase.client.Firebase
import kg.dos2.taxi_client.MainActivity
import kg.dos2.taxi_client.MainActivity.*
import kg.dos2.taxi_client.Prefs
import kg.dos2.taxi_client.Prefs.URL_CANC_ORDER
import kg.dos2.taxi_client.Prefs.URL_CHECK_CHAT
import kg.dos2.taxi_client.Prefs.URL_CHECK_FREE_RIDE
import kg.dos2.taxi_client.Prefs.URL_COMPLETE_ORDER
import kg.dos2.taxi_client.Prefs.URL_DRIVER_INFO
import kg.dos2.taxi_client.Prefs.URL_GET_ACT_DRIVERS
import kg.dos2.taxi_client.Prefs.URL_LAST_ORDER_BY_ID
import kg.dos2.taxi_client.Prefs.URL_NEW_ORDER
import kg.dos2.taxi_client.Prefs.URL_TRF_INFO
import kg.dos2.taxi_client.Prefs.getIntDef
import kg.dos2.taxi_client.Prefs.getStringDef
import kg.dos2.taxi_client.Prefs.putInt
import kg.dos2.taxi_client.Prefs.setCurrOrder
import kg.dos2.taxi_client.Trf
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap


class MyNetwork internal constructor(var activity: Activity, var listener: MyNetworkListener) {

    internal interface MyNetworkListener {
        fun myNetworkData()
        fun getLastOrder(code: Int, state: Int, obj: JSONObject, bund: Bundle)
        fun getDriverInfo(bund: Bundle)
        fun getActiveDrivers(s: String)
        fun chatData(s: String, b: Bundle)
    }

    init {
        val timer = Timer()
        val d = Date()
        timer.schedule(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    // String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(d);
                    if (d.seconds % 240 == 0) {
                        tarrifes
                    }
                    if (d.seconds % 5 == 0) { //send("gpsData " + gpsCoord);
                        checkChat()
                    }
                }
            }
        }, 10, 1000L)
    }

    fun reQueue(_c: Int, url: String, params: HashMap<String, String>, callBack: (_: String) -> Unit) {
        val rQueue = Volley.newRequestQueue(activity.applicationContext)
        rQueue.add(object : StringRequest(Method.POST, url, Response.Listener { s ->
            callBack(s)
        }, Response.ErrorListener { volleyError ->
            Log.e("myimag", "VolleyError: $volleyError")
            if ( _c < 5 ) {
                val c = _c + 1
                reQueue(c, url, params, callBack)
            } else callBack("VolleyError")
        }) {
            @Throws(AuthFailureError::class)
            public override fun getParams(): Map<String, String> {
                return params
            }

            override fun getPriority(): Priority {
                return Priority.IMMEDIATE
            }
        })
    }

    val tarrifes: Unit
        get() {
            val params = HashMap<String, String>()
            params["city"] = getIntDef(activity, Prefs.USER_CITY, 1).toString()
            reQueue(0, URL_TRF_INFO, params) { s ->
                if ( !s.equals("VolleyError") ) {
                    try {
                        val arr = JSONArray(s)
                        MainActivity.tariffList.clear()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            MainActivity.tariffList.add(Trf(
                                    obj.getInt("ID"),
                                    obj.getString("Tariff"),
                                    obj.getInt("TariffTo"),
                                    obj.getInt("TariffKm"),
                                    obj.getInt("TariffLand"),
                                    obj.getInt("TariffToS"),
                                    obj.getInt("TariffWaiting"))
                            )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("myimag", "MyNetwork getTariffes JSONException $e")
                    } catch (e: Exception) {
                        Log.e("myimag", "MyNetwork getTariffes Exception $e")
                    }
                }
            }
        }

    fun newOrder(orderParams: Bundle, callBack: () -> Unit) {
        val params = HashMap<String, String>()
        params["ord_addr1"] = orderParams.getString("addr1")!!
        params["ord_addr2"] = orderParams.getString("addr2")!!
        params["ord_desc"] = orderParams.getString("desc", "")
        params["client_phone"] = orderParams.getString("phone")!!
        params["lat1"] = orderParams.getDouble("lat1").toString()
        params["lng1"] = orderParams.getDouble("lon1").toString()
        params["lat2"] = orderParams.getDouble("lat2").toString()
        params["lng2"] = orderParams.getDouble("lon2").toString()
        params["dist"] = orderParams.getFloat("dist").toString()
        params["cost"] = orderParams.getInt("price").toString()
        params["trf"] = orderParams.getInt("trf").toString()
        params["ifr"] = orderParams.getString("ifr").toString()
        params["city"] = getIntDef(activity, Prefs.USER_CITY, 1).toString()
        params["cat"] = "7"
        params["uid"] = "0"
        params["c_id"] = getIntDef(activity, Prefs.USER_ID, 0).toString()
        reQueue(0, URL_NEW_ORDER, params) { s ->
            try {
                Log.e(MainActivity.LOG_TAG, "newOrdrer reponse " + s);
                Firebase.setAndroidContext(activity)
                Firebase("https://dos-f9894.firebaseio.com/order")
                        .child("state").setValue(System.currentTimeMillis())
                // clearBundle(bundle);
                // bundle.putInt("temp", 5);
                val obj = JSONObject(s)
                val id = obj.getInt("ID")
                if (id != 0) {
                    putInt(activity, Prefs.LAST_ORDER_ID, id)
                    callBack()
                    lastOrderByID(5)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e("myimag", "MyNetwork newOrder JSONException $e")
            } catch (e: Exception) {
                Log.e("myimag", "MyNetwork newOrder catException $e")
            }
        }
    }

    fun cancelOrder(callBack: () -> Unit) {
        val params = HashMap<String, String>()
        params["id"] = Prefs.currOrder!!.getInt("id", 0).toString()
        reQueue(0, URL_CANC_ORDER, params) { s ->
            callBack()
        }
    }

    fun completeOrder(callBack: () -> Unit) {
        reQueue(0, URL_COMPLETE_ORDER, HashMap()) { s ->

        }
    }

    fun lastOrderByID(code: Int) {
        val params = HashMap<String, String>()
        params["id"] = getIntDef(activity, Prefs.LAST_ORDER_ID, 0).toString()
        reQueue(0, URL_LAST_ORDER_BY_ID, params) { s ->
            try {
                val arr = JSONArray(s)
                for (i in 0 until  arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val state = obj.getInt("State")
                    val b = Bundle()
                    b.putInt("id", obj.getInt("ID"))
                    b.putInt("state", state)
                    b.putInt("tp", obj.getInt("tp"))
                    b.putInt("drv", obj.getInt("drv"))
                    b.putString("addr1", obj.getString("Addr1"))
                    b.putString("addr2", obj.getString("Addr2"))
                    b.putString("desc", obj.getString("Desc"))
                    b.putString("phone", obj.getString("Phone"))
                    b.putDouble("lat1", obj.getDouble("lat1"))
                    b.putDouble("lng1", obj.getDouble("lng1"))
                    b.putDouble("lat2", obj.getDouble("lat2"))
                    b.putDouble("lng2", obj.getDouble("lng2"))
                    b.putFloat("dist", obj.getDouble("dist").toFloat())
                    b.putInt("price", obj.getInt("cost"))
                    val cat = obj.getInt("cat")

                    if ( code != 0 ) {
                        setCurrOrder(
                                obj.getInt("ID"),
                                obj.getString("Phone"),
                                obj.getString("Addr1"),
                                obj.getString("Addr2"),
                                obj.getDouble("lat1"),
                                obj.getDouble("lng1"),
                                obj.getDouble("lat2"),
                                obj.getDouble("lng2"),
                                obj.getInt("cost"),
                                obj.getDouble("dist").toFloat(),
                                obj.getInt("tp"),
                                obj.getInt("trf"),
                                obj.getInt("wtime")
                        )
                    }
                    listener.getLastOrder(code, state, obj, b)

                }
            } catch (e: JSONException) {
                e.printStackTrace();
                Log.e("myimag", "MyNetwork getLastOrder JSONException " + e)
            } catch (e: Exception) {
                Log.e("myimag", "MyNetwork getLastOrder catException " + e)
            }
        }
    }

    fun getActiveDrivers(callBack: (_s: String) -> Unit) {
        val params = HashMap<String, String>()
        params["city"] = getIntDef(activity, Prefs.USER_CITY, 1).toString()
        reQueue(0, URL_GET_ACT_DRIVERS, HashMap()) { s ->
            callBack(s)
            // listener.getActiveDrivers(s)
        }
    }

    fun driverInfo(drv: Int, code: Int) {
        val params = HashMap<String, String>()
        params["id"] = drv.toString()
        reQueue(0, URL_DRIVER_INFO, params) { s ->
            try {
                val arr = JSONArray(s)
                // Log.e(MainActivity.LOG_TAG, "warn response - $s")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val bun = Bundle()
                    bun.putInt("id", obj.getInt("ID"))
                    bun.putDouble("lat", obj.getDouble("lat"))
                    bun.putDouble("lng", obj.getDouble("lng"))
                    bun.putInt("state", obj.getInt("State"))
                    bun.putString("phone", obj.getString("Phone"))
                    bun.putString("login", obj.getString("Login"))
                    bun.putString("fname", obj.getString("FirstName"))
                    bun.putString("lname", obj.getString("LastName"))
                    bun.putString("balance", obj.getString("Balance"))
                    bun.putString("anum", obj.getString("AutoNum"))
                    bun.putString("carinf", obj.getString("CarInfo"))
                    bun.putString("dvimg", obj.getString("DriverImage"))
                    bun.putString("aimg", obj.getString("AutoImage"))
                    bun.putInt("code", code)
                    val phone = obj.getString("Phone")
                    listener.getDriverInfo(bun)
                    /*   */
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "MyNetwork driverInfoException " + e.toString());
            }
        }
    }

    fun checkFreeRide(callBack: (_: Int) -> Unit) {
        val params = HashMap<String, String>()
        params["phone"] = getStringDef(activity, Prefs.USER_PHONE, "")!!
        reQueue(0, URL_CHECK_FREE_RIDE, params) { s ->
            try {
                val obj = JSONObject(s)
                val frc = obj.getInt("free_ride")
                putInt(activity, Prefs.FREE_RIDE_C, frc)
                callBack(obj.getInt("status"))
                if (obj.getInt("status") == 1) {
                    // if ( currentFragment == FRAGMENT_MAIN || currentFragment == FRAGMENT_CHAT ) {
                        // mainFragment.freeRide();
                    // }
                }
            } catch (jse: JSONException) {

            } catch (e: Exception) {
            }
        }
    }

    fun checkChat() {
        val params = HashMap<String, String>()
        params["id"] = getIntDef(activity, Prefs.USER_ID, 0).toString()
        params["act"] = "2"
        params["city"] = getIntDef(activity, Prefs.USER_CITY, 1).toString()
        reQueue(0, URL_CHECK_CHAT, params) { s ->
            try {
                val arr = JSONArray(s)
                // Log.e(LOG_TAG, "chat response - " + s);
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.getInt("m_code") == 2 || obj.getInt("m_code") == 5) {
                        var sender = "Диспетчер"
                        val bundle = Bundle()
                        bundle.putInt("id", obj.getInt("id"))
                        bundle.putInt("mCode", obj.getInt("m_code"))
                        if (obj.getInt("m_code") == 2) {
                            bundle.putInt("sender", obj.getInt("disp"))
                            bundle.putInt("getter", obj.getInt("client"))
                            bundle.putString("senderLogin", obj.getString("disp_login"))
                            bundle.putString("getterLogin", obj.getString("client_login"))
                        } else {
                            sender = "Таксист"
                            bundle.putInt("sender", obj.getInt("drv"))
                            bundle.putInt("getter", obj.getInt("client"))
                            bundle.putString("senderLogin", obj.getString("drv_login"))
                            bundle.putString("getterLogin", obj.getString("client_login"))
                        }
                        bundle.putString("message", obj.getString("mess"))
                        bundle.putString("date", obj.getString("date"))
                        bundle.putInt("getterIsRead", obj.getInt("client_is_read"))
                        listener.chatData(sender, bundle)

                    }
                }
            } catch (jse: JSONException) {

            } catch (e: Exception) {

            }
        }
    }
}