import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpServer {
    String ip;
    int port;

    public UdpServer(String ip, int port){
        this.ip = ip;
        this.port = port;
    }


    public void sendMessage(String message) throws Exception{
        
        DatagramSocket serverSocket = new DatagramSocket();

        InetAddress IPAddress = InetAddress.getByName(this.ip);

        byte[] sendData = new byte[1024]; 

        sendData = message.getBytes();

        DatagramPacket sendPacket = 
        new DatagramPacket(sendData, sendData.length, IPAddress, this.port);

        serverSocket.send(sendPacket);

        serverSocket.close();
    }
    
}
