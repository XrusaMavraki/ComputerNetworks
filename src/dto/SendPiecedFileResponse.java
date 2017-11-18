package dto;

import Util.FileInfo;
import Util.PeerCommunicationInformation;

import java.io.Serializable;

/**
 * Created by xrusa on 21/5/2017.
 */
public class SendPiecedFileResponse implements Serializable {
    private final FileInfo fileInfo;
    private final byte[] piecedFile;
    private final int piecedFileNumber;
    private final PeerCommunicationInformation usernameWhoSentIt;

    public SendPiecedFileResponse(FileInfo fileInfo, byte[] piecedFile, int piecedFileNumber, PeerCommunicationInformation usernameWhoSentIt) {
        this.fileInfo=fileInfo;
        this.piecedFile = piecedFile;
        this.piecedFileNumber = piecedFileNumber;
        this.usernameWhoSentIt = usernameWhoSentIt;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public byte[] getPiecedFile() {
        return piecedFile;
    }

    public int getPiecedFileNumber() {
        return piecedFileNumber;
    }

    public PeerCommunicationInformation getUsernameWhoSentIt() {
        return usernameWhoSentIt;
    }
}
