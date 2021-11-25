package remote.common.media.mirror.uploader;

public class UploadResponseBean {
    public static final int STATUS_OK = 1;
    public static final int STATUS_NOT_INSTALL = 2;
    public int status;
    public String filePath;

    public UploadResponseBean(int status, String filePath) {
        this.status = status;
        this.filePath = filePath;
    }
}
