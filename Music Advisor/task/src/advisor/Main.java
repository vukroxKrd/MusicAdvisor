package advisor;

import java.util.Random;
import java.util.Scanner;

import static advisor.InputSemaphore.continueReceivingInput;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    public static String domainName = "https://accounts.spotify.com";
    public static String spotifyApiV1 = "https://api.spotify.com";
    public static Integer itemsPerPage = 5;
    public static String requestOperation() {
        return scanner.nextLine();
    }
    public static String port;
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("-access")) {
            domainName = args[1];
            spotifyApiV1 = args[3];
            itemsPerPage = Integer.valueOf(args[5]);
        }


        SpotifyUser user = new SpotifyUser("616b49ea385644bbaa28ea3337e027c7", "784f6d729fb049eda00c01da538c6e3d", false);
        InputSemaphore semaphore = new InputSemaphore();

        do {
            semaphore.selectOperation(requestOperation(), user);
        } while (continueReceivingInput);

    }

    public static int generateRandomPortNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }
}
