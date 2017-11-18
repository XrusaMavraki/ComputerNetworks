package dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by xrusa on 27/4/2017.
 */
public class UpdateTrackerRequest implements Serializable {

    private final String fileName;
    private final UUID uniqueID;
    private final String userThatSendMeFile;
    public UpdateTrackerRequest(String fileName,UUID uniqueID,String userThatSendMeFile){
        this.fileName=fileName;
        this.uniqueID=uniqueID;
        this.userThatSendMeFile=userThatSendMeFile;
    }

    public String getUserThatSendMeFile() {
        return userThatSendMeFile;
    }

    public String getFileName() {
        return fileName;
    }

    public UUID getUniqueID() {
        return uniqueID;
    }
}
