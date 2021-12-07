package remote.common.daemon

import androidx.lifecycle.LiveData
import remote.common.network.ResponseBean
import retrofit2.http.GET

interface DaemonInterface {
    @GET("device")
    fun getDeviceInfo(): LiveData<ResponseBean<DaemonInfo>>
}