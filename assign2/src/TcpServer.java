import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TcpServer implements Runnable {
    String       nodeId;
    InetAddress  hostIp;
    int          serverPort   = 8080;
    ServerSocket serverSocket = null;
    boolean      isStopped    = false;
    Thread       runningThread= null;
    ExecutorService threadPool = Executors.newFixedThreadPool(10);
    Membership membership;
    private Integer counter = 0;
    private Boolean join_nodes;

    public TcpServer(String node_id, int store_port, Membership membership, Boolean join_nodes ){
        this.nodeId = node_id;
        this.serverPort = store_port;
        this.membership = membership;
        this.join_nodes = join_nodes;
        try {
            hostIp = InetAddress.getByName(node_id);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }



    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();

        while(! isStopped()){
            if ( join_nodes && ( counter >=3 )){
                System.out.println("Received 3 join_cluster_info.");
                stop();
            }
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            counter++;
            this.threadPool.execute(
                new WorkerRunnable(clientSocket, "Thread Pooled Server", nodeId, serverPort, membership));
        }

        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }


    public synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort, 50, hostIp);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }
    

}
