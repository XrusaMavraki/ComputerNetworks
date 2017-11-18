package tracker;


import Util.BooleanOperations;
import Util.FileInfo;
import Util.PeerCommunicationInformation;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoggedInUserInfo {
    private final String userName;
    private final UUID userId;
    private final PeerCommunicationInformation peerCommunicationInformation;
    private final Set<FileInfo> fullyDownloadedFiles;
    private final Map<FileInfo, boolean[]> downloadedFilePieces;
    private boolean seeder;

    public LoggedInUserInfo(String userName, UUID userId, PeerCommunicationInformation peerCommunicationInformation) {
        this.userName = userName;
        this.userId = userId;
        this.peerCommunicationInformation = peerCommunicationInformation;
        fullyDownloadedFiles = ConcurrentHashMap.newKeySet();
        downloadedFilePieces = new ConcurrentHashMap<>();
        seeder = false;
    }

    void addFullyDownloadedFile(FileInfo fileInfo) {
        fullyDownloadedFiles.add(fileInfo);
        boolean[] array = new boolean[fileInfo.getNumParts()];
        downloadedFilePieces.put(fileInfo, array);
    }

    void updateDownloadedFilePieces(FileInfo fileInfo, boolean[] newFilePieces) {
        downloadedFilePieces.putIfAbsent(fileInfo, newFilePieces);
        boolean[] currentBitset = downloadedFilePieces.get(fileInfo);
        for(int i=0; i<newFilePieces.length;i++){
            if(newFilePieces[i]){
                currentBitset[i]=true;
            }
        }
        // we check if every bit in the bit set is set to 1. I.e., if the
        // cardinality (number of 1s) is equal to the size.

        if (BooleanOperations.piecesUserIsMissing(currentBitset).isEmpty()) {
            fullyDownloadedFiles.add(fileInfo);
        }
    }

    public boolean isSeeder() {
        return seeder;
    }

    public void setSeeder(boolean seeder) {
        this.seeder = seeder;
    }

    public String getUserName() {
        return userName;
    }

    public UUID getUserId() {
        return userId;
    }

    public PeerCommunicationInformation getPeerCommunicationInformation() {
        return peerCommunicationInformation;
    }

    public Set<FileInfo> getFullyDownloadedFiles() {
        return fullyDownloadedFiles;
    }

    public Map<FileInfo, boolean[]> getDownloadedFilePieces() {
        return downloadedFilePieces;
    }
}
