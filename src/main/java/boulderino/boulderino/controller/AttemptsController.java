package boulderino.boulderino.controller;

import boulderino.boulderino.dto.AttemptsCreateDTO;
import boulderino.boulderino.dto.AttemptsResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.AttemptsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attempts")
@SecurityRequirement(name = "bearerAuth")
public class AttemptsController {

    private final AttemptsService attemptsService;
    private final UserRepository userRepository;

    public AttemptsController(
            AttemptsService attemptsService,
            UserRepository userRepository) {

        this.attemptsService = attemptsService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Show all attempts of the logged-in user")
    @GetMapping
    public List<AttemptsResponseDTO> getAllAttempts(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return attemptsService.getAllAttemptsByUser(user);
    }

    @Operation(summary = "Search for a specific attempt by its Id")
    @GetMapping("/{id}")
    public AttemptsResponseDTO getAttemptById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return attemptsService.getAttemptById(id, user);
    }

    @Operation(summary = "Create a new attempt")
    @PostMapping
    public Attempts createAttempt(
            @Valid @RequestBody AttemptsCreateDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return attemptsService.createAttempt(request, user);
    }

    @Operation(summary = "Update an existing attempt")
    @PutMapping("/{id}")
    public AttemptsResponseDTO updateAttempt(
            @PathVariable Long id,
            @Valid @RequestBody AttemptsResponseDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return attemptsService.updateAttempt(id, request, user);
    }

    @Operation(summary = "Delete an attempt")
    @DeleteMapping("/{id}")
    public void deleteAttempt(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        attemptsService.deleteAttempt(id, user);
    }

    private User getCurrentUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow();
    }
}