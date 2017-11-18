package dto;

import java.io.Serializable;

public class LoginRequest implements Serializable {

    private final String userName;
    private final String password;

    public LoginRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
