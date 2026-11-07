package boulderino.boulderino.controller;

import boulderino.boulderino.dto.SessionInsightsResponseDTO;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.SessionRepository;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.SessionStatInsightService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/sessions")
@SecurityRequirement(name = "bearerAuth")
public class SessionStatInsightController {

    private final SessionStatInsightService sessionStatInsightService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionStatInsightController(
            SessionStatInsightService sessionStatInsightService,
            SessionRepository sessionRepository,
            UserRepository userRepository) {

        this.sessionStatInsightService = sessionStatInsightService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get all insights for a specific session")
    @GetMapping("/{sessionId}/insights")
    public SessionInsightsResponseDTO getSessionInsights(
            @PathVariable Long sessionId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Session nicht gefunden"
                        )
                );

        // Prüfen, ob die Session dem eingeloggten User gehört
        if (!Objects.equals(session.getUser().getId(), user.getId())) {
            throw new AccessDeniedException(
                    "Du hast keinen Zugriff auf diese Session."
            );
        }

        return sessionStatInsightService.getSessionInsights(session, user);
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow();
    }
}