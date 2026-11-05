package SoftwareBus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoftwareBus {
    // Class for the software bus for project 2

    private boolean connectedToRouter = false;
    private final Map<UUID, List<Message>> idAndMsg =  new ConcurrentHashMap<>();
    private final Map<Identifier, Set<UUID>> topicAndSubscribers = new ConcurrentHashMap<>();
    private RouterHandler routerHandler;
    private volatile boolean isSetUp = false;

    /**
     * Creates a stand alone SoftwareBus.SoftwareBus not connected to any routers
     */
    public SoftwareBus(){}

    /**
     * Creates a SoftwareBus.SoftwareBus that is connected to a router. This bus will communicate with other
     * buses over this connected router.
     */
    public SoftwareBus(String routerIp, int routerPort){
        connectedToRouter = true;
        routerHandler = new RouterHandler(routerIp, routerPort);
        new Thread(routerHandler).start();
    }

    /**
     * Register as a subsystem connected to this software bus. Returns your corresponding id.
     * @return int id
     */
    public UUID register(){
        // If Connected to a router we confirm if UUID is unique between all software buses
        if(connectedToRouter){
            while (!isSetUp) {
                Thread.onSpinWait();
            }
            boolean valid_id = false;
            UUID unique_id = null;
            while(!valid_id){
                unique_id = UUID.randomUUID();
                if(isSetUp){
                    routerHandler.sendOut(new RouterMsg.Valid_UUID(unique_id));
                }
                boolean valid_msg = false;
                RouterMsg msgTrue;
                RouterMsg msgFalse;
                while(!valid_msg){
                    msgTrue =  routerHandler.getRouterMsg(new RouterMsg.Valid_Resp_UUID(unique_id, true));
                    msgFalse =  routerHandler.getRouterMsg(new RouterMsg.Valid_Resp_UUID(unique_id, false));
                    if(msgTrue != null || msgFalse != null){
                        valid_msg = true;
                    }
                    if(msgTrue != null && msgFalse == null){
                        valid_id = true;
                    }
                }
            }
            idAndMsg.put(unique_id, new CopyOnWriteArrayList<>());
            return unique_id;
        } else {
            UUID unique_id = UUID.randomUUID();
            idAndMsg.put(unique_id, new CopyOnWriteArrayList<>());
            return unique_id;
        }
    }

    /**
     * Subscribe to a given topic and subtopic. When a subsystem publishes with that given topic
     * and subtopic a message will be added to the corresponding queue of the given id.
     * @param id int - id of who is subscribing
     * @param topic String - topic you are subscribing to
     * @param subtopic int - subtopic you are subscribing to
     */
    public void subscribe(UUID id, int topic, int subtopic){
        Identifier ident = new Identifier(topic, subtopic);
        /*
        if the ident (topic and subtopic) is not already in the set, make a new set and add it to the hashmap with ident
        as the key, finally return the set mapped to ident. Else just return the set mapped to ident. Then add
        id(subscriber) to the set.
        Written this way to make it thread safe.
         */

        topicAndSubscribers.computeIfAbsent(ident, key -> ConcurrentHashMap.newKeySet()).add(id);
    }

    /**
     * Publish a given message from a given id
     * @param id int - id of publisher
     * @param message - message being sent to subscribers
     */
    public void publish(UUID id, Message message){
        // If router is connected we check if id is valid between all software buses
        if(connectedToRouter){
            RouterMsg msgTrue = null;
            RouterMsg msgFalse = null;
            routerHandler.sendOut(new RouterMsg.Publish(id, message));
            boolean valid_resp = false;
            while(!valid_resp){
                msgTrue =  routerHandler.getRouterMsg(new RouterMsg.Valid_Publish(id, true));
                msgFalse = routerHandler.getRouterMsg(new RouterMsg.Valid_Publish(id, false));
                if(msgTrue != null || msgFalse != null){
                    valid_resp = true;
                }
            }
            if(msgTrue != null && msgFalse == null){
                Identifier ident = new Identifier(message.topic(),  message.subtopic());
                if(topicAndSubscribers.containsKey(ident)){
                    Set<UUID> set = topicAndSubscribers.get(ident);
                    for(UUID i : set){
                        idAndMsg.get(i).add(message);
                    }
                }
            } else {
                System.err.println("Id: " + id + " not registered");
            }
        } else {
            if(!idAndMsg.containsKey(id)){
                System.out.println("Id: " + id + " not registered");
                return;
            }
            Identifier ident = new Identifier(message.topic(),  message.subtopic());
            if(topicAndSubscribers.containsKey(ident)){
                Set<UUID> set = topicAndSubscribers.get(ident);
                for(UUID i : set){
                    idAndMsg.get(i).add(message);
                }
            }
        }
    }

    /**
     * Gets the oldest message in queue of the given id that have the given topic and
     * subtopic. Removes and returns this message if found. Returns null otherwise.
     * @param id int - your id to get message
     * @param topic String - topic of message to get
     * @param subtopic int - subtopic of message to get
     * @return SoftwareBus.SoftwareBus.Message of found.
     */
    public Message getMsg(UUID id, int topic, int subtopic){
        List<Message> list = idAndMsg.get(id);
        for(Message s : list){
            if(s.topic() == (topic) && s.subtopic() == subtopic){
                list.remove(s);
                return s;
            }
        }
        return null;
    }

    /**
     * Class to handle the router connection and messages being sent between this bus and the router.
     */
    private class RouterHandler implements Runnable{
        private String routerIp;
        private int routerPort;
        private PrintWriter out;
        private final List<RouterMsg> msgs = new CopyOnWriteArrayList<>();

        public RouterHandler(String routerIp, int routerPort){
            this.routerIp = routerIp;
            this.routerPort = routerPort;
        }

        @Override
        public void run() {
            try(Socket socket = new Socket(routerIp, routerPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                this.out = out;

                out.println(new RouterMsg.Connected().encode());
                String line;
                RouterMsg msg;
                if((line = in.readLine()) != null){
                    if((msg = RouterMsg.parse(line)) instanceof  RouterMsg.Connected){
                        System.out.println("Connected");
                    }
                }

                isSetUp = true;
                while((line = in.readLine()) != null){
                    msg = RouterMsg.parse(line);
                    switch (msg){
                        case RouterMsg.SendMessage(var message) -> {
                            Identifier ident = new Identifier(message.topic(),  message.subtopic());
                            if(topicAndSubscribers.containsKey(ident)){
                                Set<UUID> set = topicAndSubscribers.get(ident);
                                for(UUID i : set){
                                    idAndMsg.get(i).add(message);
                                }
                            }
                        }
                        case RouterMsg.Valid_Resp_UUID(var id, var valid) -> {
                            msgs.add(msg);
                        }
                        case RouterMsg.Valid_Publish(var id, var valid) -> {
                            msgs.add(msg);
                        }
                        default ->  {
                            System.err.println("Unknown msg");
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Send a router message out to the other buses over the router
         * @param msg message to send
         */
        public void sendOut(RouterMsg msg){
            out.println(msg.encode());
        }

        /**
         * Searches and returns a router message if it was sent from the router otherwise returns null
         * @param msg message to search for
         * @return router message of null if not found
         */
        public RouterMsg getRouterMsg(RouterMsg msg){
            for (RouterMsg routerMsg : msgs) {
                if(routerMsg.equals(msg)){
                    msgs.remove(msg);
                    return routerMsg;
                }
            }
            return null;
        }
    }

    private record Identifier(int topic, int subtopic){}
}
