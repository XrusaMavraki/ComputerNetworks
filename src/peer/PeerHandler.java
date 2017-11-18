package peer;

import dto.*;
import server.Handler;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PeerHandler extends Handler {

    private final Peer peer;

    public PeerHandler() {
        this.peer = Peer.getInstance();
    }

    @Override
    public void handleMessage(Serializable message, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        if (message instanceof  CheckAliveRequest){
            oos.writeObject(peer.respondAlive());
        } else if(message instanceof DownloadRequest){
            oos.writeObject(peer.sendFile(((DownloadRequest) message).getFileName()));
        } else if(message instanceof ScheduledTrackerToPeersUpdateRequest){
            oos.writeObject(peer.handleTrackerUpdate((ScheduledTrackerToPeersUpdateRequest)message));
        } else if (message instanceof SendPiecedFileRequest) {
            oos.writeObject(peer.handlePiecedFileRequest((SendPiecedFileRequest)message));
        } else if (message instanceof SendPiecedFileResponse) {
            oos.writeObject(peer.receiveFilePiece((SendPiecedFileResponse)message));
        } else {
            System.out.println("Unknown message came to peer");
        }
    }
}
