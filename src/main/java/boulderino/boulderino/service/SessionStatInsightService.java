package boulderino.boulderino.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import boulderino.boulderino.dto.BoulderAttributeInsightDTO;
import boulderino.boulderino.dto.BoulderInsightDTO;
import boulderino.boulderino.dto.FormerWeaknessTopDTO;
import boulderino.boulderino.dto.GradeDistributionInsightDTO;
import boulderino.boulderino.dto.HighlightResponseDTO;
import boulderino.boulderino.dto.NewHighlightDTO;
import boulderino.boulderino.dto.SessionAttemptInsightDTO;
import boulderino.boulderino.dto.SessionInsightsResponseDTO;
import boulderino.boulderino.dto.SessionPerformanceInsightDTO;
import boulderino.boulderino.dto.SessionStatsDTO;
import boulderino.boulderino.dto.StrengthWeaknessDTO;
import boulderino.boulderino.dto.StrengthWeaknessResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.BoulderAttribute;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.HighlightType;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.SessionRepository;

@Service
public class SessionStatInsightService {
    private final SessionRepository sessionRepository;
    private final AttemptsRepository attemptsRepository;
    private final StatsService statsService;
    private final HighlightsService highlightsService;
    private final ProgressService progressService;

    public SessionStatInsightService(SessionRepository sessionRepository, AttemptsRepository attemptsRepository, StatsService statsService, HighlightsService highlightsService, ProgressService progressService) {
        this.sessionRepository = sessionRepository;
        this.attemptsRepository = attemptsRepository;
        this.statsService = statsService;
        this.highlightsService = highlightsService;
        this.progressService = progressService;
    }

    //zentrale Methode zum zusammenfassen der Berrechnungen für den Controller
    public SessionInsightsResponseDTO getSessionInsights(Session session, User user) {

        SessionInsightsResponseDTO response = new SessionInsightsResponseDTO();
        response.setSessionId(session.getId());

        // Effiziente Sends
        response.setMostEfficientSend(
                getMostEfficientSend(session)
                        .map(this::toAttemptInsightDTO)
                        .orElse(null)
        );

        response.setEfficientSends(
                getEfficientSends(session).stream()
                        .map(this::toAttemptInsightDTO)
                        .toList()
        );

        // Schwierigstes offenes Projekt
        response.setMostDifficultProject(
                getMostDifficultProject(session)
                        .map(this::toAttemptInsightDTO)
                        .orElse(null)
        );

        // Routen im hohen Gradbereich
        response.setHighGradeRoutes(
                getHighGradeRoutes(session, user).stream()
                        .map(this::toAttemptInsightDTO)
                        .toList()
        );

        // Performance-Vergleich
        response.setPerformanceInsights(
                getPerformanceInsights(session, user)
        );

        // Attribute bei offenen Projekten
        response.setUntoppedAttributeInsights(
                getUntoppedAttributeInsights(session)
        );

        // Gradverteilung
        response.setGradeDistributionInsights(
                getGradeDistributionInsights(session)
        );

        // Neue Highlights
        response.setNewHighlights(
                getNewHighlightInsights(session, user)
        );

        // Tops auf früheren Schwächen
        response.setFormerWeaknessTops(
                getTopsWithFormerWeaknesses(session, user)
        );

        return response;
    }

    // Der effizenteste Send der Session (bei gleichstand der Tries entscheided die schwierigere Grade Stufe)
    public Optional<Attempts> getMostEfficientSend(Session session) {

        List<Attempts> attempts = attemptsRepository.findBySession(session);

        return attempts.stream()
                .filter(Attempts::isDone)
                .filter(attempt -> attempt.getBoulder() != null)
                .min(
                        Comparator
                                .comparingInt(Attempts::getTries)
                                .thenComparing(
                                        (a1, a2) -> Integer.compare(
                                                this.getGradeValue(
                                                        a2.getBoulder().getGrade()
                                                ),
                                                this.getGradeValue(
                                                        a1.getBoulder().getGrade()
                                                )
                                        )
                                )
                );
    }

    // Ein effizenter Send hat nur 3 oder weniger Tries benötigt
    public List<Attempts> getEfficientSends(Session session) {

        List<Attempts> attempts = attemptsRepository.findBySession(session);

        return attempts.stream()
                .filter(Attempts::isDone)
                .filter(attempt -> attempt.getTries() <= 3)
                .filter(attempt -> attempt.getBoulder() != null)
                .sorted(
                        Comparator
                                .comparingInt(Attempts::getTries)
                                .thenComparing(
                                        (a1, a2) -> Integer.compare(
                                                this.getGradeValue(
                                                        a2.getBoulder().getGrade()
                                                ),
                                                this.getGradeValue(
                                                        a1.getBoulder().getGrade()
                                                )
                                        )
                                )
                )
                .toList();
    }

    // Gibt das ungetoppte Projekt mit den meisten Versuchen einer Session zurück
    public Optional<Attempts> getMostDifficultProject(Session session) {

        List<Attempts> attempts = attemptsRepository.findBySession(session);

        return attempts.stream()
                .filter(attempt -> !attempt.isDone())
                .filter(attempt -> attempt.getTries() > 0)
                .filter(attempt -> attempt.getBoulder() != null)
                .filter(attempt -> attempt.getBoulder().getGrade() != null)
                .sorted(
                        Comparator
                                .comparingInt(Attempts::getTries)
                                .reversed()
                                .thenComparing(
                                        (a1, a2) -> Integer.compare(
                                                this.getGradeValue(
                                                        a2.getBoulder().getGrade()
                                                ),
                                                this.getGradeValue(
                                                        a1.getBoulder().getGrade()
                                                )
                                        )
                                )
                )
                .findFirst();
    }

    // Liste von Routen innerhalb der Session die am maxGrade - 2 Berreich sind
    public List<Attempts> getHighGradeRoutes(Session session, User user) {

        // Alle bisherigen Attempts des Users laden
        List<Attempts> userAttempts = attemptsRepository.findBySessionUser(user);

        // Höchsten jemals getoppten Grad ermitteln
        Optional<Grade> maxGrade = userAttempts.stream()
                .filter(Attempts::isDone)
                .filter(attempt -> attempt.getBoulder() != null)
                .filter(attempt -> attempt.getBoulder().getGrade() != null)
                .map(attempt -> attempt.getBoulder().getGrade())
                .max(Comparator.comparingInt(this::getGradeValue));

        // Ohne bisherigen Top gibt es keinen persönlichen Maximalgrad
        if (maxGrade.isEmpty()) {
            return List.of();
        }

        int maxGradeValue = this.getGradeValue(maxGrade.get());
        int minGradeValue = maxGradeValue - 2;

        // Nur Routen der aktuellen Session zurückgeben
        List<Attempts> sessionAttempts = attemptsRepository.findBySession(session);

        return sessionAttempts.stream()
                .filter(attempt -> attempt.getTries() > 0)
                .filter(attempt -> attempt.getBoulder() != null)
                .filter(attempt -> attempt.getBoulder().getGrade() != null)
                .filter(attempt ->
                        this.getGradeValue(attempt.getBoulder().getGrade())
                                >= minGradeValue
                )
                .sorted(
                        Comparator.comparingInt(
                                (Attempts attempt) -> this.getGradeValue(
                                        attempt.getBoulder().getGrade()
                                )
                        ).reversed()
                )
                .toList();
    }

    //Vergleich der Info Card Werte mit den letzten 10 Sessions
    public List<SessionPerformanceInsightDTO> getPerformanceInsights(Session session, User user) {

        // Kennzahlen der aktuellen Session
        SessionStatsDTO current = statsService.getSessionStats(session.getId(), user);

        // Keine Insights für Sessions ohne gekletterte Boulder
        if (current.getSessionBoulders() == 0) {
            return List.of();
        }

        // Historische Sessions laden
        List<Session> previousSessions =
                sessionRepository.findByUserOrderByDateDescIdDesc(user);

        List<SessionStatsDTO> historicalMetrics = new ArrayList<>();

        // Die letzten 10 Sessions mit gekletterten Bouldern berücksichtigen
        for (Session previousSession : previousSessions) {

            // Aktuelle Session überspringen
            if (Objects.equals(previousSession.getId(), session.getId())) {
                continue;
            }

            SessionStatsDTO metrics = statsService.getSessionStats(previousSession.getId(),user);

            // Leere Sessions überspringen
            if (metrics.getSessionBoulders() == 0) {
                continue;
            }

            historicalMetrics.add(metrics);

            if (historicalMetrics.size() >= 10) {
                break;
            }
        }

        // Mindestens 3 historische Sessions benötigen wir für einen Vergleich
        if (historicalMetrics.size() < 3) {
            return List.of();
        }

        // Mediane der einzelnen Kennzahlen berechnen
        double medianSuccessRate = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> m.getSuccessRate())
                        .toList()
        );

        double medianTries = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> (double) m.getSessionTries())
                        .toList()
        );

        double medianTops = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> (double) m.getSessionTops())
                        .toList()
        );

        double medianFlashes = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> (double) m.getSessionFlashes())
                        .toList()
        );

        double medianFlashRate = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> m.getFlashRate())
                        .toList()
        );

        double medianBoulders = calculateMedian(
                historicalMetrics.stream()
                        .map(m -> (double) m.getSessionBoulders())
                        .toList()
        );

        // Insights erstellen
        List<SessionPerformanceInsightDTO> insights = new ArrayList<>();

        addInsightIfImproved(
                insights,
                "SUCCESS_RATE",
                current.getSuccessRate(),
                medianSuccessRate
        );

        addInsightIfImproved(
                insights,
                "TRIES",
                current.getSessionTries(),
                medianTries
        );

        addInsightIfImproved(
                insights,
                "TOPS",
                current.getSessionTops(),
                medianTops
        );

        addInsightIfImproved(
                insights,
                "FLASHES",
                current.getSessionFlashes(),
                medianFlashes
        );

        addInsightIfImproved(
                insights,
                "FLASHRATE",
                current.getFlashRate(),
                medianFlashRate
        );

        addInsightIfImproved(
                insights,
                "DIFFERENT_BOULDERS",
                current.getSessionBoulders(),
                medianBoulders
        );

        return insights;
    }

    /**
     * Berechnet den Median einer Liste von Werten.
     */
    private double calculateMedian(List<Double> values) {

        if (values.isEmpty()) {
            return 0.0;
        }

        List<Double> sortedValues = values.stream()
                .sorted()
                .toList();

        int size = sortedValues.size();
        int middle = size / 2;

        if (size % 2 == 0) {
            return (sortedValues.get(middle - 1)
                    + sortedValues.get(middle)) / 2.0;
        }

        return sortedValues.get(middle);
    }

    /**
     * Fügt einen Insight hinzu, wenn der aktuelle Wert
     * über dem historischen Median liegt.
     *
     * Improvement entspricht der absoluten Differenz
     * zwischen aktuellem Wert und Median.
     */
    private void addInsightIfImproved(
            List<SessionPerformanceInsightDTO> insights,
            String metric,
            double currentValue,
            double medianValue
    ) {

        if (currentValue <= medianValue) {
            return;
        }

        double improvement = currentValue - medianValue;

        insights.add(new SessionPerformanceInsightDTO(
                metric,
                currentValue,
                medianValue,
                improvement
        ));
    }


    //Diese Methode vergleicht offene Projekte einer Session auf signifikante übereinstimmung auf eines Boulder-Attributs
    private static final double ATTRIBUTE_THRESHOLD = 60.0;
    private static final int MIN_BOULDERS_FOR_ATTRIBUTE_INSIGHT = 3;

    public List<BoulderAttributeInsightDTO> getUntoppedAttributeInsights(
            Session session
    ) {

        List<Attempts> attempts = attemptsRepository.findBySession(session);

        // Nur tatsächlich versuchte und nicht getoppte Boulder berücksichtigen
        List<Boulder> untoppedBoulders = attempts.stream()
                .filter(attempt -> attempt.getTries() >= 1)
                .filter(attempt -> !attempt.isDone())
                .map(Attempts::getBoulder)
                .filter(boulder -> boulder != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Boulder::getId,
                                Function.identity(),
                                (first, second) -> first
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        if (untoppedBoulders.isEmpty()) {
            return List.of();
        }

        // Für jedes Attribut die Häufigkeit seiner Werte erfassen
        Map<BoulderAttribute, Map<String, Long>> attributeCounts =
                new EnumMap<>(BoulderAttribute.class);

        // Anzahl der Boulder mit einem gesetzten Wert je Attribut
        Map<BoulderAttribute, Long> attributeTotals =
                new EnumMap<>(BoulderAttribute.class);

        // Attributwerte auswerten
        analyzeAttribute(
                BoulderAttribute.GRIP_TYPE,
                untoppedBoulders,
                b -> b.getGripType() != null ? b.getGripType().name() : null,
                attributeCounts,
                attributeTotals
        );

        analyzeAttribute(
                BoulderAttribute.WALL_ANGLE,
                untoppedBoulders,
                b -> b.getWallAngle() != null ? b.getWallAngle().name() : null,
                attributeCounts,
                attributeTotals
        );

        analyzeAttribute(
                BoulderAttribute.CLIMBING_STYLE,
                untoppedBoulders,
                b -> b.getClimbingStyle() != null ? b.getClimbingStyle().name() : null,
                attributeCounts,
                attributeTotals
        );

        analyzeAttribute(
                BoulderAttribute.ROUTE_CHARACTER,
                untoppedBoulders,
                b -> b.getRouteCharacter() != null ? b.getRouteCharacter().name() : null,
                attributeCounts,
                attributeTotals
        );

        List<BoulderAttributeInsightDTO> insights = new ArrayList<>();

        for (BoulderAttribute attribute : BoulderAttribute.values()) {

            long total = attributeTotals.getOrDefault(attribute, 0L);

            // Zu wenige auswertbare Boulder
            if (total < MIN_BOULDERS_FOR_ATTRIBUTE_INSIGHT) {
                continue;
            }

            Map<String, Long> counts =
                    attributeCounts.getOrDefault(attribute, Map.of());

            for (Map.Entry<String, Long> entry : counts.entrySet()) {

                long count = entry.getValue();

                double percentage = (double) count / total * 100;

                if (percentage < ATTRIBUTE_THRESHOLD) {
                    continue;
                }

                BoulderAttributeInsightDTO insight =
                        new BoulderAttributeInsightDTO();

                insight.setAttribute(attribute);
                insight.setValue(entry.getKey());
                insight.setMatchingBoulders(count);
                insight.setTotalBoulders(total);
                insight.setPercentage(percentage);

                insights.add(insight);
            }
        }

        // Stärkste Gemeinsamkeit zuerst
        return insights.stream()
                .sorted(Comparator.comparingDouble(
                        BoulderAttributeInsightDTO::getPercentage
                ).reversed())
                .toList();
    }


    //Hilfsmethode für das prüfen von Boulder-Attributen
    private void analyzeAttribute(
        BoulderAttribute attribute,
        List<Boulder> boulders,
        Function<Boulder, String> valueExtractor,
        Map<BoulderAttribute, Map<String, Long>> attributeCounts,
        Map<BoulderAttribute, Long> attributeTotals
    ) {

        Map<String, Long> counts = new java.util.HashMap<>();

        long total = 0;

        for (Boulder boulder : boulders) {

            String value = valueExtractor.apply(boulder);

            // Nicht gesetzte Attribute ignorieren
            if (value == null) {
                continue;
            }

            counts.merge(value, 1L, Long::sum);
            total++;
        }

        attributeCounts.put(attribute, counts);
        attributeTotals.put(attribute, total);
    }

    // weitere Insight Auswertung für die Grade-Distribution einer Session
    public List<GradeDistributionInsightDTO> getGradeDistributionInsights(Session session) {

        List<Attempts> attempts = attemptsRepository.findBySession(session);

        // Tops und Versuche je Grad
        Map<Grade, Long> topsByGrade = new EnumMap<>(Grade.class);
        Map<Grade, Long> triesByGrade = new EnumMap<>(Grade.class);

        // Boulder-IDs verhindern doppelte Tops
        Set<Long> countedTops = new HashSet<>();

        for (Attempts attempt : attempts) {

            if (attempt.getTries() < 1 || attempt.getBoulder() == null) {
                continue;
            }

            Boulder boulder = attempt.getBoulder();
            Grade grade = boulder.getGrade();

            if (grade == null) {
                continue;
            }

            // Gesamtzahl der Versuche je Grad
            triesByGrade.merge(
                    grade,
                    (long) attempt.getTries(),
                    Long::sum
            );

            // Getoppte Boulder je Grad
            if (attempt.isDone() && countedTops.add(boulder.getId())) {
                topsByGrade.merge(grade, 1L, Long::sum);
            }
        }

        // Gesamtwerte berechnen
        long totalTops = topsByGrade.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        long totalTries = triesByGrade.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        List<GradeDistributionInsightDTO> insights = new ArrayList<>();

        // Grad mit den meisten Tops ermitteln
        if (!topsByGrade.isEmpty()) {

            Map.Entry<Grade, Long> mostTops = topsByGrade.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow();

            GradeDistributionInsightDTO insight = new GradeDistributionInsightDTO();

            insight.setType("MOST_TOPS");
            insight.setGrade(mostTops.getKey());
            insight.setValue(mostTops.getValue());
            insight.setPercentage(
                    (double) mostTops.getValue() / totalTops * 100
            );

            insights.add(insight);
        }

        // Grad mit den meisten Versuchen ermitteln
        if (!triesByGrade.isEmpty()) {

            Map.Entry<Grade, Long> mostTries = triesByGrade.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow();

            GradeDistributionInsightDTO insight = new GradeDistributionInsightDTO();

            insight.setType("MOST_ATTEMPTS");
            insight.setGrade(mostTries.getKey());
            insight.setValue(mostTries.getValue());
            insight.setPercentage((double) mostTries.getValue() / totalTries * 100);

            insights.add(insight);
        }

        return insights;
    }

    //Diese Methode gibt eine Liste von Strings zurück, über highlights die in dieser Session entsanden sind
    public List<NewHighlightDTO> getNewHighlightInsights(Session session, User user) {

        List<HighlightResponseDTO> highlights = highlightsService.getHighlightsCreatedBySession(session, user);

        return highlights.stream()
                .map(highlight -> {
                    NewHighlightDTO dto = new NewHighlightDTO();

                    dto.setType(highlight.getType());
                    dto.setMessage(
                        "Neues Highlight entstanden: "
                        + highlight.getType().getDisplayName()
                    );

                    if (highlight.getAttemptId() != null) {
                        dto.setSessionAttemptInsightDTO(toAttemptInsightDTO(attemptsRepository.getReferenceById(highlight.getAttemptId())));
                    }
                    return dto;
                })
                .toList();
    }

    // Diese Methode überprüft ob in dieser Session ein Top in einem Attribut dabei ist, der vorher noch als eine Weakness galt.
    public List<FormerWeaknessTopDTO> getTopsWithFormerWeaknesses(Session session, User user) {

        StrengthWeaknessResponseDTO progress =
                progressService.getStrengthsAndWeaknessesBefore(
                        user,
                        session.getDate()
                );

        List<StrengthWeaknessDTO> weaknesses = progress.getWeaknesses();

        if (weaknesses == null || weaknesses.isEmpty()) {
            return List.of();
        }

        List<Attempts> tops = attemptsRepository.findBySession(session)
                .stream()
                .filter(Attempts::isDone)
                .filter(attempt -> attempt.getBoulder() != null)
                .toList();

        List<FormerWeaknessTopDTO> result = new ArrayList<>();

        for (Attempts attempt : tops) {

            Boulder boulder = attempt.getBoulder();

            List<BoulderAttributeInsightDTO> matchingWeaknesses =
                    new ArrayList<>();

            collectFormerWeakness(
                    matchingWeaknesses,
                    weaknesses,
                    boulder.getGripType(),
                    BoulderAttribute.GRIP_TYPE
            );

            collectFormerWeakness(
                    matchingWeaknesses,
                    weaknesses,
                    boulder.getWallAngle(),
                    BoulderAttribute.WALL_ANGLE
            );

            collectFormerWeakness(
                    matchingWeaknesses,
                    weaknesses,
                    boulder.getClimbingStyle(),
                    BoulderAttribute.CLIMBING_STYLE
            );

            collectFormerWeakness(
                    matchingWeaknesses,
                    weaknesses,
                    boulder.getRouteCharacter(),
                    BoulderAttribute.ROUTE_CHARACTER
            );

            // Nur Boulder aufnehmen, die mindestens eine frühere Schwäche haben
            if (!matchingWeaknesses.isEmpty()) {

                FormerWeaknessTopDTO dto = new FormerWeaknessTopDTO();

                dto.setBoulder(toBoulderInsightDTO(boulder));
                dto.setTries(attempt.getTries());
                dto.setFormerWeaknesses(matchingWeaknesses);
                dto.setGrade(attempt.getBoulder().getGrade());

                result.add(dto);
            }
        }

        return result;
    }

    //Hilfsmethode für das überprüfen von Boulder-Attribute auf Schwächen beim User.
    private <T extends Enum<T>> void collectFormerWeakness(
        List<BoulderAttributeInsightDTO> result,
        List<StrengthWeaknessDTO> weaknesses,
        T attributeValue,
        BoulderAttribute attributeType
    ) {

        if (attributeValue == null) {
            return;
        }

        boolean wasWeakness = weaknesses.stream()
                .anyMatch(weakness ->
                        weakness.getAttributeType() == attributeType
                        && weakness.getAttribute().equals(attributeValue.name())
                );

        if (wasWeakness) {

            BoulderAttributeInsightDTO dto =
                    new BoulderAttributeInsightDTO();

            dto.setAttribute(attributeType);
            dto.setValue(attributeValue.name());

            result.add(dto);
        }
    }

    // Hilfsmethode für das umrechnen von Grade.Enum zu Zahlen Werten.
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

    //Hilfsmethode, die Attempt Entitäten in ein DTO mappt.
    private SessionAttemptInsightDTO toAttemptInsightDTO(Attempts attempt) {

        if (attempt == null || attempt.getBoulder() == null) {
            return null;
        }

        Boulder boulder = attempt.getBoulder();

        BoulderInsightDTO boulderDTO = new BoulderInsightDTO();

        boulderDTO.setId(boulder.getId());
        boulderDTO.setName(boulder.getName());

        boulderDTO.setGripType(boulder.getGripType());
        boulderDTO.setWallAngle(boulder.getWallAngle());
        boulderDTO.setClimbingStyle(boulder.getClimbingStyle());
        boulderDTO.setRouteCharacter(boulder.getRouteCharacter());

        SessionAttemptInsightDTO dto = new SessionAttemptInsightDTO();

        dto.setBoulder(boulderDTO);
        dto.setTries(attempt.getTries());
        dto.setDone(attempt.isDone());
        dto.setGrade(boulder.getGrade());

        return dto;
    }

    // mapped einen boulder auf ein BoulderInsightDTO
    private BoulderInsightDTO toBoulderInsightDTO(Boulder boulder) {
        if (boulder == null) {
            return null;
        }

        BoulderInsightDTO dto = new BoulderInsightDTO();
        dto.setId(boulder.getId());
        dto.setName(boulder.getName());
        dto.setGripType(boulder.getGripType());
        dto.setWallAngle(boulder.getWallAngle());
        dto.setClimbingStyle(boulder.getClimbingStyle());
        dto.setRouteCharacter(boulder.getRouteCharacter());

        return dto;
    }

}
