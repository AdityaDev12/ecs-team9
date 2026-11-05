package SoftwareBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router {
    private BusListener busListener;
    private static List<BusHandler> busHandlers = new CopyOnWriteArrayList<BusHandler>();
    private static List<UUID> takenIDs = new CopyOnWriteArrayList<UUID>();



    public Router(int port){
        busListener = new BusListener(port);
        new Thread(busListener).start();
    }

    /**
     * Class to handle a thread to listen for connecting buses.
     */
    private static class BusListener implements Runnable {
        int port;
        public BusListener(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try(ServerSocket router = new ServerSocket(port)) {
                System.out.println("SoftwareBus.SoftwareBus.Router listening on port " + port);
                while (true){
                    Socket socket = router.accept();
                    BusHandler busHandler = new BusHandler(socket);
                    System.out.println("Bus Connected");
                    busHandlers.add(busHandler);
                    new Thread(busHandler).start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Class to handle the communication to a single connected bus.
     */
    private static class BusHandler implements Runnable {

        private final Socket socket;
        private PrintWriter out;


        public BusHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try(socket;
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                this.out = out;
                RouterMsg firstMsg = RouterMsg.parse(in.readLine());
                if (!(firstMsg instanceof RouterMsg.Connected())) {
                    return;
                }
                System.out.println("Bus Has Confirmed Connected");
                out.println(new RouterMsg.Connected().encode());

                String line;
                RouterMsg msg;
                while((line = in.readLine()) != null){
                    msg = RouterMsg.parse(line);
                    switch (msg){
                        case RouterMsg.Valid_UUID(var id) -> {
                            if(!takenIDs.contains(id)){
                                takenIDs.add(id);
                                out.println(new RouterMsg.Valid_Resp_UUID(id, true).encode());
                            } else {
                                out.println(new RouterMsg.Valid_Resp_UUID(id, false).encode());
                            }
                        }
                        case RouterMsg.Publish(var id, var message) -> {
                            if(takenIDs.contains(id)){
                                for (BusHandler busHandler : busHandlers) {
                                    if(!busHandler.equals(this)){
                                        busHandler.sendMessage(message);
                                    }
                                }
                                out.println(new RouterMsg.Valid_Publish(id, true).encode());
                            } else {
                                out.println(new RouterMsg.Valid_Publish(id, false).encode());
                            }
                        }
                        default ->  throw new IllegalStateException("Unexpected SoftwareBus.SoftwareBus.RouterMsg: " + msg);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Sends a message to this bus
         * @param message message to be sent
         */
        public void sendMessage(Message message){
            out.println(new RouterMsg.SendMessage(message).encode());
        }
    }
}
