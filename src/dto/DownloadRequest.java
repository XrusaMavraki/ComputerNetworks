package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 27/4/2017.
 */
public class DownloadRequest implements Serializable {

    private final String  fileName;

    public DownloadRequest(String fileName){
        this.fileName=fileName;
    }

    public String getFileName(){return fileName;}
}
