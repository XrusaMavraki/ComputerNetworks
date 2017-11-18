package tracker;

import dto.*;

/**
 * Created by xrusa on 10/4/2017.
 */
public interface TrackerIn {
    RegisterResponse register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
    LoginSecondResponse login2(LoginSecondRequest loginSecondRequest);
    LogoutResponse logout(LogoutRequest logoutRequest);
    SearchResponse search(SearchRequest searchRequest);
    UpdateTrackerResponse update(UpdateTrackerRequest updReq);
    GenericOkResponse handlePeerUpdate(ScheduledPeerToTrackerUpdateRequest updReq);
}
