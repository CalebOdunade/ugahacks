package com.springplaylist.playlistmaker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/top-songs")
    public String showTopSongsPage() {
        return "top-songs";
    }
}
