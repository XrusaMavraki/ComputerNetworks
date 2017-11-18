package dto;

import Util.FileInfo;

import java.io.Serializable;
import java.util.*;

/**
 * Created by xrusa on 15/4/2017.
 */
public class LoginSecondRequest implements Serializable {

    private final UUID uniqueID;
    private final String ipAddress;
    private final int port;
    private final Set<FileInfo> sharedDirectoryFiles;
    private final boolean seeder;

    public LoginSecondRequest(UUID uniqueID, String ipAddress, int port, Set<FileInfo> sharedDirectoryFiles, boolean seeder) {
        this.uniqueID = uniqueID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.sharedDirectoryFiles = sharedDirectoryFiles;
        this.seeder = seeder;
    }

    public UUID getUniqueID() {
        return uniqueID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public Set<FileInfo> getSharedDirectoryFiles() {
        return sharedDirectoryFiles;
    }

    public boolean isSeeder() {
        return seeder;
    }
}
