package kg.dos2.taxi_client

import android.content.Context
import android.os.Bundle

object Prefs {
    const val SHARED_PREF = "SHARED_PREF"
    const val DEFAULT_BOOLEAN = false
    const val DEFAULT_STRING = ""
    const val DEFAULT_INT = 0
    const val DEFAULT_LONG: Long = 0
    @JvmField
    var currDrv: Bundle? = null
    @JvmField
    var currOrder: Bundle? = null
    @JvmStatic
    fun setCurrOrder(id: Int, phone: String?, addr1: String?, addr2: String?, lat1: Double, lon1: Double,
                     lat2: Double, lon2: Double, price: Int, dist: Float, tp: Int, trf: Int, wtime: Int) {
        currOrder = Bundle()
        currOrder!!.putInt("id", id)
        currOrder!!.putString("phone", phone)
        currOrder!!.putString("addr1", addr1)
        currOrder!!.putString("addr2", addr2)
        currOrder!!.putDouble("lat1", lat1)
        currOrder!!.putDouble("lon1", lon1)
        currOrder!!.putDouble("lat2", lat2)
        currOrder!!.putDouble("lon2", lon2)
        currOrder!!.putInt("price", price)
        currOrder!!.putInt("tp", tp)
        currOrder!!.putInt("trf", trf)
        currOrder!!.putInt("wtime", wtime)
        currOrder!!.putFloat("dist", dist)
    }

    fun clearCurrOrder() {
        currOrder = null
    }

    const val DRIVER_ID = "DRIVER_ID"
    const val DRIVER = "DRIVER"
    const val CHAT_WITH = "CHAT_WITH"
    const val SERVER_IP = "SERVER_IP"
    const val LAST_ORDER_ID = "LAST_ORDER_ID"

    const val IS_LOG = "IS_LOG"

    const val USER_ID = "USER_ID"
    const val USER_PHONE = "USER_PHONE"
    const val USER_NAME = "USER_NAME"
    const val USER_CITY = "USER_CITY"
    const val APP_STATE = "APP_STATE"

    const val BASE_URL = "http://taalaydev.000webhostapp.com/testtaxi/cpanel/api/"
    // const val BASE_URL = "http://2dos.kg/cpanel/api/"
    const val URL_TRF_INFO = BASE_URL + "trf_info/"
    const val URL_NEW_ORDER = BASE_URL + "nw_ord/"
    const val URL_GET_ACT_DRIVERS = BASE_URL + "get_drivers/"
    const val URL_CANC_ORDER = BASE_URL + "canc_ord/"
    const val URL_COMPLETE_ORDER = BASE_URL + "compl_ord/"
    const val URL_LAST_ORDER_BY_ID = BASE_URL + "last_ord_by_id/"
    const val URL_DRIVER_INFO = BASE_URL + "driver_info/"
    const val URL_CHECK_FREE_RIDE = BASE_URL + "chisfrrd/"
    const val URL_CHECK_CLIENT = BASE_URL + "nw_client/"
    const val URL_CHECK_CLIENT_PHONE = BASE_URL + "check_client_phone/"
    const val URL_CHECK_CHAT = BASE_URL + "get_mess/"
    const val URL_SEND_MESS = BASE_URL + "send_mess/"
    const val URL_GET_DISP_LIST = BASE_URL + "get_disp_list/"
    const val URL_TRF_ICON = BASE_URL + "trf_icon/"
    const val URL_GET_CITIES_LIST = BASE_URL + "get_city_list/"
    const val FREE_RIDE_C = "FREE_RIDE_C"

    fun getBoolean(c: Context, key: String?): Boolean {
        val sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getBoolean(key, DEFAULT_BOOLEAN)
    }

    @JvmStatic
    fun getBooleanDef(c: Context, key: String?, def: Boolean): Boolean {
        val sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getBoolean(key, def)
    }

    @JvmStatic
    fun getString(context: Context, key: String?): String? {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getString(key, DEFAULT_STRING)
    }

    @JvmStatic
    fun getStringDef(context: Context, key: String?, def: String?): String? {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getString(key, def)
    }

    fun getInt(context: Context, key: String?): Int {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getInt(key, DEFAULT_INT)
    }

    @JvmStatic
    fun getIntDef(context: Context, key: String?, def: Int): Int {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getInt(key, def)
    }

    fun getLong(context: Context, key: String?): Long {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        return sp.getLong(key, DEFAULT_LONG)
    }

    @JvmStatic
    fun putBoolean(c: Context, key: String?, value: Boolean) {
        val sp = c.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        sp.edit().putBoolean(key, value).commit()
    }

    @JvmStatic
    fun putString(context: Context, key: String?, value: String?) {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        sp.edit().putString(key, value).commit()
    }

    @JvmStatic
    fun putInt(context: Context, key: String?, value: Int) {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        sp.edit().putInt(key, value).commit()
    }

    fun putLong(context: Context, key: String?, value: Long) {
        val sp = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        sp.edit().putLong(key, value).commit()
    }
}