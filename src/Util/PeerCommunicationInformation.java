package Util;

import java.io.Serializable;

public class PeerCommunicationInformation implements Serializable {
    private final String IPAddress;
    private final int port;
    private final String userName;
    public PeerCommunicationInformation(String IPAddress, int port, String userName){
        this.IPAddress=IPAddress;
        this.port=port;
        this.userName=userName;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PeerCommunicationInformation that = (PeerCommunicationInformation) o;

        return userName != null ? userName.equals(that.userName) : that.userName == null;
    }

    @Override
    public int hashCode() {
        return userName != null ? userName.hashCode() : 0;
    }
}
