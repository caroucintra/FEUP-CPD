import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import java.net.InetAddress;


public class UdpClient implements Runnable {
    private String IP_mcast_addr;
    private int IP_mcast_port;
    private boolean running = true;
    private Membership membership;

    public UdpClient(String IP_mcast_addr, int IP_mcast_port, Membership membership){
        this.IP_mcast_addr = IP_mcast_addr;
        this.IP_mcast_port = IP_mcast_port;
        this.membership = membership;        
    }

   @Override
   public void run(){
        try {
            receiveMsg();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public void receiveMsg() throws IOException {
        byte[] receiveData = new byte[1024];
        MulticastSocket clientSocket=new MulticastSocket(this.IP_mcast_port);
        InetAddress group=InetAddress.getByName(this.IP_mcast_addr);
        clientSocket.joinGroup(group);

        while(running){
            DatagramPacket receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String message = new String(receivePacket.getData(), 0 , receivePacket.getLength());
            handle(message);
        } 
        clientSocket.leaveGroup(group);
        clientSocket.close();

    }

    public void handle(String message){
        String[] arguments = message.split(":");


        switch(arguments[0]){
            case "cluster_join":
                if (!membership.getNodeId().equals(arguments[1])){
                    membership.sendTCPJoin(arguments[1], arguments[2], arguments[3], arguments[4]);

                }
                
                break;
            case "cluster_leave":
                if (!membership.getNodeId().equals(arguments[1])){
                    membership.nodeLeft(arguments[1], arguments[2], arguments[3]);
                }
                break;
            case "leader":
                if (!membership.getNodeId().equals(arguments[1])){
                    membership.setLeader(arguments[2]);                    
                }
                break;
            case "leader_info":
                membership.saveLeaderInfo(arguments[1], arguments[2], arguments[3]);
                break;
        }
    }

    public void stop(){
        running = false;
    }

    
}
