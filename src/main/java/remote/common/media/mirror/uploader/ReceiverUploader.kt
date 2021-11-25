package remote.common.media.mirror.uploader

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import remote.common.daemon.DaemonClient
import remote.common.network.HttpClient
import remote.common.network.ResponseBean
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody

import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


object ReceiverUploader {
    private const val RECEIVER_APK_NAME = "Astrill_VPN.apk"
    private var callbacks: MutableList<UploadResultCallback> = arrayListOf()
    private lateinit var uploadReceiverLiveData: LiveData<ResponseBean<Int>>
    private lateinit var uploadCheckLiveData: LiveData<ResponseBean<Int>>
    private val uploadReceiverTrigger = MutableLiveData<String>()
    private val uploadCheckTrigger = MutableLiveData<String>()
    private var targetIp = ""
    private var appContext: Context? = null
    private var uploadSuccess = false

    fun addResultCallback(callback: UploadResultCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun removeResultCallback(callback: UploadResultCallback) {
        if (callbacks.contains(callback)) {
            callbacks.remove(callback)
        }
    }

    interface UploadResultCallback {
        fun onUploadSuccess()
        fun onUploadFailure(errorMsg: String)
    }

    fun init(context: Context) {
        appContext = context
        uploadReceiverLiveData = Transformations.switchMap(uploadReceiverTrigger) {
            uploadSuccess = false
            uploadReceiverApk(it)
        }
        uploadReceiverLiveData.observeForever { response ->
            if (response.isSuccess()) {
                var status = response.data
                when (status) {
                    UploadResponseBean.STATUS_OK -> {
                        callbacks.forEach {
                            it.onUploadSuccess()
                        }
                        uploadSuccess = true
                        //todo startmirroring
                    }
                    UploadResponseBean.STATUS_NOT_INSTALL -> {
                        callbacks.forEach {
                            it.onUploadFailure("fire tv is installing, try after installation")
                        }
                    }
                }
            } else {
                callbacks.forEach {
                    it.onUploadFailure(response.errorMessage?:"")
                }
            }
        }
        uploadCheckLiveData = Transformations.switchMap(uploadCheckTrigger) {
            DaemonClient.launchDaemon()
            Thread.sleep(500)
            val headers = hashMapOf<String, String>()
            headers["package"] = "com.astrill.astrillvpn"
            HttpClient.create("http://$targetIp:${DaemonClient.DEFAULT_PORT}/", UploadInterface::class.java)
                .uploadCheck(headers)
        }
        uploadCheckLiveData.observeForever { response ->
            if (response.isSuccess()) {
                var status = response.data
                when (status) {
                    UploadResponseBean.STATUS_OK -> {
                        callbacks.forEach {
                            it.onUploadSuccess()
                        }
                    }
                    UploadResponseBean.STATUS_NOT_INSTALL -> {
                        uploadReceiverTrigger.postValue(targetIp)
                    }
                }
            } else {
                callbacks.forEach {
                    it.onUploadFailure(response.errorMessage ?: "")
                }
            }
        }
    }

    fun release() {
    }

    fun checkUpload(targetIp: String) {
        ReceiverUploader.targetIp = targetIp
        uploadCheckTrigger.postValue(targetIp)
    }

    private fun uploadReceiverApk(targetIp: String): LiveData<ResponseBean<Int>> {
        var file = File("${appContext!!.filesDir}/tmp_receiver")
        if (!file.exists()) {
            file.createNewFile()
        }

        val assetsManager = appContext!!.resources.assets
        var inputStream = assetsManager.open(RECEIVER_APK_NAME)

        try {
            val data = ByteArray(2048)
            var nbread = 0
            var fos = FileOutputStream(file)
            while (inputStream.read(data).also { nbread = it } > -1) {
                fos.write(data, 0, nbread)
            }
        } catch (ex: Exception) {
            callbacks.forEach {
                it.onUploadFailure(ex.message?:"")
            }
        }
        val body = file.asRequestBody("multipart/form-data".toMediaType())
        val part = MultipartBody.Part.createFormData("uploaded_file", RECEIVER_APK_NAME, body)
        val headers = hashMapOf<String, String>()
        headers["package"] = "com.astrill.astrillvpn"
        return HttpClient.create("http://${targetIp}:${DaemonClient.DEFAULT_PORT}/", UploadInterface::class.java)
            .uploadReceiver(headers, part)
    }

    interface UploadInterface {
        @Multipart
        @POST("upload")
        fun uploadReceiver(@HeaderMap headers: Map<String, String>,
                           @Part file: MultipartBody.Part): LiveData<ResponseBean<Int>>

        @POST("upload/check")
        fun uploadCheck(@HeaderMap headers: Map<String, String>): LiveData<ResponseBean<Int>>
    }
}