package remote.common.media

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity

import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine

object ImageAndVideoPicker {
    const val REQUEST_VIDEO = 20210801
    const val REQUEST_IMAGE = 20210802

    private var onPickListener: OnPickListener? = null

    fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == REQUEST_VIDEO || requestCode == REQUEST_IMAGE) && resultCode == AppCompatActivity.RESULT_OK) {
            var matisseData = Matisse.obtainPathResult(data);
            onPickListener?.OnPickFile(matisseData[0])
            onPickListener = null
        }
    }

    fun pickFilesFromLocalStorage(activity: AppCompatActivity, listener: OnPickListener, requestType: Int) {
        RxPermissions(activity).request(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
            .subscribe { granted ->
                if (granted) {
                    pickFiles(activity, listener, requestType)
                } else {
                    listener.OnPickFailed()
                }
            }
    }

    private fun pickFiles(activity: AppCompatActivity, listener: OnPickListener, requestType: Int) {
        var matisse = if (requestType == REQUEST_IMAGE) {
            Matisse.from(activity).choose(MimeType.ofImage())
        } else {
            Matisse.from(activity).choose(MimeType.ofVideo())
        }
        matisse.countable(true)
            .capture(false)
            .maxSelectable(1)
            .showSingleMediaType(true)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            .thumbnailScale(0.85f)
            .imageEngine(GlideEngine())
            .forResult(requestType)
        onPickListener = listener
    }


    interface OnPickListener {
        fun OnPickFile(path: String)
        fun OnPickFailed()
    }

}