package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 15/4/2017.
 */
public class CheckAliveRequest implements Serializable {

    private final String checkAlive;

    public CheckAliveRequest(){
        checkAlive="Are you alive?";
    }
}
