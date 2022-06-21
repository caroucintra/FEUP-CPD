import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;



public class SimpleServer implements Runnable {
    String       ip;
    InetAddress  hostIp;
    int          port   = 8080;
    ServerSocket serverSocket = null;

    public SimpleServer(String ip, int port){
        this.ip = ip;
        this.port = port;
        try {
            hostIp = InetAddress.getByName(ip);
            try {
                this.serverSocket = new ServerSocket(this.port, 50, hostIp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public void run(){
        Socket socket;
        try {
            socket = serverSocket.accept();
            InputStream input  = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String parsedInput = reader.readLine();
            System.out.println(parsedInput);
            input.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


    }

    public void close(){
        try{
            this.serverSocket.close();
        System.out.println("Socket shut down\n");
        this.serverSocket = null;
        } catch (IOException e) { 
            System.out.println("Failed on shutting down socket\n");
        }
        
    }
    

}
