import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;



class sendUdpJoin implements Runnable{
    private Thread receive_msg;
    private UdpServer udpServer;
    private String msg;
    private Integer tcp_counter;
    private TcpServer tcpServer;

    public sendUdpJoin(Thread receive_msg, TcpServer tcpServer, UdpServer udpServer, String msg){
        this.receive_msg = receive_msg;
        this.udpServer = udpServer;
        this.msg = msg;

    }

    @Override
    public void run() {
        tcp_counter = 0;
        do{
            try{
                udpServer.sendMessage(msg);
                System.out.println("Sent UDP Join message " + tcp_counter);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tcp_counter++;

        }while (receive_msg.isAlive() && tcp_counter < 3);


    
        if (receive_msg.isAlive()){
            try{
                tcpServer.stop();
            }catch( NullPointerException e){
                System.out.println("Tcp already closed");
            }
            
        }
        
    }
    
}



public class Membership{
    public TreeMap<String, String> nodes;//hash code ip do node guardar a port
    private String node_ip;
    private Integer store_port;
    private Integer new_port;
    private Integer counter ;
    private File membership_log;
    private String IP_mcast_addr;
    private Integer IP_mcast_port;
    private UdpServer udpServer;
    private UdpClient udpClient;
    private TcpServer tcpServer;
    private String leader;
    private Leadership leadership;
    private TreeMap<String, Integer> counters_updated;


    public Membership(String IP_mcast_addr,Integer IP_mcast_port, String node_ip, Integer store_port){
        this.nodes = new TreeMap<>();
        this.node_ip = node_ip;
        this.store_port = store_port;
        this.IP_mcast_addr = IP_mcast_addr;
        this.IP_mcast_port = IP_mcast_port;
        this.counter = -1;
        this.counters_updated = new TreeMap<>();
        this.leader = "not initialized";
    }

    public String getLeader(){
        return leader;
    }



    public void join(){

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                leave();
            }
        });

        if ( (counter%2) == 0){
            System.out.println("Node already in cluster");
            return;
        }

        counter ++;


        new_port = store_port + 1;
        udpServer = new UdpServer(IP_mcast_addr, IP_mcast_port);

        tcpServer = new TcpServer(node_ip, new_port, this, true);


        String msg = new String("cluster_join:" + node_ip + ":" + store_port + ":" + new_port + ":" + counter);


        createMembershipLog(store_port);

        
        Thread receive_msg = new Thread(tcpServer);
        receive_msg.start();

        Thread resend_msg = new Thread(new sendUdpJoin(receive_msg, tcpServer, udpServer, msg));

        resend_msg.start();




        openUDPJoin();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (leader.equals("not initialized")){
            String key = nodes.firstKey();
            String id = nodes.get(key);

            String[] node_port = id.split(";");
            setLeader(node_port[0]);
        }


    }


    public void leave(){

        if (counter%2!= 0) return;
        counter ++;
        String msg = new String("cluster_leave:" + node_ip + ":" +  store_port + ":" + counter);
        System.out.println("Sent UDP message " + node_ip + " counter : " + counter);
        

        update_memb_view(node_ip,  Integer.toString(counter), Integer.toString(store_port));

        try{
            udpServer.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (leader.equals(node_ip)){
            leadership.stop();
            if (!nodes.isEmpty()){
                String key = nodes.firstKey();
                String id = nodes.get(key);

                String[] node_port = id.split(";");
                changeLeader(node_port[0]);

            }
            
        }
        
        udpClient.stop();


        Utils.redirectFiles(node_ip + ";" + store_port, nodes);
    }

    public void openUDPJoin(){

        udpClient = new UdpClient(IP_mcast_addr, IP_mcast_port, this);
        new Thread(udpClient).start();

    }



    public void createMembershipLog( Integer new_port){
        try {
            membership_log = new File("Membership_logs/" + node_ip + ".txt");
            if (membership_log.createNewFile()){
                System.out.println("New file created");
                update_memb_view(node_ip, Integer.toString(counter), Integer.toString(new_port));//sinto que isto devia ser no join
            }
            else{
                FileWriter fw = new FileWriter("Membership_logs/" + node_ip + ".txt", false);
                fw.write("");
                fw.close();
                update_memb_view(node_ip, Integer.toString(counter), Integer.toString(new_port));//sinto que isto devia ser no join

            }
          } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
          }
    }

    void handleJoinInfo(String active_members, String ml_received){
        String[] node_ip_list = active_members.split("/");
        String[] ml_lines = ml_received.split("/");


        for (String active_info : node_ip_list){

            addValueMemebershipActive(active_info);
        }

        for (String line : ml_lines){
            try {
                updateMembershipLog(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void updateMembershipLog(String mlog_received) throws IOException{
        String[] mlog_received_splited = mlog_received.split("-");
        Boolean found = false;

        Scanner reader;
        try {
            reader =new Scanner(membership_log);

            while(reader.hasNextLine()) {
                String line = reader.nextLine();
                
                String[] line_splited = line.split(" ", 2);

                if ( line_splited[0].equals(mlog_received_splited[0]) ){
                    found = true;
                    Integer self_counter =  Integer.parseInt(line_splited[1]);
                    Integer other_counter =  Integer.parseInt(mlog_received_splited[1].trim());
                    

                    if (self_counter < other_counter){
                        String[] ip_port = mlog_received_splited[0].split(";");
                        update_memb_view( ip_port[0], mlog_received_splited[1].trim() ,ip_port[1]);

                    }
                    break;
                }
            }

            if (!found){
                String[] ip_port = mlog_received_splited[0].split(";");
                update_memb_view(ip_port[0], mlog_received_splited[1], ip_port[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    void update_memb_view(String node_ip_r, String counter_r, String port_r){

        Integer counter_int  = Integer.parseInt(counter_r);
        String id = node_ip_r + ";" + port_r;

        counters_updated.put(id, counter_int);

        if ((counter_int % 2) == 0){
            addValueMemebershipActive(id);

        }
        else{
            nodes.remove(Utils.generateKey(id));
            System.out.println("Successfully removed " + id + " from active members of cluster size : " + nodes.size());

        }

        try(

            FileWriter fw = new FileWriter("Membership_logs/" + node_ip + ".txt", false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(fw)
            )
        {
            out.print("");

            for (Entry<String, Integer> entry : counters_updated.entrySet()){
                out.println(entry.getKey() + " " + entry.getValue());
            }

            

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    boolean addValueMemebershipActive(String id ){

        String hashCode = Utils.generateKey(id);

        if (!nodes.containsKey(hashCode)){

            nodes.put(hashCode, id);
            return true;
        }
        return false;
    }

    void sendTCPJoin(String node_ip_r, String node_port, String join_port, String counter_r){
        
        try {
            updateMembershipLog(node_ip_r + ";" + node_port + "-" + counter_r.trim());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        TcpClient tcpClient = new TcpClient(node_ip_r, Integer.parseInt(join_port));
        tcpClient.createSocket();

        String message = "cluster_join_info:";

        String msg = getMembershipInfo(message);


        tcpClient.writeToSocket(msg);

        String leader_msg = "current_leader:" + leader;

        tcpClient.writeToSocket(leader_msg);

        Utils.contentForNewNode(node_ip + ";" + store_port, nodes);
    }

    public String getMembershipInfo(String message){

        message += node_ip + ":";

        for (Entry<String, String>entry : nodes.entrySet()){
            message += entry.getValue() + "/";
        }

        message += ":";

        int i = 0; 

        try {
            Scanner reader = new Scanner (membership_log);

            while(i<32 && reader.hasNextLine()) {
                String line = reader.nextLine();
                
                String[] nodeid_counter = line.split(" ");

                message += nodeid_counter[0] + "-" + nodeid_counter[1] + "/";
                i++;
                
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return message;

    }

    public void nodeLeft(String node_ip_r, String port,  String counter_r){

        try {
            updateMembershipLog(node_ip_r + ";" + port + "-" + counter_r);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void setLeader(String new_leader){

        
        System.out.println("New leader assigned " + new_leader);
        if (new_leader.equals(node_ip)){
            String message = "leader:" + node_ip + ":" + new_leader;

            try {
                System.out.println("Sending leader message. The leader is " + new_leader);
                udpServer.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            leader = new_leader;

            leadership = new Leadership(udpServer, this);
            Thread send_leader_msg_ = new Thread(leadership);
            send_leader_msg_.start();

        }

        leader = new_leader;

    }

    void changeLeader(String new_leader){
        String message = "leader:" + node_ip + ":" + new_leader;

        try {
            System.out.println("Sending leader message. The leader is " + new_leader);
            udpServer.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        leader = new_leader;

        
        

    }

    String getLeaderMessage(){
        String msg = "leader_info:"; 

        String finalmsg = getMembershipInfo(msg);


        return finalmsg;
    }

    public void saveLeaderInfo(String leader, String active_nodes, String mem_log){
        this.leader = leader;
        if(!leader.equals(node_ip)){
            System.out.println("Received leader message " + active_nodes + "  " + mem_log);
            handleJoinInfo(active_nodes, mem_log);
        }

    }

    public String getNodeId(){
        return this.node_ip;
    }

    public Integer getNodeCounter(){
        return this.counter;
    }
    
}
