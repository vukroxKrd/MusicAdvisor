package server;

import advisor.Main;
import advisor.SpotifyUser;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static advisor.Main.domainName;
import static com.sun.net.httpserver.HttpServer.create;
import static java.net.http.HttpRequest.newBuilder;

public class HTTPServer {
    private static HttpServer server;
    public static String code;

    public static void listen(SpotifyUser user) {

        server.createContext("/",
                new HttpHandler() {
                    public void handle(HttpExchange exchange) throws IOException {
                        String query = exchange.getRequestURI().getQuery();
                        String msg;

                        if (query != null && query.contains("code")) {
                            Map<String, String> qParams = parseQueryToParams(query);
                            code = qParams.get("code");
                            msg = "Got the code. Return back to your program.";
                            user.setCode(code);
                            exchange.sendResponseHeaders(200, msg.length());
                            exchange.getResponseBody().write(msg.getBytes());
                            exchange.getResponseBody().close();
                        } else {
                            msg = "Authorization code not found. Try again.";
                            exchange.sendResponseHeaders(404, msg.length());
                            exchange.getResponseBody().write(msg.getBytes());
                            exchange.getResponseBody().close();
                        }
                    }
                }
        );
    }

    public static SpotifyUser reqeustTokensFromRemoteResource(SpotifyUser user, String code) {

        HttpClient client = HttpClient.newBuilder().build();

        String idAndSecretToEncode = user.getClientId() + ":" + user.getClientSecret();

        String encodedCreds = Base64.getEncoder().encodeToString(idAndSecretToEncode.getBytes());
        encodedCreds = "Basic " + encodedCreds;

        Map<String, String> formData = new HashMap<>();
        formData.put("code", code);
        formData.put("redirect_uri", "http://localhost:8181/");
        formData.put("grant_type", "authorization_code");

        HttpRequest postRequest = HttpRequest.newBuilder()
                .header("Authorization", encodedCreds)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(user.getEndpointUrl() + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                .build();

        HttpResponse<String> postResponse = null;
        String postBody = null;
        try {
            postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
            postBody = postResponse.body();
            System.out.println("response:");
            System.out.println(postBody);
            user.setAuthenticated(true);
            System.out.println("---SUCCESS---");
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

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
        System.out.println("use this link to request the access code: ");
        System.out.println(domainName + "/authorize?client_id=0db1be20b0494973a17516bae8af091d&redirect_uri=http://localhost:8181&response_type=code");
        System.out.println("waiting for code...");
    }

    public static void stop(int delay) {
        server.stop(delay);
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