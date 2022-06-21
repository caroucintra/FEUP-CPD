
public class Store {

    public static void main(String[] args)  {

        String IP_mcast_addr = args[0];
        Integer IP_mcast_port = Integer.parseInt(args[1]);

        String node_id = args[2];
        Integer store_port = Integer.parseInt(args[3]);
        Membership membership = new Membership(IP_mcast_addr, IP_mcast_port, node_id, store_port);


        TcpServer tcpServer = new TcpServer(node_id, store_port, membership, false);
        Thread threadTcp = new Thread(tcpServer);
        threadTcp.start();

        

    }
}
