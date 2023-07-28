package advisor;

public class SpotifyUser {

    private String clientId;
    private String clientSecret;
    private boolean isAuthenticated = false;
    private String code;
    private String storedState;
    private String access_token;
    private String refresh_token;

    private String endpointUrl;
    public SpotifyUser() {
    }

    public SpotifyUser(String clientId, String clientSecret, boolean isAuthenticated) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.isAuthenticated = false;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getStoredState() {
        return storedState;
    }
    public void setStoredState(String storedState) {
        this.storedState = storedState;
    }

    public String getAccess_token() {
        return access_token;
    }
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
