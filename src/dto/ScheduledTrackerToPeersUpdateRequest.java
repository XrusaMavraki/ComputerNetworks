package dto;

import Util.FileInfo;
import Util.PeerCommunicationInformation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ScheduledTrackerToPeersUpdateRequest implements Serializable {
    private final List<PeerCommunicationInformation> seeders;
    private final Map<PeerCommunicationInformation, Map<FileInfo, boolean[]>> peerToFilePieces;

    public ScheduledTrackerToPeersUpdateRequest(List<PeerCommunicationInformation> seeders, Map<PeerCommunicationInformation, Map<FileInfo, boolean[]>> peerToFilePieces) {
        this.seeders = seeders;
        this.peerToFilePieces = peerToFilePieces;
    }

    public List<PeerCommunicationInformation> getSeeders() {
        return seeders;
    }

    public Map<PeerCommunicationInformation, Map<FileInfo, boolean[]>> getPeerToFilePieces() {
        return peerToFilePieces;
    }
}
