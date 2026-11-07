package boulderino.boulderino.controller;

import boulderino.boulderino.dto.HighlightResponseDTO;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.HighlightsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/highlights")
@SecurityRequirement(name = "bearerAuth")
public class HighlightsController {

    private final HighlightsService highlightsService;
    private final UserRepository userRepository;

    public HighlightsController(
            HighlightsService highlightsService,
            UserRepository userRepository) {

        this.highlightsService = highlightsService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get the five most relevant highlights of the logged-in user")
    @GetMapping
    public List<HighlightResponseDTO> getHighlights(
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow();

        return highlightsService.getHighlights(user);
    }
}