import jdk.nashorn.internal.runtime.ECMAErrors;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;
    private String server, username;
    private int port;

    Client(String server, int port, String username){
        this.server = server;
        this.port = port;
        this.username = username;
    }

    public boolean start(){
        try{
            socket = new Socket(server, port);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        System.out.println(msg);

        try{
            sOutput = new ObjectOutputStream(socket.getOutputStream());
            sInput = new ObjectInputStream(socket.getInputStream());
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        new ListenFromServer().start();

        try{
            sOutput.writeObject(username);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    void sendMessage(ChatMessage msg){
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect(){
        try{
            if(sInput != null)
                sInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            if(sOutput != null)
                sOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            if(socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Rusho";

        switch (args.length){
            case 3:
                serverAddress = args[2];
            case 2:
                portNumber = Integer.parseInt(args[1]);
            case 1:
                userName = args[0];
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
        }

        Client client = new Client(serverAddress, portNumber, userName);

        if(!client.start())
            return;

        Scanner scan = new Scanner(System.in);

        while(true){
            System.out.print("> ");
            String msg = scan.nextLine();

            if(msg.equalsIgnoreCase("LOGOUT")){
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }

            else if(msg.equalsIgnoreCase("WHOISIN")){
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            }

            else
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE ,msg));
        }

        client.disconnect();
    }

    class ListenFromServer extends Thread{
        public void run(){
            while(true){
                try{
                    String msg = (String) sInput.readObject();
                    System.out.print(msg + "\n> ");
                }catch (Exception e){
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
