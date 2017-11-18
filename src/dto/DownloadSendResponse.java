package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 27/4/2017.
 */
public class DownloadSendResponse implements Serializable {

    public enum DownloadResponseType {
        SUCCESS_SEND, FAIL_SEND
    }
    private final DownloadResponseType type;
    private final byte[] file;
    public DownloadSendResponse(DownloadResponseType type, byte[] file){
        this.type=type;
        this.file=file;
    }

    public DownloadResponseType getType() {
        return type;
    }

    public byte[] getFile() {
        return file;
    }
}
