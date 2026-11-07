package boulderino.boulderino.controller;

import boulderino.boulderino.dto.GradeStatsResponseDTO;
import boulderino.boulderino.dto.AttributeStatsResponseDTO;
import boulderino.boulderino.dto.FilteredProgressDTO;
import boulderino.boulderino.dto.GradeDistributionResponseDTO;
import boulderino.boulderino.dto.MaxGradeOverTimeDTO;
import boulderino.boulderino.dto.ProgressFilterCountResponseDTO;
import boulderino.boulderino.dto.ProgressResponseDTO;
import boulderino.boulderino.dto.StrengthWeaknessResponseDTO;
import boulderino.boulderino.entity.BoulderAttribute;
import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.entity.WallAngle;
import boulderino.boulderino.repository.UserRepository;
import boulderino.boulderino.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import boulderino.boulderino.entity.Grade;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import boulderino.boulderino.dto.ProgressOverTimeResponseDTO;
import boulderino.boulderino.service.ProgressOverTimeService;

@RestController
@RequestMapping("/progress")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;
    private final ProgressOverTimeService progressOverTimeService;
    private final UserRepository userRepository;

    public ProgressController(
            ProgressService progressService,
            ProgressOverTimeService progressOverTimeService,
            UserRepository userRepository) {

        this.progressService = progressService;
        this.progressOverTimeService = progressOverTimeService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get General progress of the logged-in user")
    @GetMapping
    public ProgressResponseDTO getProgress(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return progressService.getProgress(user);
    }

    @Operation(summary = "Get progress statistics for every grade")
    @GetMapping("/grades")
    public List<GradeStatsResponseDTO> getGradeStats(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return progressService.getGradeStats(user);
    }

    @Operation(summary = "Get a Timeline of all Personal Bests")
    @GetMapping("/timeline/max-grade")
    public List<MaxGradeOverTimeDTO> getMaxGradeOverTime(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return progressService.getMaxGradeOverTime(user);
    }

    @Operation(summary = "Get max grade within a specific time period")
    @GetMapping("withinTimeFrame/max-grade")
    public String getMaxGradeForPeriod(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return progressService.getMaxGradeForPeriod(user, startDate, endDate);
    }

    // Prozentuale Verteilung der Grades aller getoppten Routen
    @Operation(summary = "Get grade distribution of the logged-in user")
    @GetMapping("/grade-distribution")
    public GradeDistributionResponseDTO getGradeDistribution(Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        return progressService.getGradeDistribution(user);
    }

    @Operation(summary = "Get progress statistics for a specific boulder attribute")
    @GetMapping("/attributes")
    public List<AttributeStatsResponseDTO> getProgressByAttribute(Authentication authentication, @RequestParam BoulderAttribute attributeType) {

        User user = getCurrentUser(authentication);

        return progressService.getProgressByAttribute(user, attributeType);
    }

    @Operation(summary = "Get progress statistics filtered by boulder attributes and grade range")
    @GetMapping("/filter")
    public FilteredProgressDTO getProgressByFilters(
            Authentication authentication,
            @RequestParam(required = false) GripType gripType,
            @RequestParam(required = false) WallAngle wallAngle,
            @RequestParam(required = false) ClimbingStyle climbingStyle,
            @RequestParam(required = false) RouteCharacter routeCharacter,
            @RequestParam(required = false) Grade minGrade,
            @RequestParam(required = false) Grade maxGrade) {

        User user = getCurrentUser(authentication);

        return progressService.getProgressByFilters(
                user,
                gripType,
                wallAngle,
                climbingStyle,
                routeCharacter,
                minGrade,
                maxGrade
        );
    }

    @Operation(summary = "Returns the number of matching boulders for every available filter option based on the other currently selected filters. The filter group currently being calculated is ignored.")
    @GetMapping("/filter-options")
    public ProgressFilterCountResponseDTO getFilterOptionCounts(
            Authentication authentication,
            @RequestParam(required = false) GripType gripType,
            @RequestParam(required = false) WallAngle wallAngle,
            @RequestParam(required = false) ClimbingStyle climbingStyle,
            @RequestParam(required = false) RouteCharacter routeCharacter,
            @RequestParam(required = false) Grade minGrade,
            @RequestParam(required = false) Grade maxGrade
    ) {
        User user = getCurrentUser(authentication);

        return progressService.getFilterOptionCounts(
                user,
                gripType,
                wallAngle,
                climbingStyle,
                routeCharacter,
                minGrade,
                maxGrade
        );
    }


    @Operation(summary = "Get a List of Strengths and weaknesses of a logged in User")
    @GetMapping("/strengths-weaknesses")
    public StrengthWeaknessResponseDTO getStrengthsAndWeaknesses(Authentication authentication) {

        User user = getCurrentUser(authentication);

        return progressService.getStrengthsAndWeaknesses(user);
    }

    private User getCurrentUser(Authentication authentication) {

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow();
    }

    @Operation(summary = "Get climbing progress over time for each Session of the logged-in user")
    @GetMapping("/timeline/over-time")
    public ProgressOverTimeResponseDTO getProgressOverTime(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return progressOverTimeService.getProgressOverTime(user);
    }
}