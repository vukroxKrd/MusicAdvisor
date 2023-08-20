package responses;

public class SpotifyNewReleasesReponse extends SpotifyResponse{
    @Override
    public void printResponse() {
        System.out.println(super.getName());
        System.out.println(super.getArtists());
        System.out.println(super.getUrl());
        System.out.println();
    }
}
