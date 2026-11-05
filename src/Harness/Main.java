package Harness;

/**
 * Entry point of Door subsystem:
 *   Configures SoftwareBus connection
 *   Creates/Starts the Manager
 */
public final class Main {

    public static void main(String[] args) {
        // SoftwareBus Router connection settings and elevator number
        String routerIp = "127.0.0.1";
        int routerPort = 5001;
        int elevator = 1;

        //default door speed in ms
        double doorSpeed = 3000.0;

        // read command-line arguments to override default settings
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--router-ip" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing SoftwareBus.SoftwareBus.Router IP.");
                        return;
                    }
                    routerIp = args[++i];
                }
                case "--router-port" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing SoftwareBus.SoftwareBus.Router port.");
                        return;
                    }
                    routerPort = Integer.parseInt(args[++i]);
                }
                case "--elevator" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing elevator number.");
                        return;
                    }
                    elevator = Integer.parseInt(args[++i]);
                }
                case "--door-speed" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Main: Missing door speed.");
                        return;
                    }
                    doorSpeed = Double.parseDouble(args[++i]);
                }
                default -> {
                    System.err.println("Main: Unknown argument " + args[i]);
                    return;
                }
            }
        }
        if (routerPort < 1 || routerPort > 65535) {
            System.err.println("Main: Invalid SoftwareBus.SoftwareBus.Router port.");
            return;
        }
        if (elevator < 1 || elevator > 4) {
            System.err.println("Main: Elevator must be 1-4.");
            return;
        }
        if (!Double.isFinite(doorSpeed) || doorSpeed <= 0) {
            System.err.println("Main: Door speed must be a positive number.");
            return;
        }
        // Main only starts the subsystem Manager
        Manager manager = new Manager(routerIp, routerPort, elevator, doorSpeed);
        manager.start();
    }
}