package boulderino.boulderino.controller;

import boulderino.boulderino.dto.StatResponseDTO;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;
    private final UserRepository userRepository;

    public StatsController(StatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get statistics of the logged-in user")
    @GetMapping
    public StatResponseDTO getStats(Authentication authentication) {
        
        User user = getCurrentUser(authentication);

        return statsService.getStats(user);
    }

    @Operation(summary = "Get statistics of specific Session of logged in User")
    @GetMapping("/session/{sessionId}")
    public StatResponseDTO getSessionStats(Authentication authentication, @PathVariable Long sessionId) {
        
        User user = getCurrentUser(authentication);

        return statsService.getSessionStats(sessionId, user);
    }

    private User getCurrentUser(Authentication authentication) {
        
        return userRepository.findByEmail(authentication.getName()).orElseThrow();
    }
}

