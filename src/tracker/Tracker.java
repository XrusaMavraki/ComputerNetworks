package tracker;

import Util.FileInfo;
import Util.Pair;
import Util.PeerCommunicationInformation;
import dto.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static Util.SocketMagic.doWithSocket;
import static dto.CheckAliveResponse.CheckAliveType.YES_BABY;
import static dto.LoginResponse.LoginResponseType.*;
import static dto.LoginSecondResponse.LoginSecondResponseType.SUCCESS_LOGIN1;
import static dto.LogoutResponse.LogoutResponseType.FAIL_LOGOUT;
import static dto.LogoutResponse.LogoutResponseType.SUCCESS_LOGOUT;
import static dto.RegisterResponse.RegisterResponseType.*;
import static dto.SearchResponse.SearchResponseType.FAIL_SEARCH;
import static dto.SearchResponse.SearchResponseType.SUCCESS_SEARCH;
import static dto.UpdateTrackerResponse.UpdateResponseType.SUCCESS_UPDATE;

/**
 * Created by xrusa on 10/4/2017.
 */
public class Tracker implements TrackerIn{

    private static Tracker instance;

    private final Set<String> existingUsernames;
    private final Set<Pair<String, String>> registeredUsernamesAndPasswords;
    private final Map<UUID, LoggedInUserInfo> loggedInUsers;
    private final Map<String, FileInfo> fileNameToFileInfo;
    private final Map<String, AtomicInteger> userNameToCountDownloads;
    private final Map<UUID,String> idToUserName;

    private Tracker() {
        existingUsernames = ConcurrentHashMap.newKeySet(); //to antistoixo tou set gia conccurent.
        registeredUsernamesAndPasswords = ConcurrentHashMap.newKeySet();
        loggedInUsers = new ConcurrentHashMap<>();
        userNameToCountDownloads = new ConcurrentHashMap<>();
        fileNameToFileInfo = new ConcurrentHashMap<>();
        idToUserName = new ConcurrentHashMap<>();
        scheduleUpdatesToPeers();
    }

    /**
     * Singleton pattern in order to ensure that only one object is ever created.
     * @return The tracker instance.
     */
    public static Tracker getInstance() {
        if (instance == null) {
            createNewInstance();
        }
        return instance;
    }

    private static synchronized void createNewInstance() {
        if (instance == null) {
            instance = new Tracker();
        }
    }

    private void scheduleUpdatesToPeers() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            List<PeerCommunicationInformation> seeders = loggedInUsers.values().stream()
                    .filter(LoggedInUserInfo::isSeeder)
                    .map(LoggedInUserInfo::getPeerCommunicationInformation)
                    .collect(Collectors.toList());

            Map<PeerCommunicationInformation, Map<FileInfo, boolean[]>> peerToFilePieces = loggedInUsers.values().stream()
                    .collect(Collectors.toMap(LoggedInUserInfo::getPeerCommunicationInformation, LoggedInUserInfo::getDownloadedFilePieces));
            ScheduledTrackerToPeersUpdateRequest scheduledTrackerToPeersUpdateRequest = new ScheduledTrackerToPeersUpdateRequest(seeders, peerToFilePieces);

            Set<LoggedInUserInfo> usersToDisconnect = new HashSet<>();
            for (LoggedInUserInfo userInfo : loggedInUsers.values()) {
                PeerCommunicationInformation com = userInfo.getPeerCommunicationInformation();
                CompletableFuture.runAsync(() -> {
                    try {
                        doWithSocket(com.getIPAddress(), com.getPort(), (ois, oos) -> {
                            oos.writeObject(scheduledTrackerToPeersUpdateRequest);
                            return ois.readObject();
                        });
                    } catch (Exception e) {
                        System.out.println("Has disconnected");
                        usersToDisconnect.add(userInfo);

                    }
                    for (LoggedInUserInfo discoUser : usersToDisconnect) {
                        doLogoutUser(discoUser.getUserId());
                    }});
            }


        }, 0L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        String user=registerRequest.getUserName();
        String pass=registerRequest.getPassword();
        Pair<String, String> pair= new Pair<>(user,pass);
        if(pass.equals("")){
        	 System.out.println("Register failed:  password empty");
            return new RegisterResponse(FAIL_PASSWORD_EMPTY);
           
        }
        else if(existingUsernames.contains(user)){
        	System.out.println("Register failed:  username exists");
            return new RegisterResponse(FAIL_USERNAME_ALREADY_EXISTS);
            
        }
        else {
            existingUsernames.add(user);
            registeredUsernamesAndPasswords.add(pair);
            System.out.println("Register successful Username: "+ user +" Password: "+ pass);
            return new RegisterResponse(SUCCESS_REGISTER);
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String user = loginRequest.getUserName();
        String pass = loginRequest.getPassword();
        if (!registeredUsernamesAndPasswords.contains(new Pair<>(user, pass))) {
        	System.out.println("Login failed:  Wrong credentials");
            return new LoginResponse(FAIL_WRONG_CREDENTIALS,null);
        }
        else{
            UUID loginID = UUID.randomUUID();
            idToUserName.put(loginID, user);
            System.out.println("Sucessfull login: Username:  "+ user +"  UUID: "+ loginID);
            return new LoginResponse(SUCCESS_LOGIN, loginID);
        }
    }

    @Override
    public LoginSecondResponse login2(LoginSecondRequest loginSecondRequest){
        String ipadd = loginSecondRequest.getIpAddress();
        int port = loginSecondRequest.getPort();
        UUID id = loginSecondRequest.getUniqueID();
        String username = idToUserName.get(id);
        PeerCommunicationInformation com = new PeerCommunicationInformation(ipadd, port, username);

        LoggedInUserInfo loggedInUserInfo = new LoggedInUserInfo(username, id, com);
        loggedInUserInfo.setSeeder(loginSecondRequest.isSeeder());
        loggedInUsers.put(id, loggedInUserInfo);
        idToUserName.remove(id);

        //arxikopoii to download value to user se 0 an den uparxei idi apo proigoumeni fora
        userNameToCountDownloads.putIfAbsent(username,new AtomicInteger(0));

        Set<FileInfo> files = loginSecondRequest.getSharedDirectoryFiles();
        for (FileInfo file : files) {
            fileNameToFileInfo.putIfAbsent(file.getFileName(), file);
            loggedInUserInfo.addFullyDownloadedFile(file);
        }

        if (userNameToCountDownloads.getOrDefault(username, new AtomicInteger(0)).get() == 0) {
            System.out.println("Penalizing user " + username + " with 0 downloads for 100 ms...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("User with UUID  "+id+"  Ip adress   "+com.getIPAddress()+"  Port  "+ com.getPort());
        return new LoginSecondResponse(SUCCESS_LOGIN1);
    }

    @Override
    public LogoutResponse logout(LogoutRequest logoutRequest) {
        final UUID tokenId = logoutRequest.getLogoutUuid();
        boolean userIsLoggedIn = loggedInUsers.containsKey(tokenId);
        doLogoutUser(tokenId);
        return userIsLoggedIn ? new LogoutResponse(SUCCESS_LOGOUT) : new LogoutResponse(FAIL_LOGOUT);
    }

    private void doLogoutUser(UUID uuid) {
        loggedInUsers.remove(uuid);
        System.out.println("Logout for user "+uuid +" was successfull");
    }

    private Set<LoggedInUserInfo> getUsersWithFile(String fileName) {
        FileInfo fileInfo = fileNameToFileInfo.get(fileName);
        return loggedInUsers.values().stream()
                .filter(user -> user.getFullyDownloadedFiles().contains(fileInfo))
                .collect(Collectors.toSet());
    }

    @Override
    public SearchResponse search(SearchRequest searchreq){
        String fileName = searchreq.getFileName();
        Set<PeerCommunicationInformation> usersWithFileCommunicationInformation = new HashSet<>();
        Set<LoggedInUserInfo> usersWithFile = getUsersWithFile(fileName);
        CheckAliveRequest req= new CheckAliveRequest();
        for (LoggedInUserInfo user : usersWithFile) {
            PeerCommunicationInformation inf= user.getPeerCommunicationInformation();
            System.out.println("Checking user: "+inf.getUserName()+"  Ip: "+inf.getIPAddress()+"  Port"+ inf.getPort());
            try{
                CheckAliveResponse resp = doWithSocket(inf.getIPAddress(),inf.getPort(),(ois,oos)->{
                    oos.writeObject(req);
                    return (CheckAliveResponse)ois.readObject();

                });
                if (resp.getCheck() == YES_BABY) {
                    usersWithFileCommunicationInformation.add(inf);
                    System.out.println("Search completed successfully.");
                }
            }catch(Exception e){
                doLogoutUser(user.getUserId());
                System.out.println("User with uuid "+ user.getUserId() +" was inactive and logged out");
            }
        }
        if(usersWithFileCommunicationInformation.isEmpty()){
            return new SearchResponse(FAIL_SEARCH,null);
        }
        else{
            return new SearchResponse(SUCCESS_SEARCH, usersWithFileCommunicationInformation);
        }
    }

    @Override
    public UpdateTrackerResponse update(UpdateTrackerRequest updateReq){
        String fileName = updateReq.getFileName();
        UUID id = updateReq.getUniqueID();
        loggedInUsers.get(id).addFullyDownloadedFile(fileNameToFileInfo.get(fileName));
        String userThatSentFile= updateReq.getUserThatSendMeFile();
        userNameToCountDownloads.putIfAbsent(userThatSentFile,new AtomicInteger(0)).incrementAndGet(); // prostheti ena sta downloads tou xristi pou estile.
        System.out.println("Tracker updated information: status success");
        return new UpdateTrackerResponse(SUCCESS_UPDATE);
    }

    @Override
    public GenericOkResponse handlePeerUpdate(ScheduledPeerToTrackerUpdateRequest updReq) {
        LoggedInUserInfo loggedInUserInfo = loggedInUsers.get(updReq.getPeerId());
        if(updReq.isSeeder()&&!loggedInUserInfo.isSeeder()){
            System.out.println("user: "+ loggedInUsers.get( updReq.getPeerId()).getUserName()+ " Is seeder now");
        }
        loggedInUserInfo.setSeeder(updReq.isSeeder());
        for (Map.Entry<FileInfo, boolean[]> entry : updReq.getFilePieces().entrySet()) {
            loggedInUserInfo.updateDownloadedFilePieces(entry.getKey(), entry.getValue());
        }
        return new GenericOkResponse();
    }

}
