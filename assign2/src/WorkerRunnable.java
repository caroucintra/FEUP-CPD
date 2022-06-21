import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;


public class WorkerRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;

    Membership membership;
    KeyValueStore kvStore;

    public WorkerRunnable(Socket clientSocket, String serverText, String nodeId, int port, Membership membership) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
        this.membership = membership;

        kvStore = new KeyValueStore(nodeId);
    }

    public void run() {
        try {
            InputStream input  = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String parsedInput = reader.readLine();

            
            OutputStream output = clientSocket.getOutputStream();
                        

            Utils callFunctions = new Utils(parsedInput, membership, kvStore);
            //callFunctions.run();

            new Thread(callFunctions).start();
            
            
            input.close();
            output.close();
            
            long time = System.currentTimeMillis();
            System.out.println("Request processed: " + time);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
