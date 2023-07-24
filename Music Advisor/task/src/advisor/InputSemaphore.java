package advisor;

import server.HTTPServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class InputSemaphore {
    public static final List<String> supportedOps = List.of("new", "featured", "categories", "playlists Mood", "auth", "exit");
    public static boolean continueReceivingInput = true;

    public void selectOperation(String input, SpotifyUser user) {

        String playlistCategory = "";
        if (input.contains("playlists")) {
            playlistCategory = input.replaceAll("playlists ", "");
            input = "playlists";
        }


        if (input.equals("exit")) {
            sayGoodBye();
        } else if (!user.isAuthenticated() && !input.equals("auth")) {
            promptUserToAuthenticate();
        } else {
            switch (input) {
                case "new" -> newReleases();
                case "featured" -> featured();
                case "categories" -> categories();
                case "playlists" -> playlists(playlistCategory);
                case "auth" -> authenticate(user);
                default -> returnSupportedOpList(supportedOps);
            }
        }
    }

    private void promptUserToAuthenticate() {
        System.out.println("Please, provide access for application.");
    }

    private void authenticate(SpotifyUser user) {

        HTTPServer.listen(user);

        makeInitiallCallForAuthCode(user);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("making http request for access_token...");
        SpotifyUser spotifyUser = obtainTokens(user);

        if (spotifyUser.getAccess_token() != null) {
            user.setAuthenticated(true);
            System.out.println("---SUCCESS---");
        }
    }

    private static void makeInitiallCallForAuthCode(SpotifyUser user) {
        System.out.println("use this link to request the access code: ");
        System.out.println("https://accounts.spotify.com/authorize?client_id=0db1be20b0494973a17516bae8af091d&redirect_uri=http://localhost:8181/callback&response_type=code");
        System.out.println("waiting for code...");
        try {
            HTTPServer.HTTPClient.sendInitialAuthRequestToRemoteResource(user);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private SpotifyUser obtainTokens(SpotifyUser user) {
        return HTTPServer.reqeustTokensFromRemoteResource(user);
    }

    private void newReleases() {
        System.out.println("---NEW RELEASES---");
        Map<String, List<String>> releases = new LinkedHashMap<>();
        releases.put("Mountains", List.of("Sia", "Diplo", "Labrinth"));
        releases.put("Runaway", List.of("Lil Peep"));
        releases.put("The Greatest Show", List.of("Panic! At The Disco"));
        releases.put("All Out Life", List.of("Slipknot"));
        releases.forEach((release, group) -> System.out.println(release + " " + group));
    }

    private void featured() {
        System.out.println("---FEATURED---");
        List<String> featured = new ArrayList<>();
        featured.add("Mellow Morning");
        featured.add("Wake Up and Smell the Coffee");
        featured.add("Monday Motivation");
        featured.add("Songs to Sing in the Shower");
        featured.forEach(System.out::println);
    }

    private void categories() {
        System.out.println("---CATEGORIES---");
        List<String> categories = new ArrayList<>();
        categories.add("Top Lists");
        categories.add("Pop");
        categories.add("Mood");
        categories.add("Latin");
        categories.forEach(System.out::println);
    }

    private void playlists(String category) {
        System.out.println("---" + category.toUpperCase() + " PLAYLISTS---");
        List<String> moodPlaylists = new ArrayList<>();
        moodPlaylists.add("Walk Like A Badass");
        moodPlaylists.add("Rage Beats");
        moodPlaylists.add("Arab Mood Booster");
        moodPlaylists.add("Sunday Stroll");
        moodPlaylists.forEach(System.out::println);
    }

    private void sayGoodBye() {
        System.out.println("---GOODBYE!---");
        continueReceivingInput = false;
    }

    private void returnSupportedOpList(List<String> operations) {
        continueReceivingInput = false;
        System.out.println("The list of supported operations: ");
        operations.forEach(System.out::println);
    }
}
