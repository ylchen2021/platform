package remote.common.firebase.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import tv.remote.platform.BuildConfig

object ConfigManager {
    private lateinit var remoteConfig: FirebaseRemoteConfig

    fun init(defaultValueRes: Int, resultAction: ((Boolean)->Unit)) {
        remoteConfig = Firebase.remoteConfig
        if (BuildConfig.DEBUG) {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
        }
        if (defaultValueRes > 0) {
            remoteConfig.setDefaultsAsync(defaultValueRes)
        }
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            resultAction.invoke(it.isSuccessful)
        }
    }

    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }

    fun getLong(key: String): Long {
        return remoteConfig.getLong(key)
    }

    fun getString(key: String): String {
        return remoteConfig.getString(key)
    }
}