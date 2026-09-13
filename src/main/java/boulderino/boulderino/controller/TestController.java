package boulderino.boulderino.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.AttemptsService;

@RestController
@RequestMapping("/api/test")
@SecurityRequirement(name = "bearerAuth")
public class TestController {

    private final AttemptsService attemptsService;
    private final UserRepository userRepository;

    public TestController(AttemptsService attemptsService, UserRepository userRepository){
        this.attemptsService = attemptsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/auth")
    public String testAuthentication() {
        return "Sie sind eingeloggt";
    }

    //Updaten von first Ascent und flash für Boulder eines Nutzers. nur zum debugging
    @PostMapping("/recalculate-progress")
    public void recalculateProgress(Authentication authentication) {

        User user = getCurrentUser(authentication);

        attemptsService.recalculateProgressForUser(user);
    }

    private User getCurrentUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow();
    }
}