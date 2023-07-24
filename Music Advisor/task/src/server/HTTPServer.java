package server;

import advisor.SpotifyUser;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;

import static com.sun.net.httpserver.HttpServer.create;
import static java.net.http.HttpRequest.newBuilder;

public class HTTPServer {
    private static HttpServer server;
    public static void listen(SpotifyUser user) {
        /* Создаем сервер и указываем на каком сокете он будет работать */

        /* Указываем ассоциацию путь<->обработчик */
        server.createContext("/callback",
                new HttpHandler() {
                    public void handle(HttpExchange exchange) {

                        byte[] buffer = new byte[4096];
                        InputStream requestBody = exchange.getRequestBody();
                        try {
                            int amountRead = requestBody.read(buffer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        String query = exchange.getRequestURI().getQuery();
                        var params = parseQueryToParams(query);

                        String code = Objects.requireNonNull(params.get("code"));
                        String stateReceivedFromSpotify = Objects.requireNonNull(params.get("state"));

                        if (user.getStoredState().equals(stateReceivedFromSpotify)) {
                            System.out.println("code received");
                            user.setCode(code);
                        }
                    }
                }
        );
    }

    public static SpotifyUser reqeustTokensFromRemoteResource(SpotifyUser user) {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                //https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        String idAndSecretToEncode = user.getClientId() + ":" + user.getClientSecret();
        String encodedCreds = Base64.getEncoder().encodeToString(idAndSecretToEncode.getBytes());
//                            System.out.println("encoded creds value is: " + encodedCreds);
        encodedCreds = "Basic " + encodedCreds;

        Map<String, String> formData = new HashMap<>();
        // without id and secret in the form it was not working
        formData.put("client_id", user.getClientId());
        formData.put("client_secret", user.getClientSecret());
        formData.put("code", user.getCode());
        formData.put("redirect_uri", "http://localhost:8181/callback");
        formData.put("grant_type", "authorization_code");

        HttpRequest postRequest = HttpRequest.newBuilder()
                .header("Authorization", encodedCreds)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                .build();

        HttpResponse<String> postResponse = null;
        try {
            postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        String postBody = postResponse.body();
        System.out.println("response:");
        System.out.println(postBody);

        Gson gson = new Gson();
        SpotifyAuthResponse spotifyAuthResponse = gson.fromJson(postBody, SpotifyAuthResponse.class);

        if (spotifyAuthResponse.getAccess_token() != null) {
            user.setAccess_token(spotifyAuthResponse.getAccess_token());
            user.setRefresh_token(spotifyAuthResponse.getRefresh_token());
        }
        return user;
    }

    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

    private static Map<String, String> parseQueryToParams(String query) {
        /* Взято с https://stackoverflow.com/questions/11640025/how-to-obtain-the-query-string-in-a-get-with-java-httpserver-httpexchange
         * С небольшими изменениями
         */
        Map<String, String> result = new HashMap<>();
        if (query == null) {
            return result;
        }

        int last = 0;
        int next;
        int length = query.length();

        while (last < length) {
            next = query.indexOf('&', last);
            if (next == -1) {
                next = length;
            }

            if (next > last) {
                int eqPos = query.indexOf('=', last);
                try {
                    if (eqPos < 0 || eqPos > next) {
                        result.put(URLDecoder.decode(query.substring(last, next), "utf-8"), "");
                    } else {
                        result.put(URLDecoder.decode(query.substring(last, eqPos), "utf-8"), URLDecoder.decode(query.substring(eqPos + 1, next), "utf-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    assert false : "UTF-8 поддерживается Java 'Из коробки' - исключения быть не должно";
                }
            }
            last = next + 1;
        }
        return result;
    }

    public static void start() throws IOException {
        server = create();
        server.bind(new InetSocketAddress(8181), 0);
        server.setExecutor(null);
        server.start();
    }

    public static void stop(int delay) {
        System.out.println("Going to stop the server: ");
        server.stop(delay);
    }

    public class HTTPClient {
        public static String storedState;

        public static void sendInitialAuthRequestToRemoteResource(SpotifyUser user) throws IOException, InterruptedException, URISyntaxException {

            //https://stackoverflow.com/questions/69837157/setting-a-session-cookie-with-the-java-11-httpclient
            storedState = generateRandomState(10);
            user.setStoredState(storedState);

            CookieHandler.setDefault(new CookieManager());
            HttpCookie sessionCookie = new HttpCookie("spotify_auth_state", storedState);
            sessionCookie.setPath("/");

            ((CookieManager) CookieHandler.getDefault()).getCookieStore().add(new URI("https://accounts.spotify.com"),
                    sessionCookie);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    //https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            /* Формируем url */
            String responseType = "code";
            String redirectUri = "http://localhost:8181/callback";
            String scope = "user-read-private%20user-read-email";

            String url = String.format("https://accounts.spotify.com/authorize?client_id=%s&response_type=%s&redirect_uri=%s&state=%s", user.getClientId(), responseType, redirectUri, storedState);

            /* Формируем HTTP запрос */
            HttpRequest request = newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> initialGetRequest = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        }
    }

    private static String generateRandomState(int length) {
        char[] text = new char[length];
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();
        for (var i = 0; i < length; i++) {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        return new String(text);
    }
}