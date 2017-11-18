package dto;

import java.io.Serializable;

/**
 * Created by xrusa on 15/4/2017.
 */
public class LoginSecondResponse implements Serializable {

    public enum LoginSecondResponseType {
        SUCCESS_LOGIN1, FAIL_LOGIN_WRONG_INFO
    }

    private final LoginSecondResponseType loginSecondResponseType;

    public LoginSecondResponse(LoginSecondResponseType loginSecondResponseType){
        this.loginSecondResponseType=loginSecondResponseType;
    }

    public LoginSecondResponseType getLoginSecondResponseType() {
        return loginSecondResponseType;
    }
}
