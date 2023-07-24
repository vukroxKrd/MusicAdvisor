package advisor;

import server.HTTPServer;

import java.io.IOException;
import java.util.Scanner;

import static advisor.InputSemaphore.continueReceivingInput;

public class Main {
    static Scanner scanner = new Scanner(System.in);

    public static String requestOperation() {
        return scanner.nextLine();
    }

    public static void main(String[] args) {

        SpotifyUser user = new SpotifyUser("616b49ea385644bbaa28ea3337e027c7", "784f6d729fb049eda00c01da538c6e3d", false);
        InputSemaphore semaphore = new InputSemaphore();

        try {
            HTTPServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        do {
            semaphore.selectOperation(requestOperation(), user);
        } while (continueReceivingInput);

        HTTPServer.stop(5);
    }
}
