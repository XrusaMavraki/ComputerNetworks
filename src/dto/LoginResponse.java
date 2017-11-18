package dto;

import java.io.Serializable;
import java.util.UUID;

public class LoginResponse implements Serializable {

    public enum LoginResponseType {
        SUCCESS_LOGIN, FAIL_WRONG_CREDENTIALS
    }

    private final LoginResponseType loginResponseType;
    private final UUID loginUniqueId;

    public LoginResponse(LoginResponseType loginResponseType, UUID loginUniqueId) {
        this.loginResponseType = loginResponseType;
        this.loginUniqueId = loginUniqueId;
    }

    public LoginResponseType getLoginResponseType() {
        return loginResponseType;
    }

    public UUID getLoginUniqueId() {
        return loginUniqueId;
    }
}
