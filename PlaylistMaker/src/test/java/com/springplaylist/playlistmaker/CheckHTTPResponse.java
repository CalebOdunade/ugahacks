package com.springplaylist.playlistmaker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CheckHTTPResponse {

    @LocalServerPort
    private int port;

    private final TestRestTemplate testRestTemplate;

    @Autowired
    public CheckHTTPResponse(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    @Test
    public void test() {
        String baseUrl = "http://localhost:" + port + "/";
        String response = testRestTemplate.getForObject(baseUrl, String.class);
        assertEquals("Hello World from Playlist Maker", response);
    }
}
