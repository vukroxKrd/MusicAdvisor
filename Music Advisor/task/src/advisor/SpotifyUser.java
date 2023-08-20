package advisor;

public class SpotifyUser {

    private String clientId;
    private String clientSecret;
    private boolean isAuthenticated = false;
    private String code;
    private String storedState;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
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

    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public String toString() {
        return "SpotifyUser{" +
                "clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", isAuthenticated=" + isAuthenticated +
                ", code='" + code + '\'' +
                ", storedState='" + storedState + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                '}';
    }
}
