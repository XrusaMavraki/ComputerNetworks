package peer;

import Util.*;
import dto.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static Util.SocketMagic.doWithSocket;
import static dto.CheckAliveResponse.CheckAliveType.YES_BABY;
import static dto.DownloadSendResponse.DownloadResponseType.FAIL_SEND;
import static dto.DownloadSendResponse.DownloadResponseType.SUCCESS_SEND;
import static dto.LoginResponse.LoginResponseType.FAIL_WRONG_CREDENTIALS;
import static dto.SearchResponse.SearchResponseType.SUCCESS_SEARCH;
import static dto.UpdateTrackerResponse.UpdateResponseType.FAIL_UPDATE;

/**
 * Created by xrusa on 10/4/2017.
 */

public class Peer implements PeerIn{

    private static final long TRACKER_UPDATER_INTERVAL_MS = 300L;
    private static final long SEEDER_WAIT_TIME_MS = 120L;
    private static final long PEER_WAIT_TIME_MS = 100L;

    private static Peer instance;
    private static  String TRACKER_HOST ;
    private static  int TRACKER_PORT = 12345;
    private String peerIp;
    private int peerPort;
    private String sharedDirectoryPath;
    private UUID token_id;
    private Set<FileInfo> sharedDirectoryFiles;
    private Set<PeerCommunicationInformation> seeders;
    private Map<PeerCommunicationInformation, Map<FileInfo, boolean[]>> peerToFilePieces;
    private Map<String, List<Pair<Integer,PeerCommunicationInformation>>> fileNameToUserWhoSentIt;
    private Map<PeerCommunicationInformation, Instant> mostRecentUsers;
    private ScheduledExecutorService trackerUpdaterExecutor;
    private IncomingRequestsDaemon incomingRequestsDaemon;
    private PeerCommunicationInformation thisPeer;
    private Map<FileInfo,boolean[]> thisPeerFilePieces;
    private Map<PeerCommunicationInformation,Integer> userToNumberOfSentFiles;
    private List<FileInfo> filesToDownload;
    private boolean seeder=false;
    private String propertiesPath;
    private Peer() {

        seeders = new HashSet<>();
        peerToFilePieces = new ConcurrentHashMap<>();
        fileNameToUserWhoSentIt = new ConcurrentHashMap<>();
        userToNumberOfSentFiles = new ConcurrentHashMap<>();
        trackerUpdaterExecutor = Executors.newScheduledThreadPool(1);
        // ConcurrentSkipListSet is a data structure that is suitable for multithreaded applications
        // that need a sorted set.
        incomingRequestsDaemon = new IncomingRequestsDaemon();
        mostRecentUsers = new ConcurrentHashMap<>();
        filesToDownload = new ArrayList<>();
    }

    public static Peer getInstance() {
        if (instance == null) {
            createNewInstance();
        }
        return instance;

    }

    private static synchronized void createNewInstance() {
        if (instance == null) {
            instance = new Peer();
        }
    }

    public void setSeeder(){
        seeder=true;
    }
    public void setPropertiesPath(String path){
        propertiesPath=path;
        try {
            loadProperties(propertiesPath);
            fillSharedDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void fillSharedDirectory(){
        sharedDirectoryFiles = ConcurrentHashMap.newKeySet();
        thisPeerFilePieces = new ConcurrentHashMap<>();
        File folder = new File(sharedDirectoryPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                FileInfo fileInfo = new FileInfo(file.getName(), file.length(), calculateNumberOfPieces(file.length()));
                // we make the assumption that every file in the shared folder is completed.
                sharedDirectoryFiles.add(fileInfo);
                thisPeerFilePieces.put(fileInfo, BooleanOperations.createBooleanOfCompletedFile(fileInfo.getNumParts()));
            }
        }
    }

    private static int calculateNumberOfPieces(long fileLength){
        return (int)Math.ceil(fileLength/10.0);
    }

    private <RES> RES doWithSocketToTracker(ExceptionalBiFunction<ObjectInputStream, ObjectOutputStream, RES> function) {
        return doWithSocket(TRACKER_HOST, TRACKER_PORT, function);
    }

    public void loadProperties(String propFile) throws IOException {
        Properties pro = new Properties();
        pro.load(Files.newBufferedReader(Paths.get(propertiesPath)));
        sharedDirectoryPath = pro.getProperty("sharedFolderpath");
        TRACKER_HOST = pro.getProperty("trackerIP");
        TRACKER_PORT = Integer.parseInt(pro.getProperty("trackerPort"));
        peerIp = pro.getProperty("peerIP");
    }

    public void setToken_id(UUID token_id) {
        this.token_id = token_id;
    }

    public UUID getToken_id() {
        return token_id;
    }

    public GenericOkResponse handleTrackerUpdate(ScheduledTrackerToPeersUpdateRequest req){
//        System.out.println("Received tracker update.");
        seeders.addAll(req.getSeeders());
        peerToFilePieces = req.getPeerToFilePieces();
        peerToFilePieces.remove(thisPeer);
        for (PeerCommunicationInformation peer : req.getPeerToFilePieces().keySet()) {
            // add new users
            mostRecentUsers.putIfAbsent(peer, Instant.MIN);
        }
        // remove users that may have disconnected
        mostRecentUsers.keySet().removeIf(peer -> !peerToFilePieces.containsKey(peer));
        // we insert into this peer's map new file infos that we get from the tracker
        // and see for the first time
        for (Map<FileInfo, boolean[]> filePieces : peerToFilePieces.values()) {
            for (FileInfo fileInfo : filePieces.keySet()) {
                thisPeerFilePieces.putIfAbsent(fileInfo, new boolean[fileInfo.getNumParts()]);
            }
        }
        // we check if we are downloading a file and send requests
        if (!filesToDownload.isEmpty()) {
            FileInfo fileInfo = filesToDownload.get(0);
            send4Requests(fileInfo);
        }
        return new GenericOkResponse();
    }

    @Override
    public RegisterResponse register(String username, String password) {
        RegisterRequest registerRequest = new RegisterRequest(username,password);
        return doWithSocketToTracker((ois, oos) -> {
            oos.writeObject(registerRequest);
            return (RegisterResponse) ois.readObject();
        });
    }

    @Override
    public LoginSecondResponse login(String username, String password) {
        LoginRequest loginRequest= new LoginRequest(username,password);
        return doWithSocketToTracker((ois, oos)->{
            oos.writeObject(loginRequest);
            LoginResponse resp=(LoginResponse) ois.readObject();
            if(!resp.getLoginResponseType().equals(FAIL_WRONG_CREDENTIALS)){
                token_id = resp.getLoginUniqueId();
                LoginSecondRequest req2= new LoginSecondRequest(token_id, peerIp, peerPort, sharedDirectoryFiles, seeder);
                thisPeer = new PeerCommunicationInformation(peerIp,peerPort,username);
                oos.writeObject(req2);
                LoginSecondResponse loginSecondResponse = (LoginSecondResponse) ois.readObject();
                startTimers();
                return loginSecondResponse;
            }
            else {
                return null;
            }

        });
    }

    private void startTimers() {
        startTrackerUpdaterScheduler();
        if (seeder) {
            incomingRequestsDaemon.setWaitTimeMs(SEEDER_WAIT_TIME_MS);
            incomingRequestsDaemon.setHandlerMethod(this::handleIncomingFilePieceRequestsAsSeeder);
        } else {
            incomingRequestsDaemon.setWaitTimeMs(PEER_WAIT_TIME_MS);
            incomingRequestsDaemon.setHandlerMethod(this::handleIncomingFilePieceRequestsAsPeer);
        }
        incomingRequestsDaemon.startDaemon();
    }

    private void startTrackerUpdaterScheduler() {
        trackerUpdaterExecutor.scheduleAtFixedRate(() -> {
                    doWithSocketToTracker((ois, oos) -> {
                        oos.writeObject(new ScheduledPeerToTrackerUpdateRequest(token_id, thisPeerFilePieces, seeder));
                        return (GenericOkResponse) ois.readObject();
                    });
                },
                0L, TRACKER_UPDATER_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public LogoutResponse logout(UUID token_id) {
        stopTimers();
        LogoutRequest logoutRequest = new LogoutRequest(getToken_id());
        return doWithSocketToTracker((ois, oos) -> {
            oos.writeObject(logoutRequest);
            return (LogoutResponse) ois.readObject();
        });
    }

    private void stopTimers() {
        trackerUpdaterExecutor.shutdownNow();
        incomingRequestsDaemon.stopDaemon();
    }


    public CheckAliveResponse checkAlive(String ip, int port) {
        CheckAliveRequest req= new CheckAliveRequest();
        return doWithSocket(ip,port,(ois,oos)->{
            oos.writeObject(req);
            return (CheckAliveResponse) ois.readObject();
        });
    }
    @Override
    public CheckAliveResponse respondAlive() {
        return new CheckAliveResponse(YES_BABY);
    }

    @Override
    public SearchResponse search(String nameFile) {
        SearchRequest searchreq= new SearchRequest(nameFile);
        return doWithSocketToTracker((ois, oos)->{
            oos.writeObject(searchreq);
            return (SearchResponse) ois.readObject();
        });
    }

    @Override
    public int simpleDownload(String fileName) {
        SearchResponse resp = search(fileName);
        if (resp.getSearchType() == SUCCESS_SEARCH) {
            Collection<PeerCommunicationInformation> usersWithFile = resp.getUsersWithFile();
            PeerCommunicationInformation bestUser = calculateBestUser(usersWithFile);

            DownloadSendResponse respDown=simpleDownloadRequest(fileName, bestUser);
            while(respDown.getType()!=SUCCESS_SEND &&(!usersWithFile.isEmpty())){
                usersWithFile.remove(bestUser);
                bestUser = calculateBestUser(usersWithFile);
                respDown = simpleDownloadRequest(fileName,bestUser);
            }
            if(usersWithFile.isEmpty()){
                return 1;
            }
            if (respDown.getType() == SUCCESS_SEND) {
                byte[] arr = respDown.getFile();
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(Paths.get(sharedDirectoryPath, fileName).toString());
                    fos.write(arr);
                    fos.close();
                }  catch (IOException e) {
                    e.printStackTrace();
                }

                FileInfo fileInfo = new FileInfo(fileName, respDown.getFile().length, calculateNumberOfPieces(respDown.getFile().length));
                sharedDirectoryFiles.add(fileInfo);
                thisPeerFilePieces.put(fileInfo, BooleanOperations.createBooleanOfCompletedFile(fileInfo.getNumParts()));
                sharedDirectoryFiles.add(fileInfo);

                UpdateTrackerResponse respUp = updateTracker(fileName,bestUser.getUserName());
                while (respUp.getType() == FAIL_UPDATE) {
                    respUp = updateTracker(fileName,bestUser.getUserName());
                }
            }
            return 0;
        }
        else{
            return 1;
        }
    }

    @Override
    public GenericOkResponse handlePiecedFileRequest(SendPiecedFileRequest request) {
        incomingRequestsDaemon.addRequest(request);
        return new GenericOkResponse();
    }

    private DownloadSendResponse simpleDownloadRequest(String fileName, PeerCommunicationInformation bestUser){
        DownloadRequest req = new DownloadRequest(fileName);
        //doWithSocketForPeer downloadReq apo edo tha paro DownloadSendResponse
        return doWithSocket(bestUser.getIPAddress(), bestUser.getPort(), (ois, oos) -> {
            oos.writeObject(req);
            return  (DownloadSendResponse) ois.readObject();
        });
    }

    public void handleIncomingFilePieceRequestsAsSeeder(Collection<SendPiecedFileRequest> requests) {
        // associate requests by peer that sent them
        Map<PeerCommunicationInformation, List<SendPiecedFileRequest>> requestsByPeer = requests.stream()
                .collect(Collectors.groupingBy(SendPiecedFileRequest::getRequestingPeer));

        PeerCommunicationInformation peer = findRandomPeer(requestsByPeer.keySet());
        sendRandomPieceToPeer(peer, requestsByPeer.get(peer));
    }

    public void handleIncomingFilePieceRequestsAsPeer(Collection<SendPiecedFileRequest> requests) {
        Random random = new Random();
        Map<PeerCommunicationInformation, List<SendPiecedFileRequest>> requestsByPeer = requests.stream()
                .collect(Collectors.groupingBy(SendPiecedFileRequest::getRequestingPeer));

        float roll = random.nextFloat();
        PeerCommunicationInformation selectedPeer;
        if (roll < 0.25F) {
            // send piece to random requester
            selectedPeer = findRandomPeer(requestsByPeer.keySet());
        } else {
            // send piece to the peer that has sent me the most pieces
            selectedPeer = findMostValuablePeer(requestsByPeer.keySet());
        }
        // send a random piece to the peer
        sendRandomPieceToPeer(selectedPeer, requestsByPeer.get(selectedPeer));

        // request back a piece that is missing from us.
        if (!filesToDownload.isEmpty()) {
            FileInfo fileDownloading = filesToDownload.get(0);
            List<Integer> piecesRequest = new ArrayList<>(BooleanOperations.compareArrayRightHasWhatLeftMisses(thisPeerFilePieces.get(fileDownloading), peerToFilePieces.get(selectedPeer).get(fileDownloading)));
            if (!piecesRequest.isEmpty()) {
                SendPiecedFileRequest req = new SendPiecedFileRequest(thisPeer, fileDownloading, piecesRequest);
                doWithSocket(selectedPeer.getIPAddress(), selectedPeer.getPort(), (ois, oos) -> {

                    oos.writeObject(req);
                    return ois.readObject();
                });
            }
        }
    }

    private PeerCommunicationInformation findMostValuablePeer(Collection<PeerCommunicationInformation> peers) {
        // find the most pieces sent by the requesting peers
        int mostPiecesSent = userToNumberOfSentFiles.entrySet().stream()
                .filter(e -> peers.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(-1);

        // get a list of the requesting peers that have sent that much pieces.
        // there may be more than one
        List<PeerCommunicationInformation> mostValuablePeers = userToNumberOfSentFiles.entrySet().stream()
                .filter(e -> peers.contains(e.getKey()))
                .filter(e -> e.getValue() == mostPiecesSent)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!mostValuablePeers.isEmpty()) {
            // in case there is at least one mvp peer, send to one of them (works with only 1 as well)
            return findRandomPeer(mostValuablePeers);
        } else {
            // in case there are no mvp peers, because no one of the requesting peers has ever sent anything to us,
            // fallback to random peer.
            return findRandomPeer(peers);
        }
    }

    private PeerCommunicationInformation findRandomPeer(Collection<PeerCommunicationInformation> peers) {
        Random random = new Random();
        // Select a random peer
        List<PeerCommunicationInformation> requestingPeers = new ArrayList<>(peers);
        return requestingPeers.get(random.nextInt(requestingPeers.size()));
    }

    private void sendRandomPieceToPeer(PeerCommunicationInformation selectedPeer, List<SendPiecedFileRequest> requestsByPeer) {
        Random random = new Random();

        // Select a random request that this peer sent
        SendPiecedFileRequest randomRequest = requestsByPeer.get(random.nextInt(requestsByPeer.size()));

        // Select a random file piece from this request
        int randomFilePiece = randomRequest.getPiecesRequested().get(random.nextInt(randomRequest.getPiecesRequested().size()));

        // Send the file piece
        sendFilePiece(randomRequest.getFileInfo(), randomFilePiece, selectedPeer, thisPeer);
    }

    private void sendFilePiece(FileInfo info,int filenum,PeerCommunicationInformation user, PeerCommunicationInformation thisPeer) {
        File file = new File(sharedDirectoryPath+ File.separator +info.getFileName());

        byte[] piece = readFromFile(file, filenum);
        SendPiecedFileResponse pieceSent= new SendPiecedFileResponse(info,piece,filenum,thisPeer);
        doWithSocket(user.getIPAddress(),user.getPort(),(ois,oos)->{
            System.out.println("Send user:"+ user.getUserName() +" file: "+info.getFileName() +"fileNumber: "+ filenum );
            oos.writeObject(pieceSent);
            return ois.readObject();
        });
    }

    public void collaborativeDownloadAllFiles(){
        Iterator iterat= seeders.iterator();
        PeerCommunicationInformation aSeeder= (PeerCommunicationInformation)iterat.next();
        for (Map.Entry<FileInfo,boolean[] > entry : peerToFilePieces.get(aSeeder).entrySet()){
            boolean[] myArray = thisPeerFilePieces.get(entry.getKey());
            if (myArray == null || BooleanOperations.piecesUserHas(myArray).isEmpty()) {
                filesToDownload.add(entry.getKey());
            }
        }
        // wait for download to finish
        while (!hasFinishedDownloading()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void collaborativeDownload(FileInfo info){
        filesToDownload.add(info);
        while (!hasFinishedDownloading()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public FileInfo doIHavethisFile(String filename){
        for(Map.Entry<FileInfo,boolean[]> entry : thisPeerFilePieces.entrySet()){
            if(entry.getKey().getFileName().equals(filename)){
                if(BooleanOperations.piecesUserIsMissing(entry.getValue()).isEmpty()){
                    return null;
                }
                else{
                    return entry.getKey();
                }
            }
        }
        return null;
    }


    public void send4Requests(FileInfo info){
        Map<PeerCommunicationInformation,SendPiecedFileRequest> requests = generateRequests(info);
        for(Map.Entry<PeerCommunicationInformation,SendPiecedFileRequest> entry: requests.entrySet()){
            CompletableFuture.runAsync(() -> doWithSocket(entry.getKey().getIPAddress(),entry.getKey().getPort(),(ois,oos)->{
                System.out.println("Sending request to user " + entry.getKey().getUserName() + " for file " + entry.getValue().getFileInfo().getFileName());
                oos.writeObject(entry.getValue());
                return (GenericOkResponse)ois.readObject();
            }));
        }
    }
    private Map<PeerCommunicationInformation,SendPiecedFileRequest> generateRequests(FileInfo info){
        boolean[] myArray= thisPeerFilePieces.get(info);
        Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces= usersWithFilePieces(info,myArray);
        Map<PeerCommunicationInformation, SendPiecedFileRequest> requests;
        if (usersWithFilePieces.size()<=4){
            requests=generateLessRequests(usersWithFilePieces,info,myArray);
        }
        else {
             requests = generateMostRecentRequests(usersWithFilePieces, info, myArray);
            for (Map.Entry<PeerCommunicationInformation, SendPiecedFileRequest> entry : requests.entrySet()) {
                usersWithFilePieces.remove(entry.getKey());
            }
            requests.putAll(generate2RandomUsers(usersWithFilePieces, info, myArray));
        }
        return requests;
    }
    private Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces(FileInfo info,boolean[] myArray){

        Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces= new HashMap<>();
        for(Map.Entry<PeerCommunicationInformation,Map<FileInfo,boolean[]>> entry : peerToFilePieces.entrySet()) {
            boolean[] hisArray = entry.getValue().get(info);
            List<Integer> piecesList = BooleanOperations.compareArrayRightHasWhatLeftMisses(myArray, hisArray);
            if (piecesList.isEmpty()) continue;
            usersWithFilePieces.put(entry.getKey(),entry.getValue());

        }
        return  usersWithFilePieces;
    }

    private  Map<PeerCommunicationInformation,SendPiecedFileRequest> generateMostRecentRequests(Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces,FileInfo info,boolean[] myArray){

        List<PeerCommunicationInformation> sortedMostRecentUsers = mostRecentUsers.entrySet().stream()
                .filter(entry -> usersWithFilePieces.containsKey(entry.getKey()))
                .sorted(Comparator.comparing((Map.Entry<PeerCommunicationInformation, Instant> entry) -> entry.getValue()).reversed())
                .map(Map.Entry::getKey)
                .limit(2)
                .collect(Collectors.toList());
        List<Integer> piecesRequest;

        Map<PeerCommunicationInformation,SendPiecedFileRequest> requests= new HashMap<>();
        for(PeerCommunicationInformation user : sortedMostRecentUsers){
            piecesRequest = BooleanOperations.compareArrayRightHasWhatLeftMisses(myArray, usersWithFilePieces.get(user).get(info));
            SendPiecedFileRequest req = new SendPiecedFileRequest(thisPeer, info, piecesRequest);
            requests.put(user,req);
        }
        return requests;
    }

    private Map<PeerCommunicationInformation,SendPiecedFileRequest> generate2RandomUsers(Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces,FileInfo info, boolean[] myArray){
        List<PeerCommunicationInformation> keysAsArray = new ArrayList<>(usersWithFilePieces.keySet());
        Random r = new Random();
        PeerCommunicationInformation key=keysAsArray.get(r.nextInt(keysAsArray.size()));
        Map<FileInfo,boolean[]> value=usersWithFilePieces.get(key);
        Map<PeerCommunicationInformation,SendPiecedFileRequest> requests= new HashMap<>();
        List<Integer> piecesRequest;
        piecesRequest = BooleanOperations.compareArrayRightHasWhatLeftMisses(myArray,value.get(info));
        SendPiecedFileRequest req = new SendPiecedFileRequest(thisPeer, info, piecesRequest);
        requests.put(key,req);
        keysAsArray.remove(key); //so we dont get it again
        //second random
        key=keysAsArray.get(r.nextInt(keysAsArray.size()));
        value=usersWithFilePieces.get(key);
        piecesRequest = BooleanOperations.compareArrayRightHasWhatLeftMisses(myArray,value.get(info));
        req = new SendPiecedFileRequest(thisPeer, info, piecesRequest);
        requests.put(key,req);
        return requests;
    }

    private Map<PeerCommunicationInformation,SendPiecedFileRequest> generateLessRequests(Map<PeerCommunicationInformation,Map<FileInfo,boolean[]>> usersWithFilePieces,FileInfo info, boolean[] myArray){
        Map<PeerCommunicationInformation,SendPiecedFileRequest> requests= new HashMap<>();
        List<Integer> piecesRequest;
        SendPiecedFileRequest req ;
        for(Map.Entry<PeerCommunicationInformation,Map<FileInfo,boolean[]>> entry : usersWithFilePieces.entrySet()) {
            piecesRequest = BooleanOperations.compareArrayRightHasWhatLeftMisses(myArray,entry.getValue().get(info));
            req = new SendPiecedFileRequest(thisPeer, info, piecesRequest);
            requests.put(entry.getKey(),req);
        }
        return requests;
    }

    public GenericOkResponse receiveFilePiece(SendPiecedFileResponse req){
        System.out.println("Received file piece :D");
        System.out.println("Part number " + req.getPiecedFileNumber());
        FileInfo info= req.getFileInfo();
        String filename= info.getFileName();
        byte[] piece = req.getPiecedFile();
        int pieceNum = req.getPiecedFileNumber();
        PeerCommunicationInformation userWhoSentIt = req.getUsernameWhoSentIt();
        mostRecentUsers.put(userWhoSentIt,Instant.now());

        List<Pair<Integer,PeerCommunicationInformation>> list =fileNameToUserWhoSentIt.get(filename);
        if( list== null){
            list= new ArrayList<>();
            fileNameToUserWhoSentIt.put(filename,list);
        }
        list.add(new Pair<>(pieceNum,userWhoSentIt));

        int number=userToNumberOfSentFiles.getOrDefault(userWhoSentIt,0); //0 default?
        if(number!=0){
            userToNumberOfSentFiles.put(userWhoSentIt,++number);
        }else{
            userToNumberOfSentFiles.put(userWhoSentIt,1);
        }

        writeToFile(new File(sharedDirectoryPath + File.separator + filename), pieceNum, piece);
        boolean[] array= thisPeerFilePieces.get(info);
        array[pieceNum]=true;
        System.out.println("Remaining : " + BooleanOperations.piecesUserIsMissing(array).size());
        if (BooleanOperations.piecesUserIsMissing(array).isEmpty()) {
            // the file has been completed
            System.out.println("The file " + filename + " has been downloaded.");
            filesToDownload.remove(info);
        }
        checkAmISeederYet();
        return new GenericOkResponse();
    }
    private void checkAmISeederYet(){
        Iterator iterat= seeders.iterator();
        PeerCommunicationInformation aSeeder= (PeerCommunicationInformation)iterat.next();
        Map<FileInfo,boolean[]> seederMap=peerToFilePieces.get(aSeeder);
        boolean seeder=true;
        for(Map.Entry<FileInfo,boolean[]> entry: seederMap.entrySet()){
            if (!(thisPeerFilePieces.containsKey(entry.getKey())&&Arrays.equals(thisPeerFilePieces.get(entry.getKey()),entry.getValue()))){
                seeder=false;
            }
        }
            if(seeder) {
                System.out.println("I AM A SEEDER NOW");
                setSeeder();
            }

    }
    private static byte[] readFromFile(File file1, int position) {
        try (RandomAccessFile file = new RandomAccessFile(file1, "r")) {
            file.seek(position*10);
            byte[] bytes = new byte[10];
            file.read(bytes);
            return bytes;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void writeToFile(File file1, int position, byte[] bytes) {
        try (RandomAccessFile file = new RandomAccessFile(file1, "rw")) {
            file.seek(position * 10);
            file.write(bytes);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private PeerCommunicationInformation calculateBestUser (Collection<PeerCommunicationInformation> usersWithFile) {
        long estimatedTimeMin = Long.MAX_VALUE;

        PeerCommunicationInformation bestUser = usersWithFile.iterator().next();//gia na ginei apla initialised
        for (PeerCommunicationInformation user : usersWithFile) {
            long startTime = System.nanoTime();
            doWithSocket(user.getIPAddress() , user.getPort(),(ois,oos)->{
                oos.writeObject(new CheckAliveRequest());
                return (CheckAliveResponse) ois.readObject();
            });
            long timeElapsed = System.nanoTime() - startTime;
            estimatedTimeMin = Math.min(estimatedTimeMin, timeElapsed);
            if (estimatedTimeMin == timeElapsed) {
                bestUser = user;
            }
        }
        System.out.println( "Best user is: "+bestUser.getUserName()+" Elapsed time was: "+ estimatedTimeMin);
        return bestUser;
    }

    private UpdateTrackerResponse updateTracker(String fileName,String userThatSendMeFile){
        UpdateTrackerRequest upd= new UpdateTrackerRequest(fileName,this.getToken_id(),userThatSendMeFile);
        return doWithSocketToTracker((ois, oos)->{
            oos.writeObject(upd);
            return (UpdateTrackerResponse)ois.readObject();
        });
    }

    @Override
    public DownloadSendResponse sendFile(String nameFile) {
        byte [] array ;
        DownloadSendResponse downResponse;
        try {
            array= Files.readAllBytes(Paths.get(sharedDirectoryPath,nameFile));
            downResponse= new DownloadSendResponse(SUCCESS_SEND,array);
        } catch (IOException e) {
            downResponse= new DownloadSendResponse(FAIL_SEND,null);
            e.printStackTrace();
        }
        return downResponse;
    }

    public boolean hasFinishedDownloading() {
        return filesToDownload.isEmpty();
    }

    public void setPeerPort(int peerPort) {
        this.peerPort = peerPort;
    }
}
