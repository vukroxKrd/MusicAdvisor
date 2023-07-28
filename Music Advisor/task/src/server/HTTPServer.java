package server;

import advisor.Main;
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
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static advisor.Main.domainName;
import static com.sun.net.httpserver.HttpServer.create;
import static java.net.http.HttpRequest.newBuilder;

public class HTTPServer {
    private static HttpServer server;
    static String code;

    public static void listen(SpotifyUser user) {

        server.createContext("/",
                new HttpHandler() {
                    public void handle(HttpExchange exchange) throws IOException {


                        String query = exchange.getRequestURI().getQuery();
                        String msg;


                        if (query != null && query.contains("code")) {
                            Map<String, String> qParams = parseQueryToParams(query);
//                            System.out.println("code received");
                            code = qParams.get("code");
//                            code = query.substring(query.indexOf('=') + 1, query.indexOf('&')); //variable of the class
                            msg = "Got the code. Return back to your program.";
                            System.out.println("Got the code. Return back to your program.");
                            user.setCode(code);
                            exchange.sendResponseHeaders(200, msg.length());
                            exchange.getResponseBody().write(msg.getBytes());
                            exchange.getResponseBody().close();
//                            server.stop(0);
                        } else {
                            msg = "Authorization code not found. Try again.";
                            System.out.println(msg);
                            exchange.sendResponseHeaders(404, msg.length());
                            exchange.getResponseBody().write(msg.getBytes());
                            exchange.getResponseBody().close();
                        }
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
                    }
                }
        );
    }

    public static SpotifyUser reqeustTokensFromRemoteResource(SpotifyUser user) {

        HttpClient client = HttpClient.newBuilder()
                //https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a
                .build();

        String idAndSecretToEncode = user.getClientId() + ":" + user.getClientSecret();
        String encodedCreds = Base64.getEncoder().encodeToString(idAndSecretToEncode.getBytes());
//                            System.out.println("encoded creds value is: " + encodedCreds);
        encodedCreds = "Basic " + encodedCreds;

        Map<String, String> formData = new HashMap<>();
        // without id and secret in the form it was not working
        formData.put("client_id", user.getClientId());
        formData.put("client_secret", user.getClientSecret());
        formData.put("code", code);
        formData.put("redirect_uri", "http://localhost:8181");
        formData.put("grant_type", "authorization_code");
        //!!!
        System.out.println(user.getEndpointUrl());

        HttpRequest postRequest = HttpRequest.newBuilder()
                .header("Authorization", encodedCreds)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create("https://accounts.spotify.com/" + user.getEndpointUrl()))
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                .build();
        System.out.println(postRequest);

        HttpResponse<String> postResponse = null;
        String postBody = null;
        try {
            postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
            HttpHeaders headers = postResponse.headers();
            System.out.println(postResponse.statusCode());
            Map<String, List<String>> hdrs = headers.map();
            hdrs.forEach((s, strings) -> {
                System.out.println("Key is: " + s);
                strings.forEach(System.out::println);
            });

            postBody = postResponse.body();
            System.out.println("response:");
            System.out.println(postBody);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

//        Gson gson = new Gson();
//        SpotifyAuthResponse spotifyAuthResponse = gson.fromJson(postBody, SpotifyAuthResponse.class);

//        if (spotifyAuthResponse.getAccess_token() != null) {
//            user.setAccess_token(spotifyAuthResponse.getAccess_token());
//            user.setRefresh_token(spotifyAuthResponse.getRefresh_token());


//            String responseStr = "\"{\"access_token\":" + user.getAccess_token() + ",\"scope\":\"\"}";
//            System.out.println(responseStr);
//        }
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
        System.out.println("use this link to request the access code: ");
        System.out.println(domainName + "/authorize?client_id=0db1be20b0494973a17516bae8af091d&redirect_uri=http://localhost:8181&response_type=code");
        System.out.println("waiting for code...");
    }

    public static void stop(int delay) {
        server.stop(delay);
    }

    public static class HTTPClient {
        public static String storedState;

        public static void sendInitialAuthRequestToRemoteResource(SpotifyUser user) throws IOException, InterruptedException, URISyntaxException {

            //https://stackoverflow.com/questions/69837157/setting-a-session-cookie-with-the-java-11-httpclient
            storedState = generateRandomState(10);
            user.setStoredState(storedState);

//            CookieHandler.setDefault(new CookieManager());
//            HttpCookie sessionCookie = new HttpCookie("spotify_auth_state", storedState);
//            sessionCookie.setPath("/");
//
//            ((CookieManager) CookieHandler.getDefault()).getCookieStore().add(new URI(Main.domainName),
//                    sessionCookie);

            HttpClient client = HttpClient.newBuilder()
                    //https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            /* Формируем url */
            String responseType = "code";
            String redirectUri = "http://localhost:8181";
            String scope = "user-read-private%20user-read-email";

            String url = String.format("%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&state=%s", Main.domainName, user.getClientId(), responseType, redirectUri, storedState);

            /* Формируем HTTP запрос */
            HttpRequest request = newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> initialGetRequest = client.send(request, HttpResponse.BodyHandlers.ofString());


            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
            Thread.sleep(5000);
        }

    }

    public static String generateRandomState(int length) {
        char[] text = new char[length];
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();
        for (var i = 0; i < length; i++) {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        return new String(text);
    }
}