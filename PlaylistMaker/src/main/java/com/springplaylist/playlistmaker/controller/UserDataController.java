package com.springplaylist.playlistmaker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.detailed.BadGatewayException;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@RestController
public class UserDataController {

    private final AuthController authController;
    private final SpotifyApi spotifyApi;
    private final String cohereApiKey;

    public UserDataController(AuthController authController,
                              @Value("${spotify.client-id}") String clientId,
                              @Value("${spotify.client-secret}") String clientSecret,
                              @Value("${cohere.api-key}") String cohereApiKey) {
        this.authController = authController;
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(authController.getRedirectUri())
                .build();
        this.cohereApiKey = cohereApiKey;
    }

    @GetMapping("/top-songs-page")
    public ResponseEntity<?> createPlaylist() {
        try {
            String accessToken = authController.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No access token. Please log in first.");
            }

            spotifyApi.setAccessToken(accessToken);

            // Fetch top tracks
            GetUsersTopTracksRequest request = spotifyApi.getUsersTopTracks().build();
            Track[] tracks = request.execute().getItems();

            if (tracks.length == 0) {
                return ResponseEntity.ok().body(Map.of("message", "No top tracks found."));
            }

            List<String> trackUris = new ArrayList<>();
            List<String> trackNames = new ArrayList<>();

            for (Track track : tracks) {
                trackUris.add(track.getUri());
                trackNames.add(track.getName());
            }

            String userId = spotifyApi.getCurrentUsersProfile().build().execute().getId();

            // Generate playlist description using Cohere AI
            String aiGeneratedDescription = generatePlaylistDescription(trackNames);

            // Create the playlist
            Playlist playlist = createPlaylistWithRetries(userId, "My Top Songs Playlist", aiGeneratedDescription);

            // Add tracks to the playlist
            int batchSize = 100;
            for (int i = 0; i < trackUris.size(); i += batchSize) {
                String[] batch = trackUris.subList(i, Math.min(i + batchSize, trackUris.size()))
                        .toArray(new String[0]);
                AddItemsToPlaylistRequest addItemsRequest = spotifyApi
                        .addItemsToPlaylist(playlist.getId(), batch)
                        .build();
                addItemsRequest.execute();
            }

            String playlistUrl = playlist.getExternalUrls().get("spotify");

            return ResponseEntity.ok(Map.of(
                    "playlistUrl", playlistUrl,
                    "trackNames", trackNames,
                    "playlistDescription", aiGeneratedDescription
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error creating playlist", "details", e.getMessage()));
        }
    }

    private String generatePlaylistDescription(List<String> trackNames) {
        try {
            String prompt = "Generate a creative playlist description based on these songs: " + String.join(", ", trackNames);
            String apiUrl = "https://api.cohere.ai/generate";

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + cohereApiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Request body for Cohere API
            String requestBody = """
                    {
                        "model": "command-xlarge-nightly",
                        "prompt": "%s",
                        "max_tokens": 100,
                        "temperature": 0.8
                    }
                    """.formatted(prompt);

            connection.getOutputStream().write(requestBody.getBytes());

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            Map<String, Object> jsonResponse = new ObjectMapper().readValue(response.toString(), Map.class);
            return ((List<Map<String, Object>>) jsonResponse.get("generations"))
                    .get(0).get("text").toString().trim();

        } catch (Exception e) {
            return "A great collection of top tracks!";
        }
    }

    private Playlist createPlaylistWithRetries(String userId, String playlistName, String description) throws Exception {
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            try {
                CreatePlaylistRequest createPlaylistRequest = spotifyApi
                        .createPlaylist(userId, playlistName)
                        .description(description)
                        .public_(false)
                        .build();
                return createPlaylistRequest.execute();
            } catch (BadGatewayException e) {
                if (i == retries - 1) {
                    throw e;
                }
                System.out.println("Retrying playlist creation...");
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("Failed to create playlist after retries.");
    }
}