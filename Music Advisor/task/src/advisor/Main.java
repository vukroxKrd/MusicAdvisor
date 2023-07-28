package advisor;

import server.HTTPServer;

import java.io.IOException;
import java.util.Scanner;

import static advisor.InputSemaphore.continueReceivingInput;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    public final static String domainName = "https://accounts.spotify.com";
    public static String requestOperation() {
        return scanner.nextLine();
    }

    public static void main(String[] args) {
//        for (String arg : args) {
//            System.out.println(arg);
//        }
        String endpointUrl = "";
        if (args.length > 0 && args[0].equals("-access")) {
            endpointUrl = args[1];
        } else {
            endpointUrl = domainName;
        }
        SpotifyUser user = new SpotifyUser("616b49ea385644bbaa28ea3337e027c7", "784f6d729fb049eda00c01da538c6e3d", false);
        user.setEndpointUrl(endpointUrl);
        InputSemaphore semaphore = new InputSemaphore();

        do {
            semaphore.selectOperation(requestOperation(), user);
        } while (continueReceivingInput);
    }
}
