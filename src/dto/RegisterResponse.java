package dto;

import java.io.Serializable;

public class RegisterResponse implements Serializable {

    public enum RegisterResponseType {
        SUCCESS_REGISTER, FAIL_USERNAME_ALREADY_EXISTS, FAIL_PASSWORD_EMPTY
    }

    private final RegisterResponseType registerResponseType;

    public RegisterResponse(RegisterResponseType registerResponseType) {
        this.registerResponseType = registerResponseType;
    }

    public RegisterResponseType getRegisterResponseType() {
        return registerResponseType;
    }
}
