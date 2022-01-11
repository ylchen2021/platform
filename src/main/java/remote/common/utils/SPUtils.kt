package remote.common.utils

import android.content.Context
import android.content.SharedPreferences

class SPUtils(context: Context, spName: String) {
    private var sp: SharedPreferences = context.getSharedPreferences(spName, Context.MODE_PRIVATE)

    fun getBoolean(key: String, default: Boolean): Boolean {
        return sp.getBoolean(key, default)
    }

    fun getInt(key: String, default: Int): Int {
        return sp.getInt(key, default)
    }

    fun getString(key: String, default: String): String? {
        return sp.getString(key, default)
    }

    fun saveBoolean(key: String, value: Boolean) {
        val editor = sp.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun saveInt(key: String, value: Int) {
        val editor = sp.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun saveString(key: String, value: String) {
        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
    }
}