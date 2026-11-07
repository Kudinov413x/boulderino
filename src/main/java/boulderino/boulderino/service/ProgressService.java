package boulderino.boulderino.service;

import boulderino.boulderino.dto.AttributeStatsResponseDTO;
import boulderino.boulderino.dto.CompareAttributeDTO;
import boulderino.boulderino.dto.FilteredProgressDTO;
import boulderino.boulderino.dto.GradeDistributionDTO;
import boulderino.boulderino.dto.GradeDistributionResponseDTO;
import boulderino.boulderino.dto.GradeStatsResponseDTO;
import boulderino.boulderino.dto.MaxGradeOverTimeDTO;
import boulderino.boulderino.dto.ProgressFilterCountDTO;
import boulderino.boulderino.dto.ProgressFilterCountResponseDTO;
import boulderino.boulderino.dto.ProgressResponseDTO;
import boulderino.boulderino.dto.StrengthWeaknessDTO;
import boulderino.boulderino.dto.StrengthWeaknessResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.BoulderAttribute;
import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.entity.WallAngle;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.BoulderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class ProgressService {

        private final BoulderRepository boulderRepository;
        private final AttemptsRepository attemptsRepository;

        public ProgressService(BoulderRepository boulderRepository, AttemptsRepository attemptsRepository) {
                this.boulderRepository = boulderRepository;
                this.attemptsRepository = attemptsRepository;
        }

        //General Progress
        public ProgressResponseDTO getProgress(User user) {

                ProgressContext context = createProgressContext(user);
                ProgressResponseDTO response = new ProgressResponseDTO();

                response.setMaxGrade(calculateMaxGrade(context));
                response.setAvgGrade(calculateAverageGrade(context));
                response.setFlashRate(calculateFlashRate(context));
                response.setTotalTriedBoulders(context.getTriedBoulders().size());
                response.setTotalToppedBoulders(calculateTotalToppedBoulders(context));
                response.setTotalSuccessRate(calculateTotalSuccessRate(context));

                return response;
        }

        //Progress by Grade
        public List<GradeStatsResponseDTO> getGradeStats(User user) {

                ProgressContext context = createProgressContext(user);
                List<Boulder> boulders = context.getBoulders();

                return Arrays.stream(Grade.values())
                        .filter(grade ->
                        boulders.stream()
                                .filter(boulder ->
                                        boulder.getGrade() == grade)
                                .anyMatch(boulder -> hasBeenTried(boulder, context)))
                        .map(grade -> {
                        GradeStatsResponseDTO response = new GradeStatsResponseDTO();
                        response.setGrade(grade);

                        long amountOfTriedBoulders = calculateTriedBouldersForGrade(boulders, grade, context);
                        long amountOfTops = calculateTopsForGrade(boulders, grade, context);
                        long amountOfZones = calculateZonesForGrade(boulders, grade, context);
                        long amountOfFlashes = calculateFlashesForGrade(boulders, grade, context);
                        response.setAmountOfTriedBoulders(amountOfTriedBoulders);
                        response.setAmountOfTops(amountOfTops);
                        response.setAmountOfZones(amountOfZones);
                        response.setAmountOfFlashes(amountOfFlashes);
                        response.setOpenProjects(amountOfTriedBoulders - amountOfTops);
                        response.setSuccessRate(calculateSuccessRateForGrade(boulders, grade, context));
                        response.setFlashRate(calculateFlashRateForGrade(boulders, grade, context));
                        response.setAverageAttemptsUntilTop(calculateAverageAttemptsUntilTopForGrade(boulders, grade,context));
                        response.setAverageTriesOnOpenProjects(calculateAverageTriesOnOpenProjectsForGrade(boulders, grade, context));

                        return response;
                        }).toList();
        }

        //Hilfsmethode für Progress per Grade
        private long calculateTriedBouldersForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                return boulders.stream()
                        .filter(boulder ->
                                boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .count();
        }

        //Hilfsmethode für Progress per Grade
        private long calculateTopsForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                return boulders.stream()
                        .filter(boulder ->
                                boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .filter(boulder ->
                                Boolean.TRUE.equals(
                                        boulder.getFirstAscent()))
                        .count();
        }

        private long calculateZonesForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {
        return boulders.stream()
                .filter(boulder -> boulder.getGrade() == grade)
                .filter(boulder -> {
                        List<Attempts> attempts = context.getAttemptsByBoulder()
                                .getOrDefault(boulder, List.of());

                        boolean hasZone = attempts.stream()
                                .anyMatch(Attempts::isZone);

                        boolean hasTop = attempts.stream()
                                .anyMatch(Attempts::isDone);

                        return hasZone && !hasTop;
                })
                .count();
        }


        private long calculateFlashesForGrade( List<Boulder> boulders, Grade grade, ProgressContext context) {
                return boulders.stream()
                        .filter(boulder -> boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFlashed()))
                        .count();
        }

        //Hilfsmethode für Progress per Grade
        private double calculateAverageTriesOnOpenProjectsForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                return boulders.stream()
                        .filter(boulder ->
                                boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .filter(boulder ->
                                !Boolean.TRUE.equals(
                                        boulder.getFirstAscent()))
                        .mapToLong(boulder -> calculateTotalTriesForBoulder(boulder, context))
                        .average()
                        .orElse(0.0);
        }

        private double calculateSuccessRateForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                long triedBoulders = boulders.stream()
                        .filter(boulder -> boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .count();

                if (triedBoulders == 0) {
                        return 0.0;
                }

                long toppedBoulders = boulders.stream()
                        .filter(boulder -> boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .count();

                return (double) toppedBoulders / triedBoulders * 100;
        }

        private String calculateMaxGrade(ProgressContext context) {

                return context.getBoulders()
                        .stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .map(Boulder::getGrade)
                        .max(this::compareGrades)
                        .map(Grade::getDisplayName)
                        .orElse(null);
        }

        public List<MaxGradeOverTimeDTO> getMaxGradeOverTime(User user) {

                List<Boulder> completedBoulders =
                        getCompletedBoulders(user)
                                .stream()
                                .filter(boulder ->
                                        boulder.getFirstAscentDate()
                                                != null)
                                .sorted(Comparator.comparing(Boulder::getFirstAscentDate))
                                .toList();

                List<MaxGradeOverTimeDTO> result = new ArrayList<>();

                Grade currentMaxGrade = null;

                for (Boulder boulder : completedBoulders) {
                if (currentMaxGrade == null || compareGrades(boulder.getGrade(), currentMaxGrade) > 0) {
                        currentMaxGrade = boulder.getGrade();
                        MaxGradeOverTimeDTO response = new MaxGradeOverTimeDTO();
                        response.setDate(boulder.getFirstAscentDate());
                        response.setMaxGrade(currentMaxGrade);
                        result.add(response);
                }
                }
                return result;
        }

        public String getMaxGradeForPeriod(User user, LocalDate startDate, LocalDate endDate) {

                return getCompletedBoulders(user)
                        .stream()
                        .filter(boulder ->
                                boulder.getFirstAscentDate() != null)
                        .filter(boulder ->
                                !boulder.getFirstAscentDate().isBefore(startDate))
                        .filter(boulder ->
                                !boulder.getFirstAscentDate().isAfter(endDate))
                        .map(Boulder::getGrade)
                        .max(this::compareGrades)
                        .map(Grade::getDisplayName)
                        .orElse(null);
        }

        private String calculateAverageGrade(ProgressContext context) {

                double averageValue = context.getBoulders()
                        .stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .map(Boulder::getGrade)
                        .mapToInt(this::getGradeValue)
                        .average()
                        .orElse(0.0);

                if (averageValue == 0.0) {
                        return null;
                }

                return getGradeFromValue(averageValue);
        }

        private long calculateTotalTriesForBoulder(Boulder boulder, ProgressContext context) {

                return context.getAttemptsByBoulder()
                        .getOrDefault(boulder, List.of())
                        .stream()
                        .mapToLong(Attempts::getTries)
                        .sum();
        }

        private long calculateTotalToppedBoulders(ProgressContext context) {

                return context.getBoulders()
                        .stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .count();
        }

        private double calculateTotalSuccessRate(ProgressContext context) {

                long totalTriedBoulders = context.getTriedBoulders().size();

                if (totalTriedBoulders == 0) {
                        return 0.0;
                }

                long totalToppedBoulders = calculateTotalToppedBoulders(context);

                return (double) totalToppedBoulders / totalTriedBoulders * 100;
        }

        private double calculateFlashRate(ProgressContext context) {

                List<Boulder> triedBoulders =
                        context.getTriedBoulders();

                if (triedBoulders.isEmpty()) {
                        return 0.0;
                }

                long flashedBoulders = triedBoulders.stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFlashed()))
                        .count();

                return (double) flashedBoulders / triedBoulders.size() * 100;
        }

        private double calculateFlashRateForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                List<Boulder> triedBoulders = boulders.stream()
                        .filter(boulder ->
                                boulder.getGrade() == grade)
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .toList();

                if (triedBoulders.isEmpty()) {
                        return 0.0;
                }

                long flashedBoulders = triedBoulders.stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFlashed()))
                        .count();

                return (double) flashedBoulders / triedBoulders.size() * 100;
        }

        private long calculateAttemptsUntilTop(Boulder boulder, ProgressContext context) {

                List<Attempts> attempts =
                        context.getAttemptsByBoulder()
                                .getOrDefault(boulder, List.of())
                                .stream()
                                .sorted(Comparator.comparing(Attempts::getCreatedAt))
                                .toList();

                long totalTries = 0;

                for (Attempts attempt : attempts) {

                        totalTries += attempt.getTries();

                        if (attempt.isDone()) {
                                return totalTries;
                        }
                }

                return 0;
        }

        private List<Boulder> getCompletedBoulders(User user) {

                return boulderRepository.findByUser(user)
                        .stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(
                                        boulder.getFirstAscent()))
                        .toList();
        }


        private double calculateAverageAttemptsUntilTopForGrade(List<Boulder> boulders, Grade grade, ProgressContext context) {

                return boulders.stream()
                        .filter(boulder ->
                                boulder.getGrade() == grade)
                        .filter(boulder ->
                                Boolean.TRUE.equals(
                                        boulder.getFirstAscent()))
                        .mapToLong(boulder ->
                                calculateAttemptsUntilTop(
                                        boulder,
                                        context))
                        .average()
                        .orElse(0.0);
        }

        private int compareGrades(Grade grade1, Grade grade2) {

                return Integer.compare(
                        getGradeValue(grade1),
                        getGradeValue(grade2)
                );
        }

        public GradeDistributionResponseDTO getGradeDistribution(User user) {

                ProgressContext context = createProgressContext(user);
                List<Boulder> triedBoulders = context.getTriedBoulders();

                long totalTops = triedBoulders.stream()
                                .filter(boulder -> Boolean.TRUE.equals(boulder.getFirstAscent()))
                                .count();

                List<GradeDistributionDTO> gradeDistribution = Arrays.stream(Grade.values())
                        .filter(grade ->
                                triedBoulders.stream()
                                        .anyMatch(boulder ->
                                                boulder.getGrade() == grade))
                        .map(grade -> {
                        long topsOfGrade =
                                triedBoulders.stream()
                                        .filter(boulder ->
                                                boulder.getGrade() == grade)
                                        .filter(boulder ->
                                                Boolean.TRUE.equals(
                                                        boulder.getFirstAscent()))
                                        .count();
                        double percentage = 0.0;
                        if (totalTops > 0) {
                                percentage =
                                        (double) topsOfGrade
                                                / totalTops
                                                * 100;
                        }
                        GradeDistributionDTO response = new GradeDistributionDTO();
                        response.setGrade(grade);
                        response.setPercentage(percentage);
                        response.setAbsoluteTops(topsOfGrade);
                        return response;
                        }).toList();

                GradeDistributionResponseDTO response = new GradeDistributionResponseDTO();

                response.setTotalTops(totalTops);
                response.setGrades(gradeDistribution);
                return response;
        }

        private int getGradeValue(Grade grade) {

                return switch (grade) {

                case FB1 -> 1;
                case FB1_PLUS -> 2;

                case FB2 -> 3;
                case FB2_PLUS -> 4;

                case FB3 -> 5;
                case FB3_PLUS -> 6;

                case FB4 -> 7;
                case FB4_PLUS -> 8;

                case FB5 -> 9;
                case FB5_PLUS -> 10;

                case FB6 -> 11;
                case FB6_PLUS -> 12;

                case FB6_A -> 13;
                case FB6_B -> 14;
                case FB6_C -> 15;

                case FB7 -> 16;
                case FB7_PLUS -> 17;

                case FB7_A -> 18;
                case FB7_B -> 19;
                case FB7_C -> 20;

                case FB8 -> 21;
                case FB8_PLUS -> 22;
                };
        }

        private String getGradeFromValue(double averageValue) {

                Grade closestGrade = null;
                double smallestDifference = Double.MAX_VALUE;

                for (Grade grade : Grade.values()) {

                        double difference = Math.abs(getGradeValue(grade) - averageValue);

                        if (difference < smallestDifference) {
                                smallestDifference = difference;
                                closestGrade = grade;
                        }
                }

                return closestGrade != null
                        ? closestGrade.getDisplayName()
                        : null;
        }

        //Hilfsmethode für das Berechnen vpn Stärken und Schwächen
        private List<AttributeStatsResponseDTO> getProgressByAttributeSW(ProgressContext context, BoulderAttribute attributeType) {

                List<Boulder> boulders = context.getBoulders();

                return switch (attributeType) {

                        case GRIP_TYPE ->
                                calculateAttributeProgress(
                                        boulders,
                                        GripType.values(),
                                        Boulder::getGripType,
                                        context
                                );

                        case WALL_ANGLE ->
                                calculateAttributeProgress(
                                        boulders,
                                        WallAngle.values(),
                                        Boulder::getWallAngle,
                                        context
                                );

                        case CLIMBING_STYLE ->
                                calculateAttributeProgress(
                                        boulders,
                                        ClimbingStyle.values(),
                                        Boulder::getClimbingStyle,
                                        context
                                );

                        case ROUTE_CHARACTER ->
                                calculateAttributeProgress(
                                        boulders,
                                        RouteCharacter.values(),
                                        Boulder::getRouteCharacter,
                                        context
                                );
                };
        }

        //Methoden zur Abfrage von Progress bei verschiedenen Boulder Enums
        public List<AttributeStatsResponseDTO> getProgressByAttribute(User user, BoulderAttribute attributeType) {

                ProgressContext context = createProgressContext(user);
                List<Boulder> boulders = context.getBoulders();

                return switch (attributeType) {

                        case GRIP_TYPE ->
                                calculateAttributeProgress(
                                        boulders,
                                        GripType.values(),
                                        Boulder::getGripType,
                                        context
                                );

                        case WALL_ANGLE ->
                                calculateAttributeProgress(
                                        boulders,
                                        WallAngle.values(),
                                        Boulder::getWallAngle,
                                        context
                                );

                        case CLIMBING_STYLE ->
                                calculateAttributeProgress(
                                        boulders,
                                        ClimbingStyle.values(),
                                        Boulder::getClimbingStyle,
                                        context
                                );

                        case ROUTE_CHARACTER ->
                                calculateAttributeProgress(
                                        boulders,
                                        RouteCharacter.values(),
                                        Boulder::getRouteCharacter,
                                        context
                                );
                };
        }

        private <T extends Enum<T>> List<AttributeStatsResponseDTO> calculateAttributeProgress(
        List<Boulder> boulders,
        T[] values,
        Function<Boulder, T> attributeGetter,
        ProgressContext context) {

                return Arrays.stream(values)
                        .filter(value ->
                                boulders.stream()
                                        .anyMatch(boulder ->
                                                attributeGetter.apply(boulder) == value))
                        .map(value -> {

                                List<Boulder> filteredBoulders =
                                        boulders.stream()
                                                .filter(boulder ->
                                                        attributeGetter.apply(boulder) == value)
                                                .toList();

                                // Nur Boulder mit mindestens einem Versuch
                                List<Boulder> triedBoulders =
                                        filteredBoulders.stream()
                                                .filter(boulder ->
                                                        hasBeenTried(boulder, context))
                                                .toList();

                                // Getoppte Boulder
                                List<Boulder> toppedBoulders =
                                        filteredBoulders.stream()
                                                .filter(boulder ->
                                                        Boolean.TRUE.equals(
                                                                boulder.getFirstAscent()))
                                                .toList();

                                // Offene Projekte:
                                // nicht getoppt + mindestens ein Versuch
                                List<Boulder> openProjects =
                                        filteredBoulders.stream()
                                                .filter(boulder ->
                                                        !Boolean.TRUE.equals(
                                                                boulder.getFirstAscent()))
                                                .filter(boulder ->
                                                        hasBeenTried(boulder, context))
                                                .toList();

                                // Nicht getoppte Boulder mit mindestens einer Zone
                                List<Boulder> zoneBoulders =
                                        filteredBoulders.stream()
                                                .filter(boulder ->
                                                        !Boolean.TRUE.equals(
                                                                boulder.getFirstAscent()))
                                                .filter(boulder ->
                                                        hasZone(boulder, context))
                                                .toList();

                                long amountOfTriedBoulders = triedBoulders.size();

                                long amountOfTops = toppedBoulders.size();

                                long amountOfOpenProjects = openProjects.size();

                                long amountOfFlashes =
                                        filteredBoulders.stream()
                                                .filter(boulder ->
                                                        Boolean.TRUE.equals(
                                                                boulder.getFlashed()))
                                                .count();

                                long amountOfZones = zoneBoulders.size();

                                double successRate =
                                        amountOfTriedBoulders == 0
                                                ? 0.0
                                                : (double) amountOfTops
                                                / amountOfTriedBoulders
                                                * 100;

                                double flashRate =
                                        amountOfTriedBoulders == 0
                                                ? 0.0
                                                : (double) amountOfFlashes
                                                / amountOfTriedBoulders
                                                * 100;

                                double averageAttemptsUntilTop =
                                        toppedBoulders.stream()
                                                .mapToLong(boulder ->
                                                        calculateAttemptsUntilTop(
                                                                boulder,
                                                                context))
                                                .average()
                                                .orElse(0.0);

                                double averageTriesOnOpenProjects =
                                        openProjects.stream()
                                                .mapToLong(boulder ->
                                                        calculateTotalTriesForBoulder(
                                                                boulder,
                                                                context))
                                                .average()
                                                .orElse(0.0);

                                String avgGrade =
                                        calculateAverageGrade(toppedBoulders);

                                String maxGrade =
                                        calculateMaxGrade(toppedBoulders);

                                String highestZoneGrade =
                                        calculateMaxGrade(zoneBoulders);

                                String avgGradeOnOpenProjects =
                                        calculateAverageGrade(openProjects);

                                AttributeStatsResponseDTO response =
                                        new AttributeStatsResponseDTO();

                                response.setAttribute(value.name());

                                response.setAmountOfTriedBoulders(
                                        amountOfTriedBoulders);

                                response.setAmountOfTops(
                                        amountOfTops);

                                response.setOpenProjects(
                                        amountOfOpenProjects);

                                response.setAmountOfFlashes(
                                        amountOfFlashes);

                                response.setAmountOfZones(
                                        amountOfZones);

                                response.setSuccessRate(
                                        successRate);

                                response.setFlashRate(
                                        flashRate);

                                response.setAverageAttemptsUntilTop(
                                        averageAttemptsUntilTop);

                                response.setAverageTriesOnOpenProjects(
                                        averageTriesOnOpenProjects);

                                response.setAvgGrade(
                                        avgGrade);

                                response.setMaxGrade(
                                        maxGrade);

                                response.setHighestProjectZoneGrade(
                                        highestZoneGrade);

                                response.setAvgGradeOnOpenProjects(
                                        avgGradeOnOpenProjects);

                                return response;
                        })
                        .toList();
        }

        //Hilfsmethoden für calculateAttributeProgress
        private boolean hasZone( Boulder boulder, ProgressContext context) {

                return context.getAttemptsByBoulder()
                        .getOrDefault(boulder, List.of())
                        .stream()
                        .anyMatch(Attempts::isZone);
        }

        private String calculateAverageGrade(List<Boulder> boulders) {

                OptionalDouble average =
                        boulders.stream()
                                .map(Boulder::getGrade)
                                .mapToInt(this::getGradeValue)
                                .average();

                if (average.isEmpty()) {
                        return null;
                }

                return getGradeFromValue(average.getAsDouble());
        }

        private String calculateMaxGrade(List<Boulder> boulders) {

                return boulders.stream()
                        .map(Boulder::getGrade)
                        .max(Comparator.comparingInt(this::getGradeValue))
                        .map(Grade::getDisplayName)
                        .orElse(null);
        }

        // Flexibler Filter für die Suche nach einer bestimmten Kombination von Boulder-Eigenschaften inklusive optionalem Grade-Bereich
        public FilteredProgressDTO getProgressByFilters(
                User user,
                GripType gripType,
                WallAngle wallAngle,
                ClimbingStyle climbingStyle,
                RouteCharacter routeCharacter,
                Grade minGrade,
                Grade ceilingGrade) {

                // Ungültigen Grade-Bereich abfangen
                if (minGrade != null
                        && ceilingGrade != null
                        && compareGrades(minGrade, ceilingGrade) > 0) {

                        throw new IllegalArgumentException(
                                "Der minimale Grade darf nicht größer als der maximale Grade sein."
                        );
                }

                ProgressContext context = createProgressContext(user);

                List<Boulder> matchingBoulders = context.getBoulders()
                        .stream()

                        // Grip Type
                        .filter(boulder ->
                                gripType == null
                                || boulder.getGripType() == gripType)

                        // Wall Angle
                        .filter(boulder ->
                                wallAngle == null
                                || boulder.getWallAngle() == wallAngle)

                        // Climbing Style
                        .filter(boulder ->
                                climbingStyle == null
                                || boulder.getClimbingStyle() == climbingStyle)

                        // Route Character
                        .filter(boulder ->
                                routeCharacter == null
                                || boulder.getRouteCharacter() == routeCharacter)

                        // Untere Grade-Grenze
                        .filter(boulder ->
                                minGrade == null
                                || compareGrades(boulder.getGrade(), minGrade) >= 0)

                        // Obere Grade-Grenze
                        .filter(boulder ->
                                ceilingGrade == null
                                || compareGrades(boulder.getGrade(), ceilingGrade) <= 0)

                        .toList();

                // Boulder mit mindestens einem Versuch
                List<Boulder> triedBoulders = matchingBoulders.stream()
                        .filter(boulder ->
                                hasBeenTried(boulder, context))
                        .toList();

                // Getoppte Boulder
                List<Boulder> toppedBoulders = matchingBoulders.stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .toList();

                // Offene Projekte:
                // mindestens ein Versuch + noch nicht getoppt
                List<Boulder> openProjects = matchingBoulders.stream()
                        .filter(boulder ->
                                !Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .filter(boulder ->
                                hasBeenTried(boulder, context))
                        .toList();

                // Offene Projekte mit mindestens einer Zone
                List<Boulder> zoneBoulders = matchingBoulders.stream()
                        .filter(boulder ->
                                !Boolean.TRUE.equals(boulder.getFirstAscent()))
                        .filter(boulder ->
                                hasZone(boulder, context))
                        .toList();

                long amountOfTriedBoulders =
                        triedBoulders.size();

                long amountOfTops =
                        toppedBoulders.size();

                long amountOfOpenProjects =
                        openProjects.size();

                long amountOfFlashes =
                        matchingBoulders.stream()
                                .filter(boulder ->
                                        Boolean.TRUE.equals(boulder.getFlashed()))
                                .count();

                long amountOfZones =
                        zoneBoulders.size();

                double successRate =
                        amountOfTriedBoulders == 0
                                ? 0.0
                                : (double) amountOfTops
                                / amountOfTriedBoulders
                                * 100;

                double flashRate =
                        amountOfTriedBoulders == 0
                                ? 0.0
                                : (double) amountOfFlashes
                                / amountOfTriedBoulders
                                * 100;

                double averageAttemptsUntilTop =
                        toppedBoulders.stream()
                                .mapToLong(boulder ->
                                        calculateAttemptsUntilTop(
                                                boulder,
                                                context))
                                .average()
                                .orElse(0.0);

                double averageTriesOnOpenProjects =
                        openProjects.stream()
                                .mapToLong(boulder ->
                                        calculateTotalTriesForBoulder(
                                                boulder,
                                                context))
                                .average()
                                .orElse(0.0);

                // Durchschnittsgrad der getoppten Boulder
                String avgGrade =
                        calculateAverageGrade(toppedBoulders);

                // Höchster getoppter Boulder
                String maxGrade = calculateMaxGrade(toppedBoulders);

                // Höchster Grad eines offenen Projekts mit Zone
                String highestProjectZoneGrade = calculateMaxGrade(zoneBoulders);

                // Durchschnittsgrad der offenen Projekte
                String avgGradeOnOpenProjects =
                        calculateAverageGrade(openProjects);

                FilteredProgressDTO response =
                        new FilteredProgressDTO();

                response.setAmountOfTriedBoulders(
                        amountOfTriedBoulders);

                response.setAmountOfTops(
                        amountOfTops);

                response.setOpenProjects(
                        amountOfOpenProjects);

                response.setAmountOfFlashes(
                        amountOfFlashes);

                response.setAmountOfZones(
                        amountOfZones);

                response.setSuccessRate(
                        successRate);

                response.setFlashRate(
                        flashRate);

                response.setAverageAttemptsUntilTop(
                        averageAttemptsUntilTop);

                response.setAverageTriesOnOpenProjects(
                        averageTriesOnOpenProjects);

                response.setAvgGrade(
                        avgGrade);

                response.setMaxGrade(
                        maxGrade);

                response.setHighestProjectZoneGrade(
                        highestProjectZoneGrade);

                response.setAvgGradeOnOpenProjects(
                        avgGradeOnOpenProjects);

                return response;
        }

        // Frontend Erweiterung:  Vorschau der Anzahl der zutreffenden Boulder für weitere Filter-Option
        public ProgressFilterCountResponseDTO getFilterOptionCounts(
                User user,
                GripType selectedGripType,
                WallAngle selectedWallAngle,
                ClimbingStyle selectedClimbingStyle,
                RouteCharacter selectedRouteCharacter,
                Grade minGrade,
                Grade maxGrade) {

                if (minGrade != null
                        && maxGrade != null
                        && compareGrades(minGrade, maxGrade) > 0) {

                        throw new IllegalArgumentException(
                                "Der minimale Grade darf nicht größer als der maximale Grade sein."
                        );
                }

                ProgressContext context = createProgressContext(user);

                /*
                * ============================================================
                * NUR TRIED BOULDERS
                * ============================================================
                *
                * Ein Boulder ohne Attempt darf in den Filter-Counts
                * nicht auftauchen.
                */
                List<Boulder> triedBoulders = context.getBoulders()
                        .stream()
                        .filter(boulder -> hasBeenTried(boulder, context))
                        .toList();

                ProgressFilterCountResponseDTO response = new ProgressFilterCountResponseDTO();

                /*
                * ============================================================
                * GRIP TYPES
                * ============================================================
                *
                * Der aktuell ausgewählte GripType wird ignoriert.
                * Alle anderen aktiven Filter bleiben bestehen.
                */
                List<ProgressFilterCountDTO> gripTypeCounts =
                        Arrays.stream(GripType.values())
                                .map(gripType -> {

                                        long count = triedBoulders.stream()
                                                .filter(boulder ->
                                                        boulder.getGripType() == gripType)

                                                .filter(boulder ->
                                                        selectedWallAngle == null
                                                        || boulder.getWallAngle() == selectedWallAngle)

                                                .filter(boulder ->
                                                        selectedClimbingStyle == null
                                                        || boulder.getClimbingStyle() == selectedClimbingStyle)

                                                .filter(boulder ->
                                                        selectedRouteCharacter == null
                                                        || boulder.getRouteCharacter() == selectedRouteCharacter)

                                                .filter(boulder ->
                                                        minGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                minGrade) >= 0)

                                                .filter(boulder ->
                                                        maxGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                maxGrade) <= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                gripType.name(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * WALL ANGLES
                * ============================================================
                */
                List<ProgressFilterCountDTO> wallAngleCounts =
                        Arrays.stream(WallAngle.values())
                                .map(wallAngle -> {

                                        long count = triedBoulders.stream()
                                                .filter(boulder ->
                                                        boulder.getWallAngle() == wallAngle)

                                                .filter(boulder ->
                                                        selectedGripType == null
                                                        || boulder.getGripType() == selectedGripType)

                                                .filter(boulder ->
                                                        selectedClimbingStyle == null
                                                        || boulder.getClimbingStyle() == selectedClimbingStyle)

                                                .filter(boulder ->
                                                        selectedRouteCharacter == null
                                                        || boulder.getRouteCharacter() == selectedRouteCharacter)

                                                .filter(boulder ->
                                                        minGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                minGrade) >= 0)

                                                .filter(boulder ->
                                                        maxGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                maxGrade) <= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                wallAngle.name(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * CLIMBING STYLES
                * ============================================================
                */
                List<ProgressFilterCountDTO> climbingStyleCounts =
                        Arrays.stream(ClimbingStyle.values())
                                .map(climbingStyle -> {

                                        long count = triedBoulders.stream()
                                                .filter(boulder ->
                                                        boulder.getClimbingStyle() == climbingStyle)

                                                .filter(boulder ->
                                                        selectedGripType == null
                                                        || boulder.getGripType() == selectedGripType)

                                                .filter(boulder ->
                                                        selectedWallAngle == null
                                                        || boulder.getWallAngle() == selectedWallAngle)

                                                .filter(boulder ->
                                                        selectedRouteCharacter == null
                                                        || boulder.getRouteCharacter() == selectedRouteCharacter)

                                                .filter(boulder ->
                                                        minGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                minGrade) >= 0)

                                                .filter(boulder ->
                                                        maxGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                maxGrade) <= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                climbingStyle.name(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * ROUTE CHARACTERS
                * ============================================================
                */
                List<ProgressFilterCountDTO> routeCharacterCounts =
                        Arrays.stream(RouteCharacter.values())
                                .map(routeCharacter -> {

                                        long count = triedBoulders.stream()
                                                .filter(boulder ->
                                                        boulder.getRouteCharacter() == routeCharacter)

                                                .filter(boulder ->
                                                        selectedGripType == null
                                                        || boulder.getGripType() == selectedGripType)

                                                .filter(boulder ->
                                                        selectedWallAngle == null
                                                        || boulder.getWallAngle() == selectedWallAngle)

                                                .filter(boulder ->
                                                        selectedClimbingStyle == null
                                                        || boulder.getClimbingStyle() == selectedClimbingStyle)

                                                .filter(boulder ->
                                                        minGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                minGrade) >= 0)

                                                .filter(boulder ->
                                                        maxGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                maxGrade) <= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                routeCharacter.name(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * MIN GRADES
                * ============================================================
                *
                * Für jeden möglichen MinGrade:
                *
                *     Boulder Grade >= ausgewählter Grade
                *
                * Bereits gesetzter maxGrade wird berücksichtigt.
                *
                * Beispiel:
                *
                * MinGrade: 6a
                * MaxGrade: 7b
                *
                * -> für MinGrade 6a zählen nur Boulder
                *    zwischen 6a und 7b.
                */
                List<ProgressFilterCountDTO> minGradeCounts =
                        Arrays.stream(Grade.values())
                                .map(grade -> {

                                        long count = triedBoulders.stream()

                                                .filter(boulder ->
                                                        selectedGripType == null
                                                        || boulder.getGripType() == selectedGripType)

                                                .filter(boulder ->
                                                        selectedWallAngle == null
                                                        || boulder.getWallAngle() == selectedWallAngle)

                                                .filter(boulder ->
                                                        selectedClimbingStyle == null
                                                        || boulder.getClimbingStyle() == selectedClimbingStyle)

                                                .filter(boulder ->
                                                        selectedRouteCharacter == null
                                                        || boulder.getRouteCharacter() == selectedRouteCharacter)

                                                /*
                                                * MinGrade-Bedingung:
                                                *
                                                * Boulder müssen mindestens
                                                * den dargestellten Grade haben.
                                                */
                                                .filter(boulder ->
                                                        compareGrades(
                                                                boulder.getGrade(),
                                                                grade) >= 0)

                                                /*
                                                * Falls bereits ein MaxGrade
                                                * ausgewählt wurde, muss dieser
                                                * ebenfalls eingehalten werden.
                                                */
                                                .filter(boulder ->
                                                        maxGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                maxGrade) <= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                grade.getDisplayName(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * MAX GRADES
                * ============================================================
                *
                * Für jeden möglichen MaxGrade:
                *
                *     Boulder Grade <= ausgewählter Grade
                *
                * Bereits gesetzter minGrade wird berücksichtigt.
                *
                * Beispiel:
                *
                * MinGrade: 6a
                * MaxGrade: 7b
                *
                * -> für MaxGrade 7b zählen nur Boulder
                *    zwischen 6a und 7b.
                */
                List<ProgressFilterCountDTO> maxGradeCounts =
                        Arrays.stream(Grade.values())
                                .map(grade -> {

                                        long count = triedBoulders.stream()

                                                .filter(boulder ->
                                                        selectedGripType == null
                                                        || boulder.getGripType() == selectedGripType)

                                                .filter(boulder ->
                                                        selectedWallAngle == null
                                                        || boulder.getWallAngle() == selectedWallAngle)

                                                .filter(boulder ->
                                                        selectedClimbingStyle == null
                                                        || boulder.getClimbingStyle() == selectedClimbingStyle)

                                                .filter(boulder ->
                                                        selectedRouteCharacter == null
                                                        || boulder.getRouteCharacter() == selectedRouteCharacter)

                                                /*
                                                * MaxGrade-Bedingung:
                                                *
                                                * Boulder dürfen maximal
                                                * den dargestellten Grade haben.
                                                */
                                                .filter(boulder ->
                                                        compareGrades(
                                                                boulder.getGrade(),
                                                                grade) <= 0)

                                                /*
                                                * Falls bereits ein MinGrade
                                                * ausgewählt wurde, muss dieser
                                                * ebenfalls eingehalten werden.
                                                */
                                                .filter(boulder ->
                                                        minGrade == null
                                                        || compareGrades(
                                                                boulder.getGrade(),
                                                                minGrade) >= 0)

                                                .count();

                                        return new ProgressFilterCountDTO(
                                                grade.getDisplayName(),
                                                count
                                        );
                                })
                                .toList();

                /*
                * ============================================================
                * RESPONSE
                * ============================================================
                */
                response.setGripTypes(gripTypeCounts);
                response.setWallAngles(wallAngleCounts);
                response.setClimbingStyles(climbingStyleCounts);
                response.setRouteCharacters(routeCharacterCounts);
                response.setMinGrades(minGradeCounts);
                response.setMaxGrades(maxGradeCounts);

                return response;
        }

        //Vergleichs Analyse
        public StrengthWeaknessResponseDTO getStrengthsAndWeaknesses(User user) {

                ProgressContext context = createProgressContext(user);
                List<StrengthWeaknessDTO> strengths = new ArrayList<>();
                List<StrengthWeaknessDTO> weaknesses = new ArrayList<>();
                double maxGradeUser = calculateMaxGradeValue(context);

                for (BoulderAttribute attributeType : BoulderAttribute.values()) {

                        List<AttributeStatsResponseDTO> stats = getProgressByAttributeSW(context,attributeType);

                        List<CompareAttributeDTO> compareStats = stats.stream()
                                        .map(stat ->
                                                createCompareAttributeDTO(
                                                        context,
                                                        stat,
                                                        attributeType))
                                        .toList();

                        compareAttributes(
                                compareStats,
                                attributeType,
                                strengths,
                                weaknesses,
                                maxGradeUser);
                }

                StrengthWeaknessResponseDTO response = new StrengthWeaknessResponseDTO();
                response.setStrengths(strengths);
                response.setWeaknesses(weaknesses);

                return response;
        }

        private void compareAttributes(List<CompareAttributeDTO> stats, BoulderAttribute attributeType, List<StrengthWeaknessDTO> strengths, List<StrengthWeaknessDTO> weaknesses, double maxGradeUser) {

                final int MINIMUM_TRIED_BOULDERS = 5;
                final double THRESHOLD = 5.0;
                List<CompareAttributeDTO> validStats = stats.stream()
                                .filter(stat ->
                                        stat.getAmountOfTriedBoulders()
                                                >= MINIMUM_TRIED_BOULDERS)
                                .toList();
                if (validStats.size() < 2) {
                        return;
                }

                double averageScore = validStats.stream()
                                .mapToDouble(stat -> calculateCompositeScore(stat, maxGradeUser))
                                .average()
                                .orElse(0.0);
                for (CompareAttributeDTO stat : validStats) {
                        double compositeScore = calculateCompositeScore(stat, maxGradeUser);
                        if (compositeScore >= averageScore + THRESHOLD) {
                                strengths.add(createStrengthWeaknessDTO(stat, attributeType, compositeScore));
                        } else if (compositeScore <= averageScore - THRESHOLD) {
                                weaknesses.add(createStrengthWeaknessDTO(stat, attributeType, compositeScore));
                        }
                }
        }

        private StrengthWeaknessDTO createStrengthWeaknessDTO(CompareAttributeDTO stat, BoulderAttribute attributeType, double compositeScore) {
                StrengthWeaknessDTO response = new StrengthWeaknessDTO();
                response.setAttributeType(attributeType);
                response.setAttribute(stat.getAttribute());
                response.setCompositeScore(compositeScore);
                return response;
        }

        private double calculateCompositeScore(CompareAttributeDTO stat, double maxGradeUser) {
                double successFactor = calculateSuccessFactor(stat, maxGradeUser);
                double efficiencyFactor = calculateEfficiencyFactor(stat);
                double projectFactor = calculateProjectFactor(stat);
                return successFactor * 0.55 + efficiencyFactor * 0.40 + projectFactor * 0.05;
        }

        //(SuccessRate * (Gavg_top/Gmax)*0,7) + (flashRate * (Gavg_flash/Gmax)*0,3)
        private double calculateSuccessFactor(CompareAttributeDTO stat, double gradeMaxOverall){
                
                if (gradeMaxOverall <= 0) {
                        return 0.0;
                }
                double gradeAverageTopForAttribute = stat.getGradeAvgTop();
                double gradeAverageFlashForAttribute = stat.getGradeAvgFlash();
                double successFactor = (stat.getSuccessRate() * (gradeAverageTopForAttribute/gradeMaxOverall) * 0.7 ) + (stat.getFlashRate() * (gradeAverageFlashForAttribute/gradeMaxOverall) * 0.3);
                
                return successFactor;
        }

        private int calculateMaxGradeValue(ProgressContext context) {

        return context.getBoulders()
                .stream()
                .filter(boulder ->
                        Boolean.TRUE.equals(
                                boulder.getFirstAscent()))
                .map(Boulder::getGrade)
                .mapToInt(this::getGradeValue)
                .max()
                .orElse(0);
        }

        private CompareAttributeDTO createCompareAttributeDTO(ProgressContext context, AttributeStatsResponseDTO stat, BoulderAttribute attributeType) {

                CompareAttributeDTO response = new CompareAttributeDTO();
                response.setAttribute(stat.getAttribute());
                response.setAmountOfTriedBoulders(stat.getAmountOfTriedBoulders());
                response.setAmountOfTops(stat.getAmountOfTops());
                response.setSuccessRate(stat.getSuccessRate());
                response.setFlashRate(stat.getFlashRate());
                response.setAverageAttemptsUntilTop(stat.getAverageAttemptsUntilTop());
                response.setAverageTriesOnOpenProjects(stat.getAverageTriesOnOpenProjects());

                response.setGradeAvgTop(calculateGradeAverageTopForAttribute(context, attributeType, stat.getAttribute()));
                response.setGradeAvgFlash(calculateGradeAverageFlashForAttribute(context, attributeType, stat.getAttribute()));

                return response;
        }

        private double calculateGradeAverageFlashForAttribute(ProgressContext context, BoulderAttribute attributeType, String attributeValue) {

                List<Boulder> boulders = context.getTriedBoulders();

                return switch (attributeType) {
                        case GRIP_TYPE ->
                                calculateAverageFlashGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getGripType().name()
                                );
                        case WALL_ANGLE ->
                                calculateAverageFlashGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getWallAngle().name()
                                );
                        case CLIMBING_STYLE ->
                                calculateAverageFlashGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getClimbingStyle().name()
                                );
                        case ROUTE_CHARACTER ->
                                calculateAverageFlashGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getRouteCharacter().name()
                                );
                };
        }

        private double calculateAverageFlashGradeForAttribute(List<Boulder> boulders, String attributeValue, Function<Boulder, String> attributeGetter) {

                return boulders.stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(
                                        boulder.getFlashed()))
                        .filter(boulder ->
                                attributeGetter
                                        .apply(boulder)
                                        .equals(attributeValue))
                        .map(Boulder::getGrade)
                        .mapToInt(this::getGradeValue)
                        .average()
                        .orElse(0.0);
        }

        private double calculateGradeAverageTopForAttribute(ProgressContext context, BoulderAttribute attributeType, String attributeValue) {
        List<Boulder> boulders = context.getTriedBoulders();

                return switch (attributeType) {
                        case GRIP_TYPE ->
                                calculateAverageGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getGripType().name()
                                );
                        case WALL_ANGLE ->
                                calculateAverageGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getWallAngle().name()
                                );
                        case CLIMBING_STYLE ->
                                calculateAverageGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getClimbingStyle().name()
                                );
                        case ROUTE_CHARACTER ->
                                calculateAverageGradeForAttribute(
                                        boulders,
                                        attributeValue,
                                        boulder ->
                                                boulder.getRouteCharacter().name()
                                );
                };
        }

        private double calculateAverageGradeForAttribute(List<Boulder> boulders, String attributeValue, Function<Boulder, String> attributeGetter) {

                return boulders.stream()
                        .filter(boulder ->
                                Boolean.TRUE.equals(
                                        boulder.getFirstAscent()))
                        .filter(boulder ->
                                attributeGetter
                                        .apply(boulder)
                                        .equals(attributeValue))
                        .map(Boulder::getGrade)
                        .mapToInt(this::getGradeValue)
                        .average()
                        .orElse(0.0);
        }

        //100 * (1 - log(avgAttempts) / log(upperLimit))
        private double calculateEfficiencyFactor(CompareAttributeDTO stat){
                double upperLimit = 50;
                if (stat.getAverageAttemptsUntilTop() <= 0) {
                        return 0.0;
                }

                double efficiencyFactor = 100 * (1 - (Math.log(stat.getAverageAttemptsUntilTop()) / Math.log(upperLimit)));
                return Math.max(0.0, efficiencyFactor);
        }

        //min(100, (avgTriesOnOpenProjects/upperLimit)*100)
        private double calculateProjectFactor(CompareAttributeDTO stat){
                double upperLimit = 50;
                
                if (stat.getAverageTriesOnOpenProjects() <= 0) {
                        return 0.0;
                }

                double projectFactor = Math.min(100.0, (stat.getAverageTriesOnOpenProjects()/upperLimit) * 100);
                return projectFactor;
        }

        //Strength&Weaknesses für eine Bestimmte Session. Wird für SessionStatsInsightService genutzt.
        public StrengthWeaknessResponseDTO getStrengthsAndWeaknessesBefore(User user, LocalDate date) {

                ProgressContext context = createProgressContext(user, date);

                List<StrengthWeaknessDTO> strengths = new ArrayList<>();
                List<StrengthWeaknessDTO> weaknesses = new ArrayList<>();

                double maxGradeUser = calculateMaxGradeValue(context);

                for (BoulderAttribute attributeType : BoulderAttribute.values()) {

                        List<AttributeStatsResponseDTO> stats =
                                getProgressByAttributeSW(context, attributeType);

                        List<CompareAttributeDTO> compareStats = stats.stream()
                                .map(stat -> createCompareAttributeDTO(
                                        context,
                                        stat,
                                        attributeType))
                                .toList();

                        compareAttributes(
                                compareStats,
                                attributeType,
                                strengths,
                                weaknesses,
                                maxGradeUser);
                }

                StrengthWeaknessResponseDTO response =
                        new StrengthWeaknessResponseDTO();

                response.setStrengths(strengths);
                response.setWeaknesses(weaknesses);

                return response;
        }

        private static class ProgressContext {

                private final List<Boulder> boulders;
                private final List<Boulder> triedBoulders;
                private final Map<Boulder, List<Attempts>> attemptsByBoulder;

                public ProgressContext(
                        List<Boulder> boulders,
                        List<Boulder> triedBoulders,
                        Map<Boulder, List<Attempts>> attemptsByBoulder) {

                        this.boulders = boulders;
                        this.triedBoulders = triedBoulders;
                        this.attemptsByBoulder = attemptsByBoulder;
                }

                public List<Boulder> getBoulders() {
                        return boulders;
                }

                public List<Boulder> getTriedBoulders() {
                        return triedBoulders;
                }

                public Map<Boulder, List<Attempts>> getAttemptsByBoulder() {
                        return attemptsByBoulder;
                }
        }

        private ProgressContext createProgressContext(User user) {

                List<Boulder> boulders =
                        boulderRepository.findByUser(user);

                List<Attempts> attempts =
                        attemptsRepository.findByBoulderIn(boulders);

                Map<Boulder, List<Attempts>> attemptsByBoulder =
                        attempts.stream()
                                .collect(Collectors.groupingBy(Attempts::getBoulder));

                List<Boulder> triedBoulders =
                        boulders.stream()
                                .filter(boulder ->
                                        attemptsByBoulder
                                                .getOrDefault(boulder, List.of())
                                                .stream()
                                                .anyMatch(attempt ->
                                                        attempt.getTries() >= 1))
                                .toList();

                return new ProgressContext(
                        boulders,
                        triedBoulders,
                        attemptsByBoulder
                );
        }

        private ProgressContext createProgressContext(User user, LocalDate beforeDate) {

                List<Boulder> boulders = boulderRepository.findByUser(user);

                List<Attempts> attempts = attemptsRepository.findByBoulderIn(boulders)
                        .stream()
                        .filter(attempt ->
                                attempt.getSession() != null
                                && attempt.getSession().getDate() != null
                                && attempt.getSession().getDate().isBefore(beforeDate))
                        .toList();

                Map<Boulder, List<Attempts>> attemptsByBoulder =
                        attempts.stream()
                                .collect(Collectors.groupingBy(Attempts::getBoulder));

                List<Boulder> triedBoulders = boulders.stream()
                        .filter(boulder ->
                                attemptsByBoulder.getOrDefault(boulder, List.of())
                                        .stream()
                                        .anyMatch(attempt -> attempt.getTries() >= 1))
                        .toList();

                return new ProgressContext(
                        boulders,
                        triedBoulders,
                        attemptsByBoulder
                );
        }

        //HilfsMethode
        private boolean hasBeenTried(Boulder boulder, ProgressContext context) {

                return context.getAttemptsByBoulder()
                        .getOrDefault(boulder, List.of())
                        .stream()
                        .anyMatch(attempt ->
                                attempt.getTries() >= 1);
        }
}