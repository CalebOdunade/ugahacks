package com.springplaylist.playlistmaker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import jakarta.annotation.PostConstruct;

import java.net.URI;

@Controller
public class AuthController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri; // Store as String first

    private SpotifyApi spotifyApi;
    private String accessToken;
    private URI redirectUriObject; // Store as URI

    @PostConstruct
    public void init() {
        this.redirectUriObject = SpotifyHttpManager.makeUri(redirectUri); // Convert String to URI
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUriObject)
                .build();
    }

    @GetMapping("/")
    public RedirectView login() {
        String authorizeURL = spotifyApi.authorizationCodeUri()
                .scope("playlist-modify-private playlist-modify-public user-top-read")
                .build()
                .execute()
                .toString();
        return new RedirectView(authorizeURL);
    }


    @GetMapping("/callback")
    public RedirectView callback(@RequestParam("code") String code) {
        try {
            AuthorizationCodeCredentials credentials = spotifyApi.authorizationCode(code).build().execute();
            accessToken = credentials.getAccessToken();
            spotifyApi.setAccessToken(accessToken);
            return new RedirectView("/top-songs");
        } catch (Exception e) {
            return new RedirectView("/error");
        }
    }


    public String getAccessToken() {
        return accessToken;
    }

    public URI getRedirectUri() {
        return redirectUriObject;
    }

}
