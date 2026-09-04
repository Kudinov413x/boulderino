package boulderino.boulderino.controller;

import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.BoulderService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import boulderino.boulderino.dto.BoulderCreateDTO;
import jakarta.validation.Valid;
import boulderino.boulderino.dto.BoulderResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/boulders")
@SecurityRequirement(name = "bearerAuth")
public class BoulderController {

    private final BoulderService boulderService;
    private final UserRepository userRepository;

    public BoulderController(
            BoulderService boulderService,
            UserRepository userRepository) {

        this.boulderService = boulderService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Show all boulders of the logged-in user")
    @GetMapping
    public List<BoulderResponseDTO> getAllBoulders(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return boulderService.getAllBouldersByUser(user);
    }

    @Operation(summary = "Search for a specific boulder by its Id")
    @GetMapping("/{id}")
    public BoulderResponseDTO getBoulderById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return boulderService.getBoulderById(id, user);
    }

    @Operation(summary = "Create a new Boulder")
    @PostMapping
    public Boulder createBoulder(
            @Valid @RequestBody BoulderCreateDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return boulderService.createBoulder(request, user);
    }

    @Operation(summary = "Update an existing Boulder")
    @PutMapping("/{id}")
    public BoulderResponseDTO updateBoulder(
            @PathVariable Long id,
            @Valid @RequestBody BoulderCreateDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return boulderService.updateBoulder(id, request, user);
    }

    @Operation(summary = "Delete a Boulder")
    @DeleteMapping("/{id}")
    public void deleteBoulder(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        boulderService.deleteBoulder(id, user);
    }

    private User getCurrentUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow();
    }
}