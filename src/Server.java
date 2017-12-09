import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread>al;
    private int port;
    private boolean keepGoing;

    Server(int port){
        this.port = port;
        al = new ArrayList<>();
    }

    public void start(){
        keepGoing = true;

        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while(keepGoing){
                System.out.println("Server waiting for Clients on port " + port + ".");
                Socket socket  = serverSocket.accept();

                if(!keepGoing)
                    break;

                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
                if(al.isEmpty())
                    stop();
            }

            try{
                serverSocket.close();
                for(int i=0;  i<al.size(); i++) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sOutput.close();
                        tc.sInput.close();
                        tc.socket.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    protected void stop(){
        keepGoing = false;

        try {
            new Socket("localhost", port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void broadcast(String message){
        System.out.println(message);

        for(int i=0; i<al.size(); i++){
            ClientThread ct = al.get(i);
            if(!ct.writeMsg(message)){
                al.remove(i);
                System.out.println("Disconnected Client");
            }
        }
    }

    synchronized void remove(int id){
        for(int i=0; i<al.size(); i++){
            ClientThread ct = al.get(i);
            if(ct.id == id){
                al.remove(i);
                return;
            }
        }
    }

    public static void main(String[] args){
        int portNumber = 1530;

        switch (args.length){
            case 1:
                try{
                    portNumber = Integer.parseInt(args[0]);
                }catch (Exception e){
                    e.printStackTrace();
                }

            case 0:
                break;

            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }

        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread{
        Socket socket;
        ObjectOutputStream sOutput;
        ObjectInputStream sInput;
        String username;
        ChatMessage cm;
        int id;

        ClientThread(Socket socket){
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");

            try{
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                System.out.println(username + " just connected.");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            boolean keepGoing = true;

            while(keepGoing){
                try{
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                String message = cm.getMessage();

                switch (cm.getType()){
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;

                    case ChatMessage.LOGOUT:
                        System.out.println(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;

                    case ChatMessage.WHOISIN:
                        for(int i=0; i<al.size(); i++){
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username);
                        }
                        break;
                }
            }

            remove(id);
            close();
        }

        private void close(){
            try {
                if(sInput != null)
                    sInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(sInput != null)
                    sOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean writeMsg(String msg){
            if(!socket.isConnected()){
                close();
                return false;
            }

            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }
}
