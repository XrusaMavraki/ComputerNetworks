package dto;

import Util.FileInfo;
import Util.PeerCommunicationInformation;

import java.io.Serializable;
import java.util.List;

/**
 * Created by xrusa on 27/5/2017.
 */
public class SendPiecedFileRequest implements Serializable {
    private final PeerCommunicationInformation requestingPeer;
    private final FileInfo fileInfo;
    private final List<Integer> piecesRequested;

    public SendPiecedFileRequest(PeerCommunicationInformation requestingPeer, FileInfo fileInfo, List<Integer> piecesRequested) {
        this.requestingPeer = requestingPeer;
        this.fileInfo = fileInfo;
        this.piecesRequested = piecesRequested;
    }

    public PeerCommunicationInformation getRequestingPeer() {
        return requestingPeer;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public List<Integer> getPiecesRequested() {
        return piecesRequested;
    }
}
