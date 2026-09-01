package boulderino.boulderino.controller;

import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.service.BoulderService;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boulders")
public class BoulderController {

    private final BoulderService boulderService;

    public BoulderController(BoulderService boulderService) {
        this.boulderService = boulderService;
    }

    @Operation(summary = "show all boulders")
    @GetMapping
    public List<Boulder> getAllBoulders() {
        return boulderService.getAllBoulders();
    }

    @Operation(summary = "Search for a specific boulder by its Id")
    @GetMapping("/{id}")
    public Boulder getBoulderById(@PathVariable Long id) {
        return boulderService.getBoulderById(id);
    }

    @Operation(summary = "Create a new Boulder")
    @PostMapping
    public Boulder createBoulder(@RequestBody Boulder boulder) {
        return boulderService.createBoulder(boulder);
    }

    @Operation(summary = "Update an existing Boulder")
    @PutMapping("/{id}")
    public Boulder updateBoulder(
            @PathVariable Long id,
            @RequestBody Boulder boulder) {

        return boulderService.updateBoulder(id, boulder);
    }

    @Operation(summary = "Delete a Boulder")
    @DeleteMapping("/{id}")
    public void deleteBoulder(@PathVariable Long id) {
        boulderService.deleteBoulder(id);
    }
}