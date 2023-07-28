package advisor;

import server.HTTPServer;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;

import static advisor.Main.domainName;
import static java.net.http.HttpRequest.newBuilder;

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

        try {
            HTTPServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HTTPServer.listen(user);

        String storedState = HTTPServer.generateRandomState(10);
        user.setStoredState(storedState);

        HttpClient client = HttpClient.newBuilder()
                //https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a
//                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        /* Формируем url */
        String responseType = "code";
        String redirectUri = "http://localhost:8181";
//        String scope = "user-read-private%20user-read-email";

        String url = String.format("%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&state=%s", Main.domainName, user.getClientId(), responseType, redirectUri, storedState);

        /* Формируем HTTP запрос */
        HttpRequest request = newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> initialGetRequest;
        try {
            initialGetRequest = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("making http request for access_token...");
        SpotifyUser user1 = HTTPServer.reqeustTokensFromRemoteResource(user);
        System.out.println("---SUCCESS---");

    }

    private static void makeInitiallCallForAuthCode(SpotifyUser user) {

        try {
            HTTPServer.HTTPClient.sendInitialAuthRequestToRemoteResource(user);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
