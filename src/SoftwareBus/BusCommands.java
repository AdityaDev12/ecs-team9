package SoftwareBus;

import java.util.LinkedList;
import java.util.Scanner;

// NOT AN ORIGINAL FILE
// for testing software bus with routers

public class BusCommands {

    static void main() throws InterruptedException {

        Scanner scanner = new Scanner(System.in);
        Router router;
        LinkedList<SoftwareBus> buses = new LinkedList<>();
        LinkedList<TestClient> testClients = new LinkedList<>();
        while (true) {
            String line = scanner.nextLine();
            String[] split = line.split(" ");
            try {
                switch (split[0]) {
                    // start <port #>
                    case "start" -> {
                        router = new Router(Integer.parseInt(split[1]));
                    }
                    // makeBus <IP> <port #>
                    // use localhost for IP if done locally
                    case "makeBus" -> {
                        buses.add(new SoftwareBus(split[1], Integer.parseInt(split[2])));
                        System.out.println("Made bus " + (buses.size() - 1));
                    }
                    // register <bus #>
                    case "register" -> {
                        testClients.add(new TestClient(buses.get(Integer.parseInt(split[1])).register()));
                        System.out.println("Made client " + (testClients.size() - 1) + " on bus " + split[1]);
                    }
                    // subscribe <bus #> <client #> <topic> <subtopic>
                    case "subscribe" -> {
                        buses.get(Integer.parseInt(split[1])).subscribe(testClients.get(Integer.parseInt(split[2])).getUuid(), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
                        System.out.println("Subscribed on bus " + split[1] + " with client " + split[2] + " for topic " + split[3] + " and subtopic " + split[4]);
                    }
                    // publish <bus #> <client #> <topic> <subtopic> <body>
                    case "publish" -> {
                        buses.get(Integer.parseInt(split[1])).publish(testClients.get(Integer.parseInt(split[2])).getUuid(),
                                new Message(Integer.parseInt(split[3]), Integer.parseInt(split[4]), Integer.parseInt(split[5])));
                        System.out.println("published on bus " + split[1] + " from client " + split[2] + " for topic " +
                                split[3] + " and subtopic " + split[4] + " body " + split[5]);
                    }
                    // getMsg <bus #> <client #> <topic> <subtopic>
                    case "getMsg" -> {
                        System.out.println("Got SoftwareBus.SoftwareBus.Message " + buses.get(Integer.parseInt(split[1])).getMsg(testClients.get(Integer.parseInt(split[2])).getUuid(),
                                Integer.parseInt(split[3]), Integer.parseInt(split[4])));

                    }
                    default -> {
                        System.out.println("Invalid Command. Options Below:\n" +
                                " start <port #> \n" +
                                " makeBus <IP> <port #>\n" +
                                " register <bus #>\n" +
                                " subscribe <bus #> <client #> <topic> <subtopic>\n" +
                                " publish <bus #> <client #> <topic> <subtopic> <body>\n" +
                                " getMsg <bus #> <client> <topic> <subtopic>\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        System.out.println(msg);
    }
}
