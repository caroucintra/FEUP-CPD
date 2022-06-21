import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class KeyValueStore{
    private String nodeId;

    public KeyValueStore(String nodeId){
        this.nodeId = nodeId;

        //create directory to store files
        new File("../" + nodeId).mkdirs();
    }

    //saves the file
    public void put(String key, String value){
        try {
            FileWriter myWriter = new FileWriter("../" + nodeId + "/" + key + ".txt");
            myWriter.write(value);
            myWriter.close();
            System.out.println("Successfully created the file.");
            sendMessage("Successfully created the file.");
          } catch (IOException e) {
            System.out.println("An error occurred.");
            sendMessage("An error occurred.");
            e.printStackTrace();
          }
    }


    public void get(String key){
        String path = "../" + this.nodeId + "/" + key + ".txt";
        String content = Utils.getFileContent(path);
        if(content != null)
          sendMessage(content);
        else
          sendMessage("File not found");
    }


    public void delete(String key){
        File file = new File( "../" + this.nodeId + "/" + key + ".txt");

        if (file.delete()) {
            sendMessage("Successfully deleted the file.");
        }
        else {
            sendMessage("An error occurred.");
        }
    }

    public static void sendMessage(String message){
        TcpClient tcpClient = new TcpClient("127.9.9.111", 8080);
        tcpClient.createSocket();
        tcpClient.writeToSocket(message);
    }
    
}
