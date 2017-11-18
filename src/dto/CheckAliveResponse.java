package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 15/4/2017.
 */
public class CheckAliveResponse implements Serializable {


    public enum CheckAliveType{
        YES_BABY
    }


    private final CheckAliveType check;

    public CheckAliveResponse(CheckAliveType check){
        this.check=check;
    }

    public CheckAliveType getCheck() {
        return check;
    }

}
