package remote.common.media.mirror

import androidx.lifecycle.LiveData
import remote.common.network.ResponseBean
import retrofit2.http.Body
import retrofit2.http.POST

interface MirrorInterface {
    @POST("mirror")
    fun mirror(@Body bodyMap: Map<String, String>): LiveData<ResponseBean<Any>>
}