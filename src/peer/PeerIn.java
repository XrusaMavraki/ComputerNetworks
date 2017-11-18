package peer;

import dto.*;

import java.util.UUID;

/**
 * Created by xrusa on 10/4/2017.
 */
public interface PeerIn {

     RegisterResponse register(String username, String password);
     LoginSecondResponse login(String username, String password);
     LogoutResponse logout(UUID token_id);
     CheckAliveResponse respondAlive();
     SearchResponse search(String nameFile);
     int simpleDownload(String fileName);
     DownloadSendResponse sendFile(String nameFile);
     GenericOkResponse handlePiecedFileRequest(SendPiecedFileRequest request);
}
