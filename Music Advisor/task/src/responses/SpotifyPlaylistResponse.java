package responses;

public class SpotifyPlaylistResponse extends SpotifyResponse{
    @Override
    public void printResponse() {
        System.out.println(super.getName());
        System.out.println(super.getUrl());
        System.out.println();
    }
}
