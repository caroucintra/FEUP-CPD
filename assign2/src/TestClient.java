public class TestClient {
    //recebe node_id e operator(join, leave, put, get, delete)


    public static void main(String[] args) {
        //if x establece ligaçao tcp com node id e diz ao no a funçao
        // nodeAp <IP address>:<port number>,
        String[] nodeAp = args[0].split(":");
        String operator = args[1];
        String message;

        TcpClient tcpClient = new TcpClient(nodeAp[0], Integer.parseInt(nodeAp[1]));
        tcpClient.createSocket();

        if(operator.equals("put")){
            String pathname = args[2];

            String value = Utils.getFileContent(pathname);
            String key = Utils.generateKey(value);

            System.out.println("key is: " + key);

            message = operator + ":" + key + ":" + value;
        }
        
        else if(operator.equals("get") || operator.equals("delete")){
            String key = args[2];
            message = operator + ":" + key;
        }
        else{
            message = operator;
        }
        tcpClient.writeToSocket(message);

        if(operator.equals("get") || operator.equals("delete") || operator.equals("put")){
            SimpleServer simpleServer = new SimpleServer("127.9.9.111", 8080);
            simpleServer.run();
        }

        
    }
}
