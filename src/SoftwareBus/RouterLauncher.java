package SoftwareBus;

public class RouterLauncher {
    public static void main(String[] args) {
        int port = 5001;
        System.out.println("Starting Software Bus Router...");
        new Router(port);
    }
}
