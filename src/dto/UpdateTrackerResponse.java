package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 27/4/2017.
 */
public class UpdateTrackerResponse implements Serializable {

    public enum UpdateResponseType {
        SUCCESS_UPDATE, FAIL_UPDATE
    }

    private final UpdateResponseType type;

    public UpdateTrackerResponse(UpdateResponseType type){
        this.type=type;
    }

    public UpdateResponseType getType() {
        return type;
    }
}
