package com.safetyparis.safetyparis_api.controller;

import com.safetyparis.safetyparis_api.dto.HelpLocationResponseDto;
import com.safetyparis.safetyparis_api.service.HelpLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/help-locations")
@RequiredArgsConstructor
public class HelpLocationController {

    private final HelpLocationService helpLocationService;

    @GetMapping
    public ResponseEntity<List<HelpLocationResponseDto>> getHelpLocations() {
        return ResponseEntity.ok(helpLocationService.getAllHelpLocations());
    }
}
