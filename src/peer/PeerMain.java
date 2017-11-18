package peer;

import Util.FileInfo;
import Util.Pair;
import Util.PeerCommunicationInformation;
import dto.*;
import server.SocketServer;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static dto.CheckAliveResponse.CheckAliveType.YES_BABY;
import static dto.SearchResponse.SearchResponseType.SUCCESS_SEARCH;

public class PeerMain {

    public static void main(String[] args) {
        System.out.println("Starting Peer Server...");
        Peer peer= Peer.getInstance();
        SocketServer socketServer = new SocketServer(0,8, PeerHandler::new);
        // start peer server on the background.
        Thread thread = new Thread(socketServer);
        thread.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        peer.setPeerPort(socketServer.getPort());
        if(args.length >= 2 && args[1].equals("seeder") ) {
            System.out.println("Starting as seeder");
            peer.setSeeder();
        }
        if(args.length>=1){
            String propPath=args[0];
            peer.setPropertiesPath(propPath);
        }

        Scanner scanner= new Scanner(System.in);
        handleMenu1(scanner ,peer);
    }
    private static void handleMenu1(Scanner scanner, Peer peer){
    	   int userInput=printmenu(scanner);
           switch(userInput){
               case 1: {
                   int resp=handleRegister(peer,scanner); // tha rota to xristi klp
                   if (resp==0){
                   	handleMenu1(scanner,peer);
                   }
                   break;
               }
               case 2: {
                   if( handleLogin(peer,scanner)==1){
                      handleMenu(scanner,peer);
                   }
                   break;
               }
               case 0:
                   System.exit(0);
           }
    }
    private static void handleMenu(Scanner scanner,Peer peer){
    	 int men=printmenu2(scanner);
         switch (men){
             case 1: {
                 handleLogout(peer);
                 break;
             }
             case 2: {
                 handleCheckAlive(peer,scanner);
                handleMenu(scanner, peer);
                 break;
                
             }
             case 3:{
                 handleSearch(peer,scanner);
                 handleMenu(scanner, peer);
                 break;
             }
             case 4:{
                 handleTypeOfDownload(peer,scanner);
                 handleMenu(scanner, peer);
                 break;
             }
             case 0: System.exit(0);
         }
    }
    private static int printmenu(Scanner scanner){

        System.out.println("What would you like to do:");
        System.out.println("Press 1 to Register");
        System.out.println("Press 2 to Login");
        System.out.println("Press 0 to exit");
        return Integer.parseInt(scanner.nextLine().trim());
    }
    private static int printmenu2(Scanner scanner){
        System.out.println("Press 1 to logout");
        System.out.println("Press 2 to check if someone is alive");
        System.out.println("Press 3 to search for a file");
        System.out.println("Press 4 to download a file");
        System.out.println("Press 0 to exit");
        int value=    Integer.parseInt(scanner.nextLine().trim());
        return value;
    }
    private static int handleRegister(Peer peer,Scanner scanner){
        Pair<String,String> userPas=requestUserPass(scanner);
        RegisterResponse resp=peer.register(userPas.getLeft(),userPas.getRight());
        if(!(resp.getRegisterResponseType()==RegisterResponse.RegisterResponseType.SUCCESS_REGISTER)) {
            if (resp.getRegisterResponseType() == RegisterResponse.RegisterResponseType.FAIL_PASSWORD_EMPTY) {
                System.out.println("You entered an empty password. Please try again");
                return 0;
            }
            else if(resp.getRegisterResponseType() == RegisterResponse.RegisterResponseType.FAIL_USERNAME_ALREADY_EXISTS){
                System.out.println("You entered a username that is already taken. Please try again.");
                return 0;
            }
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("You registered successfully");
            return 0;
        }
        return 1;


    }
    private static int handleLogin(Peer peer,Scanner scanner){
        Pair<String,String> userPas= requestUserPass(scanner);
        LoginSecondResponse resp= peer.login(userPas.getLeft(),userPas.getRight());
        if (resp==null){
            System.out.println("Something went wrong please retry");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handleLogin(peer,scanner);
        }
        else{
            System.out.println("You logged in successfully");
            return 1;

        }
        return 0;
    }
    private static void handleLogout(Peer peer){
        LogoutResponse resp= peer.logout(peer.getToken_id());
        while(resp.getLogoutResponseType()!= LogoutResponse.LogoutResponseType.SUCCESS_LOGOUT){
            resp=peer.logout(peer.getToken_id());
        }
        System.out.println("You have logged out successfully");
        System.exit(0);
    }
    private static void handleSearch(Peer peer,Scanner scanner){
        System.out.println("Please enter the name of the file you want to search");
        String filename= scanner.nextLine().trim();
        SearchResponse resp = peer.search(filename);
        if(resp.getSearchType()==SUCCESS_SEARCH) {
            System.out.println("The users with this file are: ");
            for(PeerCommunicationInformation peerCommInfo : resp.getUsersWithFile()){
                System.out.println("UserName: " + peerCommInfo.getUserName());
                System.out.println("Ip Address: " + peerCommInfo.getIPAddress());
                System.out.println("Port: " + peerCommInfo.getPort());
            }
        }
        else{
            System.out.println("No users had a file with this name");
        }
    }
    private static void handleTypeOfDownload(Peer peer,Scanner scanner){
        System.out.println("Do you want to do simpleDownload or collaborativeDownload?");
        System.out.println("Press 1 for simpleDownload 2 for collaborativeDownload");
        int userInput;
        do {
            userInput=  Integer.parseInt(scanner.nextLine().trim());
            if (userInput == 1) {
                handleDownload(peer, scanner);
            } else if (userInput == 2) {
                handleCollaborativeDownload(peer, scanner);
            } else {
                System.out.println("Wrong input press 1 or 2");
            }
        }while (userInput!=1 &&userInput!=2);
    }
    private static void handleDownload(Peer peer, Scanner scanner){
        System.out.println("Please enter the name of the file you want to search");
        String filename= scanner.nextLine().trim();
        int state= peer.simpleDownload(filename);
        if(state==1){
            System.out.println("No users had a file with this name");
        }
        else{
            System.out.println("File Downloaded succesffuly, tracker updated");
        }
    }
    private static void handleCollaborativeDownload(Peer peer, Scanner scanner){
        System.out.println("Do you want to download all files collaboratively? Press 1 for yes 2 for no");
        int userInput=  Integer.parseInt(scanner.nextLine().trim());
        if(userInput==1){
            peer.collaborativeDownloadAllFiles();
        }
        else{
            System.out.println("Which file do you want to download collaboratively?");
            String filename= scanner.nextLine().trim();
            FileInfo info= peer.doIHavethisFile(filename);

            if(info==null){
                System.out.println("You either have this file already or it doesn't exist");
            }
            else{
                peer.collaborativeDownload(info);
            }
        }
    }
    private static void handleCheckAlive(Peer peer,Scanner scanner){
        String ip;
        int port;
        do {
            System.out.println("Please enter the ip of the peer you want to checkAlive");
            ip = scanner.nextLine().trim();
        }while(!validIP(ip));
        do {
            System.out.println("Please enter the port of the peer you want to checkAlive");
            port = Integer.parseInt(scanner.nextLine().trim());
        } while( port<=0 || port>65535);
        CheckAliveResponse resp=peer.checkAlive(ip,port);
        if (resp.getCheck()== YES_BABY){
            System.out.println("It's alive!!!");
        }
    }
    private static Pair<String,String> requestUserPass(Scanner scanner){
        String username;
        String password;
        System.out.println("Please enter username");
        username=scanner.nextLine().trim();
        System.out.println("Please enter password");
        password=scanner.nextLine().trim();
        return new Pair<>(username, password);
    }

    private static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


}
