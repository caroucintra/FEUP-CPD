import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.File;


public class Utils implements Runnable {
    Membership membership;
    KeyValueStore kvStore;
    String[] arguments; 
    String originalMessage;

    public Utils(String message, Membership membership, KeyValueStore kvStore){
        originalMessage = message;

        arguments = message.split(":");


        this.membership = membership;

        this.kvStore = kvStore;
    }

    @Override
    public void run() {

        //check if this is the correct node
        if(arguments[0].equals("put") || arguments[0].equals("get") || arguments[0].equals("delete")){
            if(membership.getNodeCounter() % 2 != 0){
                //out of the cluster
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                KeyValueStore.sendMessage("Node is out of the cluster, operation not permitted");
                return;
            }

            System.out.println("arguments is " + arguments[1]);

            String correctNode = getNode(arguments[1], membership.nodes, membership.getNodeId());

            //this is not the correct node, we need to send the message to the right one
            if(correctNode != null){
                String[] node = correctNode.split(";");


                TcpClient tcpClient = new TcpClient(node[0], Integer.parseInt(node[1]));
                tcpClient.createSocket();
                tcpClient.writeToSocket(originalMessage);
                return;
            }

        }
        switch(arguments[0]){
            case("join"):
                membership.join();  
                break;

            case("leave"):
                membership.leave();
                break;

            case("put"):
                kvStore.put(arguments[1], arguments[2]);
                break;

            case("get"):
                kvStore.get(arguments[1]);
                break;

            case("delete"):
                kvStore.delete(arguments[1]);
                break;

            case("cluster_join_info"): // receives mensagem 
                System.out.println( "Received cluster join info " + arguments[2] + " " + arguments[3]);
                if (!membership.getNodeId().equals(arguments[1])){
                    membership.handleJoinInfo(arguments[2], arguments[3]);
                }
            
            case ("current_leader"):
                membership.setLeader(arguments[1]);
                break;
                

        }
        
    }


    public static String getFileContent(String path){
        Path filePath = Path.of(path);
        String content;
        try {
            content = Files.readString(filePath);
            return content;
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static String generateKey(String originalString){
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] encodedhash = digest.digest(
        originalString.getBytes(StandardCharsets.UTF_8));


        return bytesToHex(encodedhash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getNode(String key, TreeMap<String, String> nodeMap, String originalNode) {    

        if(nodeMap.isEmpty()){
            return null;
        }

        String hashCode = generateKey(key);

        String target = hashCode;

        //Processing when not included
        if (!nodeMap.containsKey(hashCode)) {
            target = nodeMap.ceilingKey(hashCode);
            if (target == null && !nodeMap.isEmpty()) {
                target = nodeMap.firstKey();
            }
        }

        String value = nodeMap.get(target);

        System.out.println("size is " + nodeMap.size());

        System.out.println("Value is " + value);
        

        String[] node = value.split(";");


        
        if(node[0].equals(originalNode)){
            return null;
        }
        else 
            return node[0] + ";" + node[1];
    }

    public static void redirectFiles(String node_id, TreeMap<String, String> nodeMap){
        String[] arguments = node_id.split(";");

        final File folder = Paths.get("../" + arguments[0]).toFile();
        if (!folder.exists()) return;
        List<String> fileNames = listFilesForFolder(folder);

        
        for (String file : fileNames){

            String fileContent = getFileContent(folder.getParent() + "/" + arguments[0] + "/" + file);

            String[] arguments3 = file.split("\\.");

            String fileName = arguments3[0];

            String node = getNode(fileName, nodeMap, "");
            String[] arguments2 = node.split(";");

            TcpClient tcpClient = new TcpClient(arguments2[0], Integer.parseInt(arguments2[1]));
            tcpClient.createSocket();

            //op:key:content

            String message = "put:" + fileName + ":" + fileContent;
            tcpClient.writeToSocket(message);

            SimpleServer simpleServer = new SimpleServer("127.9.9.111", 8080);
            simpleServer.run();
            simpleServer.close();
            simpleServer = null;
        }
        Path p1 = Paths.get("../" + arguments[0]);
        try{
            deleteDirectoryStream(p1);
        } catch(IOException i){
            System.out.println("Exception in deleteDirectoryStream\n");
        }
        
    }

    public static List<String> listFilesForFolder(File folder) {
        List<String> files = new ArrayList<String>();
        if (folder.listFiles().length == 0) return files;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                files.add(fileEntry.getName());
            }
        }
        return files;
    }

   public static void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }


    public static void contentForNewNode(String node_id, TreeMap<String, String> nodeMap){

        String[] arguments = node_id.split(";");

        final File folder = Paths.get("../" + arguments[0]).toFile();
        if(!folder.exists()) return;

        List<String> fileNames = listFilesForFolder(folder);


        for (String file : fileNames) {

            String fileContent = getFileContent(folder.getParent() + "/" + arguments[0] + "/" + file);

            String[] arguments3 = file.split("\\.");
            String fileName = arguments3[0];

            String node = getNode(fileName, nodeMap, "");

            if (node != node_id){
                String[] arguments2 = node.split(";");

                TcpClient tcpClient = new TcpClient(arguments[0], Integer.parseInt(arguments[1]));
                tcpClient.createSocket();

                //delete file
                File thisFile = new File( "../" + arguments[0] + "/" + file);

                if (thisFile.delete()) {
                    System.out.println("Successfully deleted the file.");
                }
                else {
                    System.out.println("An error occurred.");
                }

                //send put
                TcpClient tcpClient2 = new TcpClient(arguments2[0], Integer.parseInt(arguments2[1]));
                tcpClient2.createSocket();

                String message2 = "put:" + fileName + ":" + fileContent;
                tcpClient2.writeToSocket(message2);

                SimpleServer simpleServer = new SimpleServer("127.9.9.111", 8080);
                simpleServer.run();
                simpleServer.close();
                simpleServer = null;
            }
        }

    }
}