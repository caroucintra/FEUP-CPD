

public class Leadership  implements Runnable{

    private UdpServer udpServer;

    private Membership membership;

    private Boolean running;



    Leadership(UdpServer udpServer, Membership membership){
        this.udpServer = udpServer;
        this.membership = membership;
        this.running = true;
    }

    @Override
    public void run() {

        while(running){
            String msg = new String( membership.getLeaderMessage());
            try {
                udpServer.sendMessage(msg);
                System.out.println("Sent Leader message  "+ msg );
                Thread.sleep(1000);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        
        
    }

    public synchronized void stop(){
        running = false;
    }
}
