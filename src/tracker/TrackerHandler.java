package tracker;

import dto.*;
import server.Handler;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TrackerHandler extends Handler {

    private final TrackerIn tracker;

    public TrackerHandler() {
        this.tracker = Tracker.getInstance();
    }

    @Override
    public void handleMessage(Serializable message, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        if (message instanceof LoginRequest) {
            LoginResponse loginResponse = tracker.login((LoginRequest) message);
            oos.writeObject(loginResponse);
            if (loginResponse.getLoginResponseType() == LoginResponse.LoginResponseType.SUCCESS_LOGIN) {
                oos.writeObject(tracker.login2((LoginSecondRequest) ois.readObject()));
            }
        }
        else if (message instanceof LogoutRequest)
            oos.writeObject(tracker.logout((LogoutRequest) message));
        else if (message instanceof RegisterRequest)
            oos.writeObject(tracker.register((RegisterRequest) message));
        else if (message instanceof UpdateTrackerRequest)
            oos.writeObject(tracker.update((UpdateTrackerRequest) message));
        else if (message instanceof SearchRequest)
            oos.writeObject(tracker.search((SearchRequest) message));
        else if (message instanceof ScheduledPeerToTrackerUpdateRequest) {
            oos.writeObject(tracker.handlePeerUpdate((ScheduledPeerToTrackerUpdateRequest) message));
        }
        else
            System.out.println("Unknown message type received on Tracker");
    }

}
