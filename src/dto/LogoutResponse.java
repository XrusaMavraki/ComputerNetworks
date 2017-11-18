package dto;

import java.io.Serializable;

public class LogoutResponse implements Serializable {

    public enum LogoutResponseType {
        SUCCESS_LOGOUT, FAIL_LOGOUT
    }

    private final LogoutResponseType logoutResponseType;

    public LogoutResponse(LogoutResponseType logoutResponseType) {
        this.logoutResponseType = logoutResponseType;
    }

    public LogoutResponseType getLogoutResponseType() {
        return logoutResponseType;
    }
}
