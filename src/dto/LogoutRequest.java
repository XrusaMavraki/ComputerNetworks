package dto;

import java.io.Serializable;
import java.util.UUID;

public class LogoutRequest implements Serializable {

    private final UUID logoutUuid;

    public LogoutRequest(UUID logoutUuid) {
        this.logoutUuid = logoutUuid;
    }

    public UUID getLogoutUuid() {
        return logoutUuid;
    }
}
