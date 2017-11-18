package dto;

import Util.FileInfo;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xrusa on 27/5/2017.
 */
public class ScheduledPeerToTrackerUpdateRequest implements Serializable {
    private final UUID peerId;
    private final Map<FileInfo, boolean[]> filePieces;
    private final boolean seeder;

    public ScheduledPeerToTrackerUpdateRequest(UUID peerId, Map<FileInfo, boolean[]> filePieces, boolean seeder) {
        this.peerId = peerId;
        this.filePieces = filePieces;
        this.seeder = seeder;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public Map<FileInfo, boolean[]> getFilePieces() {
        return filePieces;
    }

    public boolean isSeeder() {
        return seeder;
    }
}
