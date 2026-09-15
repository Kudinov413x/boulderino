package boulderino.boulderino.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import boulderino.boulderino.dto.FrontendTestDTO;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/frontend/test")
public class FrontendTestController {
    
    @PostMapping("/FrontendTest")
    public FrontendTestDTO frontendTest() {
        return new FrontendTestDTO(
            "Test User",
            "FB7A",
            42,
            27
        );
        
    }
}
