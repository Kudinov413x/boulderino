package boulderino.boulderino.controller;

import boulderino.boulderino.dto.SessionUpdateDTO;
import boulderino.boulderino.dto.SessionCreateDTO;
import boulderino.boulderino.dto.SessionResponseDTO;
import boulderino.boulderino.dto.SessionDetailsResponseDTO;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    public SessionController(
            SessionService sessionService,
            UserRepository userRepository) {

        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Show all sessions of the logged-in user")
    @GetMapping
    public List<SessionResponseDTO> getAllSessions(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return sessionService.getAllSessionsByUser(user);
    }

    @Operation(summary = "Search for a specific session by its Id")
    @GetMapping("/{id}")
    public SessionResponseDTO getSessionById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return sessionService.getSessionById(id, user);
    }

    @Operation(summary = "Create a new Session")
    @PostMapping
    public Session createSession(
            @Valid @RequestBody SessionCreateDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return sessionService.createSession(request, user);
    }

    @Operation(summary = "Update an existing Session")
    @PutMapping("/{id}")
    public SessionResponseDTO updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionUpdateDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return sessionService.updateSession(id, request, user);
    }

    @Operation(summary = "Delete a Session")
    @DeleteMapping("/{id}")
    public void deleteSession(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        sessionService.deleteSession(id, user);
    }

    private User getCurrentUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow();
    }

    @Operation(summary = "Show all attempts and boulder details of a specific session")
    @GetMapping("/{sessionId}/details")
    public List<SessionDetailsResponseDTO> getSessionDetails(@PathVariable Long sessionId, Authentication authentication) {

        User user = getCurrentUser(authentication);

        return sessionService.getSessionDetails(sessionId, user);
    }
}