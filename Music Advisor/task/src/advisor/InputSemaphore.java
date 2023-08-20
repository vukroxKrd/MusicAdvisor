package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import responses.*;
import server.HTTPServer;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;

import static java.net.http.HttpRequest.newBuilder;

public class InputSemaphore {
    public static final List<String> supportedOps = List.of("new", "featured", "categories", "playlists Mood", "auth", "exit");
    public static boolean continueReceivingInput = true;
    public static Map<String, String> parameters = new HashMap<>();
    private static Map<String, String> categories;

    public InputSemaphore() {
    }

    public static Map<String, String> getCategories() {
        if (categories == null) {
            categories = new HashMap<>();
        }
        return categories;
    }

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
                case "new" -> newReleases(user, parameters);
                case "featured" -> featured(user, parameters);
                case "categories" -> categories(user, parameters, true);
                case "playlists" -> playlists(user, parameters, playlistCategory);
                case "auth" -> authenticate(user);
                default -> returnSupportedOpList(supportedOps);
            }
        }
    }

    private void promptUserToAuthenticate() {
        System.out.println("Please, provide access for application.");
    }

    private void authenticate(SpotifyUser user) {
        HTTPServer.createHttpServer(user);
        HTTPServer.authRequest(user);
    }

    private static void initialGetForCodeFromSpotify(SpotifyUser user) {
        String port = Main.port;

        String storedState = HTTPServer.generateRandomState(10);
        user.setStoredState(storedState);

        HttpClient client = HttpClient.newBuilder()
                .build();

        String responseType = "code";
//        String redirectUri = "http://localhost:8181";
        String redirectUri = String.format("http://localhost:%s", port);

        String url = String.format("%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&state=%s", Main.domainName, user.getClientId(), responseType, redirectUri, storedState);

        HttpRequest request = newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
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
    }

    private static HttpResponse<String> sendGetRequestToSpotifyApi(SpotifyUser user, String endpointName, Map<String, String> parameters) {

        HttpRequest.Builder builder = newBuilder();

        HttpClient client = HttpClient.newBuilder().build();
        String endpoint = Main.spotifyApiV1 + endpointName;

        String pathToCall;
        if (!parameters.isEmpty()) {
            String processedParams = HTTPServer.getFormDataAsString(parameters);
            pathToCall = String.format(endpoint + "?" + processedParams);
        } else {
            pathToCall = endpoint;
        }

        HttpRequest request = builder.header("Authorization", "Bearer " + user.getAccessToken())
                .uri(URI.create(pathToCall))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    private void newReleases(SpotifyUser user, Map<String, String> parameters) {
        String endpoint = "/v1/browse/new-releases";
        parameters.put("limit", Main.itemsPerPage.toString());

        HttpResponse<String> response = sendGetRequestToSpotifyApi(user, endpoint, parameters);

        List<SpotifyResponse> newReleasesResponses = new ArrayList<>();
        if (response.statusCode() == 200) {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject albums = body.getAsJsonObject("albums");
            JsonArray items = albums.getAsJsonArray("items");


            for (JsonElement item : items) {
                SpotifyResponse spotifyResponse = new SpotifyNewReleasesReponse();

                JsonObject itm = item.getAsJsonObject();
                String albumName = itm.get("name").getAsString();
                spotifyResponse.setName(albumName);

                List<String> allArtists = new ArrayList<>();

                JsonArray artists = itm.get("artists").getAsJsonArray();
                for (JsonElement oneArtist : artists) {
                    JsonObject artist = oneArtist.getAsJsonObject();
                    allArtists.add(artist.get("name").getAsString());
                }
                spotifyResponse.setArtists(allArtists);

                JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
                String spotifyUrl = externalUrls.get("spotify").getAsString();
                spotifyResponse.setUrl(spotifyUrl);

                newReleasesResponses.add(spotifyResponse);
            }
        } else {
            if (response.body() != null && !response.body().isEmpty()) {
                JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject error = errorBody.getAsJsonObject("error");
                System.out.println(error.get("message").getAsString());
            }
        }
        paginateResult(newReleasesResponses);

//        if (response.headers().firstValue(":status").isPresent()) {
//            String status = response.headers().firstValue(":status").get();
//            if (status.equals("200")) {
//                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
//                JsonObject albums = body.getAsJsonObject("albums");
//                JsonArray items = albums.getAsJsonArray("items");
//
//                for (JsonElement item : items) {
//                    JsonObject itm = item.getAsJsonObject();
//                    String albumName = itm.get("name").getAsString();
//                    System.out.println(albumName);
//
//                    List<String> allArtists = new ArrayList<>();
//                    JsonArray artists = itm.get("artists").getAsJsonArray();
//                    for (JsonElement oneArtist : artists) {
//                        JsonObject artist = oneArtist.getAsJsonObject();
//                        allArtists.add(artist.get("name").getAsString());
//                    }
//                    System.out.println(allArtists);
//
//                    JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
//                    String spotifyUrl = externalUrls.get("spotify").getAsString();
//                    System.out.println(spotifyUrl);
//                    System.out.println();
//                }
//            } else {
//                if (response.body() != null && !response.body().isEmpty()) {
//                    JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
//                    JsonObject error = errorBody.getAsJsonObject("error");
//                    System.out.println(error.get("message").getAsString());
//                }
//            }
//        } else {
//            System.out.println("Request was not successful");
//            System.out.println(response.body());
//        }
    }

    private void featured(SpotifyUser user, Map<String, String> parameters) {

        String endpoint = "/v1/browse/featured-playlists";
//        parameters.put("country", "US");
        parameters.put("limit", Main.itemsPerPage.toString());

        HttpResponse<String> response = sendGetRequestToSpotifyApi(user, endpoint, parameters);

        List<SpotifyResponse> featuredResponses = new ArrayList<>();
        if (response.statusCode() == 200) {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject playlists = body.getAsJsonObject("playlists");
            JsonArray itemsJson = playlists.getAsJsonArray("items");

            for (JsonElement item : itemsJson.getAsJsonArray()) {

                SpotifyResponse featureResponse = new SpotifyFeaturedResponse();

                JsonObject itm = item.getAsJsonObject();
                JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
                String spotifyUrl = externalUrls.get("spotify").getAsString();

                featureResponse.setName(itm.get("name").getAsString());
                featureResponse.setUrl(spotifyUrl);
                featuredResponses.add(featureResponse);

                featureResponse = null;
            }
        } else {
            if (response.body() != null && !response.body().isEmpty()) {
                JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject error = errorBody.getAsJsonObject("error");
                System.out.println(error.get("message").getAsString());
            }
        }

        paginateResult(featuredResponses);
//        if (response.headers().firstValue(":status").isPresent()) {
//            String status = response.headers().firstValue(":status").get();
//            if (status.equals("200")) {
//                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
//                JsonObject playlists = body.getAsJsonObject("playlists");
//                JsonArray itemsJson = playlists.getAsJsonArray("items");
//
//                for (JsonElement item : itemsJson.getAsJsonArray()) {
//                    JsonObject itm = item.getAsJsonObject();
//                    System.out.println(itm.get("name").getAsString());
//
//                    JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
//                    String spotifyUrl = externalUrls.get("spotify").getAsString();
//                    System.out.println(spotifyUrl);
//                    System.out.println();
//                }
//            } else {
//                if (response.body() != null && !response.body().isEmpty()) {
//                    JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
//                    JsonObject error = errorBody.getAsJsonObject("error");
//                    System.out.println(error.get("message").getAsString());
//                }
//            }
//        } else {
//            System.out.println("Request was not successful");
//            System.out.println(response.body());
//        }
    }

    private void categories(SpotifyUser user, Map<String, String> parameters, boolean calledSeparately) {
        String endpoint = "/v1/browse/categories";


//        parameters.put("offset", "0");
        parameters.put("limit", Main.itemsPerPage.toString());

//        parameters.put("country", "US");

        HttpResponse<String> response = sendGetRequestToSpotifyApi(user, endpoint, parameters);
        List<SpotifyResponse> categoryResponses = new ArrayList<>();

        if (response.statusCode() == 200) {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
//            System.out.println(body);

            JsonObject categories = body.getAsJsonObject("categories");
            JsonArray itemsJson = categories.getAsJsonArray("items");

            for (JsonElement item : itemsJson.getAsJsonArray()) {
                SpotifyResponse categoryResponse = new SpotifyCategoryResponse();
                JsonObject itm = item.getAsJsonObject();
                String categoryName = itm.get("name").getAsString();
                String catId = itm.get("id").getAsString();

                categoryResponse.setName(categoryName);
                categoryResponse.setId(catId);
                categoryResponses.add(categoryResponse);

                getCategories().put(categoryName, catId);
                categoryResponse = null;
            }
        } else {
            if (response.body() != null && !response.body().isEmpty()) {
                JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject error = errorBody.getAsJsonObject("error");
                System.out.println(error.get("message").getAsString());
            }
        }
        if (calledSeparately) {
            paginateResult(categoryResponses);
        }


//        if (response.headers().firstValue(":status").isPresent()) {
//            String status = response.headers().firstValue(":status").get();
//            if (status.equals("200")) {
//                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
//                JsonObject categories = body.getAsJsonObject("categories");
//                JsonArray itemsJson = categories.getAsJsonArray("items");
//
//                for (JsonElement item : itemsJson.getAsJsonArray()) {
//                    JsonObject itm = item.getAsJsonObject();
//                    String categoryName = itm.get("name").getAsString();
//                    String catId = itm.get("id").getAsString();
//                    getCategories().put(categoryName, catId);
//
//                    System.out.println(itm.get("name").getAsString());
//                }
//            } else {
//                if (response.body() != null && !response.body().isEmpty()) {
//                    JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
//                    JsonObject error = errorBody.getAsJsonObject("error");
//                    System.out.println(error.get("message").getAsString());
//                }
//            }
//        } else {
//            System.out.println("Request was not successful");
//            System.out.println(response.body());
//        }
    }
    private void playlists(SpotifyUser user, Map<String, String> parameters, String playlistCategory) {
        categories(user, parameters, false);
        String catId = getCategories().get(playlistCategory);

        if (catId == null) {
            System.out.println("Unknown category name.");
        } else {
            String endpoint = String.format("/v1/browse/categories/%s/playlists", catId);
            parameters.put("limit", Main.itemsPerPage.toString());

            HttpResponse<String> response = sendGetRequestToSpotifyApi(user, endpoint, parameters);

            List<SpotifyResponse> playlistResponses = new ArrayList<>();
            if (response.statusCode() == 200) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

                try {
                    JsonObject playlists = body.getAsJsonObject("playlists");
                    JsonArray itemsJson = playlists.getAsJsonArray("items");

                    for (JsonElement item : itemsJson.getAsJsonArray()) {
                        SpotifyResponse playlistItem = new SpotifyPlaylistResponse();
                        JsonObject itm = item.getAsJsonObject();

                        JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
                        String spotifyUrl = externalUrls.get("spotify").getAsString();

                        playlistItem.setName(itm.get("name").getAsString());
                        playlistItem.setUrl(spotifyUrl);

                        playlistResponses.add(playlistItem);

                        playlistItem = null;
                    }
                } catch (Exception e) {
                    if (e instanceof NullPointerException) {
                        JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonObject error = errorBody.getAsJsonObject("error");
                        System.out.println(error.get("message").getAsString());
                    }
                }
                paginateResult(playlistResponses);
            } else {
                if (response.body() != null && !response.body().isEmpty()) {
                    JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonObject error = errorBody.getAsJsonObject("error");
                    System.out.println(error.get("message").getAsString());
                }
//            if (response.headers().firstValue(":status").isPresent()) {
//                String status = response.headers().firstValue(":status").get();
//                if (status.equals("200")) {
//                    JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
//                    JsonObject categories = body.getAsJsonObject("playlists");
//                    JsonArray itemsJson = categories.getAsJsonArray("items");
//
//                    for (JsonElement item : itemsJson.getAsJsonArray()) {
//                        JsonObject itm = item.getAsJsonObject();
//                        System.out.println(itm.get("name").getAsString());
//
//                        JsonObject externalUrls = itm.get("external_urls").getAsJsonObject();
//                        String spotifyUrl = externalUrls.get("spotify").getAsString();
//                        System.out.println(spotifyUrl);
//                        System.out.println();
//                    }
//                } else {
//                    if (response.body() != null && !response.body().isEmpty()) {
//                        JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
//                        JsonObject error = errorBody.getAsJsonObject("error");
//                        System.out.println(error.get("message").getAsString());
//                    }
//                }
//            } else {
//                System.out.println("Request was not successful");
//                System.out.println(response.body());
            }
        }
    }


    private static void paginateResult(List<SpotifyResponse> responses) {
        int totalPages = (responses.size() / Main.itemsPerPage);
        int currentPage = 1;

        Integer offset = 0;

        Integer limit = Main.itemsPerPage;
        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            for (int i = offset; i < limit; i++) {
                SpotifyResponse response = responses.get(i);
                response.printResponse();
            }
            String pagination = String.format("---PAGE %s OF %s---", currentPage, totalPages);
            System.out.println(pagination);

            input = scanner.nextLine();
            if (Objects.equals(input, "next")) {
                if (currentPage >= totalPages) {
                    System.out.println("No more pages.");
                } else {
                    offset = offset + Main.itemsPerPage;
                    limit = limit + Main.itemsPerPage;
                    currentPage++;
                }

            } else {
                if (currentPage <= 1) {
                    System.out.println("No more pages.");
                } else {
                    offset = offset - Main.itemsPerPage;
                    limit = limit - Main.itemsPerPage;
                    currentPage--;
                }
            }
        } while (Objects.equals(input, "next") || Objects.equals(input, "prev"));
    }

    private void sayGoodBye() {
        System.out.println("---GOODBYE!---");
        continueReceivingInput = false;
        System.exit(0);
    }

    private void returnSupportedOpList(List<String> operations) {
        System.out.println("The list of supported operations: ");
        operations.forEach(System.out::println);
    }
}
