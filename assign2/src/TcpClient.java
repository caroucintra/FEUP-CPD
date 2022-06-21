import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient {
    private String hostname;
    private int port;
    private Socket socket;
    private OutputStream output;
    private PrintWriter writer;
    private InputStream input;
    private BufferedReader reader;

    public TcpClient(String ip, int port) {
 
        this.hostname = ip;
        this.port = port;
    }

    public void createSocket(){
        
        try {

            socket = new Socket(hostname, port);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
 
    }

    public void writeToSocket(String message){
        try {
            output = this.socket.getOutputStream();

            writer = new PrintWriter(output, true);

            writer.println(message.toString());
        } catch (IOException e) {
            System.out.println("Tcp socket already closed. Could not send message.");
        }
        catch( NullPointerException a){
            System.out.println("Tcp socket already closed. Could not send message.");
        }
        

    }

    public String readFromSocket(){
        
        try {
            input = this.socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
    
}
