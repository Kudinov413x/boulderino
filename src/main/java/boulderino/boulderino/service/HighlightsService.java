package boulderino.boulderino.service;

import boulderino.boulderino.dto.HighlightResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.HighlightType;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.temporal.WeekFields;

@Service
public class HighlightsService {

        //Konstante für das Highlight Limit
        private static final int DEFAULT_HIGHLIGHT_LIMIT = 999;

        // Konstanten für TopOuts
        private static final int RECENT_DAYS = 14;
        private static final int SESSION_RECORD_DAYS = 30;
        private static final int HARD_SEND_PROFILE_DAYS = 60;
        private static final int HARD_SEND_MINIMUM_SENDS = 15;
        private static final int HARD_SEND_FALLBACK_DELTA = 2;

        private static final double HARD_SEND_RARITY_THRESHOLD = 0.20;
        private static final int HARD_SEND_MAX_FLOOR_DELTA = 2;

        // Konstanten für Sessions
        private static final int SESSION_TRIES_MINIMUM = 18;
        private static final int SESSION_TRIES_LOOKBACK = 10;
        private static final int SESSION_TRIES_MIN_HISTORY = 5;
        private static final double SESSION_TRIES_THRESHOLD = 1.3;
        private static final int SESSION_TRIES_RECENT_DAYS = 30;

        private static final int SESSION_PEAK_MIN_HARD_SENDS = 3;

        private static final int SESSION_PROJECT_MIN_TRIES = 5;

        private static final int HOT_STREAK_WEEKS_PER_MONTH = 4;

        // Konstanten für Developement
        private static final int PROGRESS_PERIOD_DAYS = 30;
        private static final int PROGRESS_MINIMUM_SENDS = 5;

        private static final double EFFICIENCY_MINIMUM_IMPROVEMENT = 1.0;

        private static final int CONSISTENCY_HISTORY_WEEKS = 8;
        private static final int CONSISTENCY_MIN_HISTORY_WEEKS = 4;
        private static final double CONSISTENCY_MIN_IMPROVEMENT = 1.25;

        private static final int MILESTONE_TOPS_INTERVAL = 50;
        private static final int MILESTONE_LOOKBACK_DAYS = 7;

        private static final int FLASHRATE_RECENT_DAYS = 14;
        private static final double FLASHRATE_MINIMUM_IMPROVEMENT = 2.0;

        private static final List<Grade> GRADE_ORDER = List.of(
                Grade.FB1,
                Grade.FB1_PLUS,
                Grade.FB2,
                Grade.FB2_PLUS,
                Grade.FB3,
                Grade.FB3_PLUS,
                Grade.FB4,
                Grade.FB4_PLUS,
                Grade.FB5,
                Grade.FB5_PLUS,
                Grade.FB6,
                Grade.FB6_PLUS,
                Grade.FB6_A,
                Grade.FB6_B,
                Grade.FB6_C,
                Grade.FB7,
                Grade.FB7_PLUS,
                Grade.FB7_A,
                Grade.FB7_B,
                Grade.FB7_C,
                Grade.FB8,
                Grade.FB8_PLUS
        );

        /*
        * Ab 4+ werden Hard Sends nicht mehr ausschließlich
        * über einen neuen MaxGrade erkannt.
        */
        private static final Grade HARD_SEND_PLATEAU_GRADE = Grade.FB4_PLUS;
        private final AttemptsRepository attemptsRepository;
        public HighlightsService(AttemptsRepository attemptsRepository) {
                this.attemptsRepository = attemptsRepository;
        }

        // Zusammenfassung für den Controller
        @Transactional
        public List<HighlightResponseDTO> getHighlights(User user) {

                List<Attempts> attempts = attemptsRepository.findBySessionUser(user);

                if (attempts.isEmpty()) {
                return List.of();
                }

                LocalDate latestDate = attempts.stream()
                        .map(this::getSessionDate)
                        .max(LocalDate::compareTo)
                        .orElse(LocalDate.now());

                LocalDate recentStart = latestDate.minusDays(RECENT_DAYS);

                LocalDate sessionRecordStart = latestDate.minusDays(SESSION_RECORD_DAYS);

                /*
                * -----------------------------------------------------
                * ALLE DATEN
                * -----------------------------------------------------
                */

                List<Attempts> successfulAttempts = attempts.stream()
                        .filter(Attempts::isDone)
                        .sorted(
                                Comparator
                                        .comparing(this::getSessionDate)
                                        .thenComparing(this::getSessionId)
                                        .thenComparing(this::getAttemptId)
                        )
                        .toList();

                /*
                * Nur Events innerhalb des Recent-Zeitraums.
                */
                List<Attempts> recentSuccessfulAttempts =
                        successfulAttempts.stream()
                                .filter(a ->
                                        !getSessionDate(a).isBefore(recentStart)
                                                && !getSessionDate(a).isAfter(latestDate)
                                )
                                .toList();

                List<Attempts> recentFlashAttempts =
                        recentSuccessfulAttempts.stream()
                                .filter(this::isFlash)
                                .toList();

                Map<Boulder, List<Attempts>> attemptsByBoulder =
                        attempts.stream()
                                .collect(Collectors.groupingBy(
                                        Attempts::getBoulder
                                ));

                Map<Session, List<Attempts>> attemptsBySession =
                        attempts.stream()
                                .collect(Collectors.groupingBy(
                                        Attempts::getSession
                                ));

                List<HighlightResponseDTO> candidates = new ArrayList<>();

                LocalDate performanceStart = latestDate.minusDays(HARD_SEND_PROFILE_DAYS);
                List<Attempts> successfulAttempts60Days =
                        successfulAttempts.stream()
                                .filter(a ->
                                        !getSessionDate(a).isBefore(performanceStart)
                                                && !getSessionDate(a).isAfter(latestDate)
                                )
                                .toList();
                List<Attempts> successfulFlashAttempts60Days =
                successfulAttempts60Days.stream()
                        .filter(this::isFlash)
                        .toList();
                
                /*
                * 1. HARD SEND
                */
                calculateHardSend(
                        successfulAttempts,
                        successfulAttempts60Days,
                        recentSuccessfulAttempts
                        ).ifPresent(candidates::add);

                /*
                * 2. HARD FLASH
                */
                calculateHardFlash(
                        successfulAttempts,
                        successfulFlashAttempts60Days,
                        recentFlashAttempts
                ).ifPresent(candidates::add);

                /*
                * 3. PROJECT ZONE REACHED
                */
                calculateProjectZone(
                        attempts,
                        successfulAttempts60Days,
                        recentStart,
                        latestDate
                ).ifPresent(candidates::add);


                /*
                * 4. NEW GRADE / ATTRIBUTE DEVELOPMENT
                */
                candidates.addAll(
                        calculateNewMaxGradeForAttribute(
                                successfulAttempts,
                                recentSuccessfulAttempts,
                                recentStart,
                                latestDate
                        )
                );

                /*
                * 5. PROJECT SEND
                */
                calculateProjectSend(attemptsByBoulder, recentStart,latestDate).ifPresent(candidates::add);

                // Calculations for Sessions
                /*
                * 6. high Volume Session
                */
                calculateSessionTriesHighlight(attemptsBySession, latestDate).ifPresent(candidates::add);

                /*
                * 7. Peak Session with at least 3 hard sends
                */
                Grade maxGrade = getMaxGrade(successfulAttempts);
                calculateSessionPeak(attemptsBySession, recentSuccessfulAttempts, maxGrade, latestDate).ifPresent(candidates::add);

                /*
                * 8. Session Fun: besonders viele Attempts
                */
                calculateSessionFunHighlight(attemptsBySession, latestDate).ifPresent(candidates::add);

                /*
                * 9. Session Project: intensive Arbeit an schwierigen Bouldern
                */
                calculateSessionProjectHighlight(attemptsBySession, maxGrade, latestDate).ifPresent(candidates::add);

                
                //10. Session Hot Streak
                calculateSessionHotStreak(attemptsBySession).ifPresent(candidates::add);

                // Calculation für Developements

                // 11. Verbesserung der avgTries
                calculateProgressHighlight(successfulAttempts, latestDate).ifPresent(candidates::add);

                // 12. Verbesserung von avgTries on Grade
                calculateEfficiencyHighlight(successfulAttempts, latestDate).ifPresent(candidates::add);

                // 13. Konstant wöchentliche Tops bei harten bouldern
                calculateConsistencyHighlight(attempts, latestDate).ifPresent(candidates::add);

                // 14. Meilenstein für erreichte TopsJ
                calculateMilestoneHighlight(attempts, latestDate).ifPresent(candidates::add);

                // 15. Verbesserung der Flashrate
                calculateFlashrateHighlight(attempts, latestDate).ifPresent(candidates::add);

                /*
                * Die Significance bestimmt die Reihenfolge auf dem Dashboard.
                */
                return candidates.stream()
                        .sorted(
                                Comparator.comparing(
                                        HighlightResponseDTO::getSignificance,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .limit(DEFAULT_HIGHLIGHT_LIMIT) //Platzhalter: Aktuell Limit so groß dass alle highlights ausgegeben werden. kann auf 10 gesetzt werden wenn es im FE zu viele Highlights werden.
                        .toList();
        }

        // TopOuts

        // =========================================================
        // 1. HARD SEND
        // =========================================================
        private Optional<HighlightResponseDTO> calculateHardSend(
                        List<Attempts> allSuccessfulAttempts,
                        List<Attempts> successfulAttempts60Days,
                        List<Attempts> recentSuccessfulAttempts) {

                if (recentSuccessfulAttempts.isEmpty()) {
                        return Optional.empty();
                }

                LocalDate earliestRecentDate = recentSuccessfulAttempts.stream()
                        .map(this::getSessionDate)
                        .min(LocalDate::compareTo)
                        .orElse(null);

                if (earliestRecentDate == null) {
                        return Optional.empty();
                }

                /*
                * All-Time-MaxGrade vor dem aktuellen Recent-Zeitraum.
                */
                Grade maxGradeBeforePeriod = getMaxGrade(
                        allSuccessfulAttempts.stream()
                                .filter(a -> getSessionDate(a).isBefore(earliestRecentDate))
                                .toList()
                );

                Grade recentMaxGrade = getMaxGrade(recentSuccessfulAttempts);

                if (recentMaxGrade == null) {
                        return Optional.empty();
                }

                /*
                * PHASE 1:
                * Ein neuer persönlicher Rekord wird immer hervorgehoben.
                */
                boolean newPersonalRecord =
                        maxGradeBeforePeriod == null
                                || compareGrades(recentMaxGrade, maxGradeBeforePeriod) > 0;

                if (newPersonalRecord) {
                        Attempts record = recentSuccessfulAttempts.stream()
                                .filter(a ->
                                        a.getBoulder() != null
                                                && Objects.equals(
                                                        a.getBoulder().getGrade(),
                                                        recentMaxGrade
                                                )
                                )
                                .max(
                                        Comparator
                                                .comparing(this::getSessionDate)
                                                .thenComparing(this::getSessionId)
                                                .thenComparing(this::getAttemptId)
                                )
                                .orElse(null);

                        if (record == null) {
                        return Optional.empty();
                        }

                        double significance = calculateNewMaxGradeSignificance(recentMaxGrade);

                        return Optional.of(createHighlight(
                                HighlightType.HARD_SEND,
                                "Neuer persönlicher Rekord",
                                "Du hast mit "
                                        + recentMaxGrade.getDisplayName()
                                        + " deinen bisher härtesten Boulder geschafft.",
                                record,
                                significance
                        ));
                }

                /*
                * PHASE 2:
                * Unterhalb von FB4+ gibt es ausschließlich neue persönliche
                * Rekorde als Hard-Send-Highlights.
                */
                if (maxGradeBeforePeriod == null
                        || compareGrades(
                                maxGradeBeforePeriod,
                                HARD_SEND_PLATEAU_GRADE
                        ) < 0) {
                        return Optional.empty();
                }

                /*
                * PHASE 3:
                * Das Leistungsprofil der letzten 60 Tage erstellen.
                */
                PerformanceProfile profile =
                        buildPerformanceProfile(successfulAttempts60Days);

                boolean adaptiveMode =
                        profile.totalTops() >= HARD_SEND_MINIMUM_SENDS;

                Grade referenceMax;
                int allowedDelta;

                if (adaptiveMode) {
                        /*
                        * AB 15 TOPS:
                        *
                        * Ausschließlich die aktuelle Performance bestimmt
                        * die Referenz. Der All-Time-MaxGrade wird hier nicht
                        * zur normalen Hard-Send-Auswahl verwendet.
                        */
                        referenceMax = getHighestGrade(profile);

                        if (referenceMax == null) {
                        return Optional.empty();
                        }

                        allowedDelta =
                                calculateAllowedHardSendDelta(referenceMax, profile);

                } else {
                        /*
                        * UNTER 15 TOPS:
                        *
                        * Fallback auf ein festes Delta relativ zum bisherigen
                        * persönlichen MaxGrade.
                        *
                        * Neue persönliche Rekorde wurden bereits in Phase 1
                        * behandelt.
                        */
                        referenceMax = maxGradeBeforePeriod;

                        allowedDelta = HARD_SEND_FALLBACK_DELTA;
                }

                if (allowedDelta < 0) {
                        return Optional.empty();
                }

                int referenceIndex = getGradeIndex(referenceMax);

                if (referenceIndex < 0) {
                        return Optional.empty();
                }

                /*
                * PHASE 4:
                * Ausschließlich erfolgreiche Tops aus den letzten 14 Tagen.
                */
                List<Attempts> hardSendCandidates =
                        recentSuccessfulAttempts.stream()
                                .filter(a ->
                                        a.getBoulder() != null
                                                && a.getBoulder().getGrade() != null
                                )
                                .filter(a -> {
                                        int gradeIndex =
                                                getGradeIndex(a.getBoulder().getGrade());

                                        if (gradeIndex < 0) {
                                        return false;
                                        }

                                        int delta = referenceIndex - gradeIndex;

                                        /*
                                        * Adaptive Mode:
                                        * allowedDelta wurde aus der 60-Tage-
                                        * Untergrenze abgeleitet.
                                        *
                                        * Fallback:
                                        * maximal zwei Enum-Stufen unter
                                        * dem bisherigen All-Time-MaxGrade.
                                        */
                                        return delta >= 0 && delta <= allowedDelta;
                                })
                                .filter(a -> {
                                        /*
                                        * Im adaptiven Modus muss der Kandidat
                                        * zusätzlich die aus dem 60-Tage-Profil
                                        * abgeleitete Seltenheits-Untergrenze
                                        * erfüllen.
                                        *
                                        * Da allowedDelta aus dieser Untergrenze
                                        * berechnet wird, ist dieser zusätzliche
                                        * Filter nicht erforderlich.
                                        */
                                        return true;
                                })
                                .toList();

                if (hardSendCandidates.isEmpty()) {
                        return Optional.empty();
                }

                /*
                * PHASE 5:
                * Schwierigkeit hat Vorrang, Seltenheit ist sekundär.
                * Bei gleichem Grade gewinnt der neuere Top.
                */
                Attempts best = hardSendCandidates.stream()
                        .max(
                                Comparator
                                        .comparingInt(this::getBoulderGradeValue)
                                        .thenComparingDouble(a ->
                                                calculateGradeRarity(
                                                        a.getBoulder().getGrade(),
                                                        recentSuccessfulAttempts
                                                )
                                        )
                                        .thenComparing(this::getSessionDate)
                                        .thenComparing(this::getSessionId)
                                        .thenComparing(this::getAttemptId)
                        )
                        .orElse(null);

                if (best == null) {
                        return Optional.empty();
                }

                Grade sendGrade = best.getBoulder().getGrade();

                int delta = referenceIndex - getGradeIndex(sendGrade);

                double rarity = calculateGradeRarity(
                        sendGrade,
                        recentSuccessfulAttempts
                );

                double significance = calculateHardSendSignificance(
                        sendGrade,
                        referenceMax,
                        delta,
                        rarity
                );

                String description =
                        "Du hast "
                                + sendGrade.getDisplayName()
                                + " geschafft – "
                                + describeGradeDelta(delta)
                                + " unter deinem "
                                + (adaptiveMode
                                        ? "aktuellen Leistungsreferenzgrad von "
                                        : "MaxGrade von ")
                                + referenceMax.getDisplayName()
                                + ".";

                return Optional.of(createHighlight(
                        HighlightType.HARD_SEND,
                        "Starker Send",
                        description,
                        best,
                        significance
                ));
        }

        //Hilfsmethoden zum ermitteln des Schwellenwertes (delta), welche Tops sich als Hart_Sends qualifizieren
        private record PerformanceProfile(
                int totalTops,
                Map<Grade, Long> topsPerGrade
        ) {}

        private PerformanceProfile buildPerformanceProfile(List<Attempts> successfulAttempts60Days) {

                Map<Grade, Long> topsPerGrade =
                        successfulAttempts60Days.stream()
                                .filter(a -> a.getBoulder() != null)
                                .map(a -> a.getBoulder().getGrade())
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(
                                        Function.identity(),
                                        Collectors.counting()
                                ));

                int totalTops = (int) topsPerGrade.values().stream()
                        .mapToLong(Long::longValue)
                        .sum();

                return new PerformanceProfile(totalTops, topsPerGrade);
        }

        private int getGradeIndex(Grade grade) {
                return GRADE_ORDER.indexOf(grade);
        }

        private long getTopsAtGrade(
                Grade grade,
                PerformanceProfile profile) {

        return profile.topsPerGrade().getOrDefault(grade, 0L);
        }

        /**
         * Anteil aller Tops, die auf diesem Grade oder einem
         * höheren Grade erreicht wurden.
         *
         * Beispiel:
         * 2 Tops auf FB6, 1 Top auf FB6+ und 20 Tops insgesamt:
         * kumulativer Anteil ab FB6 = 3 / 20 = 15 %.
         */
        private double calculateCumulativeShare(
                Grade grade,
                PerformanceProfile profile) {

        if (profile.totalTops() == 0) {
                return 1.0;
        }

        int gradeIndex = getGradeIndex(grade);

        if (gradeIndex < 0) {
                return 1.0;
        }

        long topsAtOrAbove = GRADE_ORDER.stream()
                .filter(g -> getGradeIndex(g) >= gradeIndex)
                .mapToLong(g -> getTopsAtGrade(g, profile))
                .sum();

        return (double) topsAtOrAbove / profile.totalTops();
        }

        private boolean isOutlierGrade( Grade grade, PerformanceProfile profile) {

                if (grade == null || getTopsAtGrade(grade, profile) != 1L) {
                        return false;
                }

                int index = getGradeIndex(grade);

                if (index <= 0) {
                        return false;
                }

                Grade nextLowerGrade = GRADE_ORDER.get(index - 1);

                return getTopsAtGrade(nextLowerGrade, profile) < 2;
                }

        private Grade getHighestGrade(PerformanceProfile profile) {
                for (int i = GRADE_ORDER.size() - 1; i >= 0; i--) {
                        Grade grade = GRADE_ORDER.get(i);

                        if (getTopsAtGrade(grade, profile) > 0) {
                        return grade;
                        }
                }

                return null;
        }

        /**
         * Findet den höchsten getoppten Grade, der kein Ausreißer ist.
         * Einzelne Ausreißer oberhalb dieses Grades werden übersprungen.
         */
        private Grade determineReferenceGrade( PerformanceProfile profile) {

                for (int i = GRADE_ORDER.size() - 1; i >= 0; i--) {
                        Grade grade = GRADE_ORDER.get(i);

                        if (getTopsAtGrade(grade, profile) == 0) {
                        continue;
                        }

                        if (!isOutlierGrade(grade, profile)) {
                        return grade;
                        }
                }

                return null;
        }

        private Grade calculateHardSendFloor( PerformanceProfile profile) {

                Grade highestGrade = getHighestGrade(profile);

                if (highestGrade == null) {
                        return null;
                }

                Grade referenceGrade = determineReferenceGrade(profile);

                /*
                * Wenn es keinen Nicht-Ausreißer als Referenz gibt,
                * bleibt der höchste tatsächlich getoppte Grade
                * die einzige sinnvolle Schwelle.
                */
                if (referenceGrade == null) {
                        return highestGrade;
                }

                boolean highestIsOutlier =
                        !highestGrade.equals(referenceGrade);

                double referenceShare =
                        calculateCumulativeShare(referenceGrade, profile);

                /*
                * Sonderfall:
                * Ein höchster Ausreißer liegt über einem zu häufig
                * getoppten Referenz-Grade.
                *
                * Dann dürfen niedrigere reguläre Tops nicht
                * als Hard_Sent ausgezeichnet werden.
                */
                if (highestIsOutlier
                        && referenceShare >= HARD_SEND_RARITY_THRESHOLD) {
                        return highestGrade;
                }

                /*
                * Regulärer Referenz-Grade ist zu häufig:
                * Erst ein Grade darüber ist qualifiziert.
                *
                * Wenn darüber kein Grade mehr existiert, gibt es
                * innerhalb der Skala keinen zulässigen Grade.
                */
                if (referenceShare >= HARD_SEND_RARITY_THRESHOLD) {
                        int nextIndex = getGradeIndex(referenceGrade) + 1;

                        if (nextIndex >= GRADE_ORDER.size()) {
                        return null;
                        }

                        return GRADE_ORDER.get(nextIndex);
                }

                /*
                * Referenz-Grade ist selten genug.
                * Wir gehen höchstens zwei Enum-Schritte nach unten.
                * Sobald ein Grade die 10-%-Grenze erreicht, ist
                * die Suche nach unten beendet.
                */
                int referenceIndex = getGradeIndex(referenceGrade);
                int lowestIndex = Math.max(
                        0,
                        referenceIndex - HARD_SEND_MAX_FLOOR_DELTA
                );

                Grade floor = referenceGrade;

                for (int i = referenceIndex - 1; i >= lowestIndex; i--) {
                        Grade candidateGrade = GRADE_ORDER.get(i);

                        double share =
                                calculateCumulativeShare(candidateGrade, profile);

                        if (share >= HARD_SEND_RARITY_THRESHOLD) {
                        break;
                        }

                        floor = candidateGrade;
                }

                return floor;
        }

        private int calculateAllowedHardSendDelta( Grade referenceMax, PerformanceProfile profile) {

                if (referenceMax == null) {
                        return -1;
                }

                /*
                * Bei weniger als 15 Tops ist die Verteilung noch
                * nicht zuverlässig genug. Wir verwenden deshalb
                * die feste Fallback-Regel.
                */
                if (profile.totalTops() < HARD_SEND_MINIMUM_SENDS) {
                        return HARD_SEND_FALLBACK_DELTA;
                }

                Grade floor = calculateHardSendFloor(profile);

                if (floor == null) {
                        return -1;
                }

                int referenceIndex = getGradeIndex(referenceMax);
                int floorIndex = getGradeIndex(floor);

                if (referenceIndex < 0 || floorIndex < 0) {
                        return -1;
                }

                /*
                * Liegt die Untergrenze oberhalb des Referenz-MaxGrades,
                * kann kein Kandidat im erlaubten Bereich liegen.
                */
                if (floorIndex > referenceIndex) {
                        return -1;
                }

                return referenceIndex - floorIndex;
        }

    // =========================================================
    // 2. HARD FLASH
    // =========================================================

        private Optional<HighlightResponseDTO> calculateHardFlash(
                        List<Attempts> allSuccessfulAttempts,
                        List<Attempts> successfulFlashAttempts60Days,
                        List<Attempts> recentFlashAttempts) {

                if (recentFlashAttempts.isEmpty()) {
                        return Optional.empty();
                }

                /*
                * PHASE 1:
                * Den bisherigen All-Time-MaxFlashGrade vor dem
                * aktuellen Recent-Zeitraum bestimmen.
                */
                LocalDate earliestRecentDate = recentFlashAttempts.stream()
                        .map(this::getSessionDate)
                        .min(LocalDate::compareTo)
                        .orElse(null);

                if (earliestRecentDate == null) {
                        return Optional.empty();
                }

                Grade maxFlashBeforePeriod = getMaxGrade(
                        allSuccessfulAttempts.stream()
                                .filter(this::isFlash)
                                .filter(a ->
                                        getSessionDate(a).isBefore(earliestRecentDate)
                                )
                                .toList()
                );

                Grade recentMaxFlash = getMaxGrade(recentFlashAttempts);

                if (recentMaxFlash == null) {
                        return Optional.empty();
                }

                /*
                * PHASE 2:
                * Ein neuer persönlicher Flash-Rekord wird immer hervorgehoben.
                */
                boolean newPersonalFlashRecord =
                        maxFlashBeforePeriod == null
                                || compareGrades(
                                        recentMaxFlash,
                                        maxFlashBeforePeriod
                                ) > 0;

                if (newPersonalFlashRecord) {
                        Attempts record = recentFlashAttempts.stream()
                                .filter(a ->
                                        a.getBoulder() != null
                                                && Objects.equals(
                                                        a.getBoulder().getGrade(),
                                                        recentMaxFlash
                                                )
                                )
                                .max(
                                        Comparator
                                                .comparing(this::getSessionDate)
                                                .thenComparing(this::getSessionId)
                                                .thenComparing(this::getAttemptId)
                                )
                                .orElse(null);

                        if (record == null) {
                        return Optional.empty();
                        }

                        double significance =
                                calculateNewMaxGradeSignificance(recentMaxFlash);

                        return Optional.of(createHighlight(
                                HighlightType.HARD_FLASH,
                                "Neuer Flash-Rekord",
                                "Du hast mit "
                                        + recentMaxFlash.getDisplayName()
                                        + " deinen bisher härtesten Boulder direkt im ersten Versuch geschafft.",
                                record,
                                significance
                        ));
                }

                /*
                * PHASE 3:
                * Unterhalb von FB4+ gibt es ausschließlich neue
                * persönliche Flash-Rekorde als Hard-Flash-Highlights.
                */
                if (maxFlashBeforePeriod == null
                        || compareGrades(
                                maxFlashBeforePeriod,
                                HARD_SEND_PLATEAU_GRADE
                        ) < 0) {
                        return Optional.empty();
                }

                /*
                * PHASE 4:
                * Das Flash-Leistungsprofil der letzten 60 Tage erstellen.
                *
                * buildPerformanceProfile() zählt die übergebene Liste.
                * Da hier ausschließlich Flashes übergeben werden,
                * beschreibt das Profil die Flash-Leistung.
                */
                PerformanceProfile profile =
                        buildPerformanceProfile(successfulFlashAttempts60Days);

                boolean adaptiveMode =
                        profile.totalTops() >= HARD_SEND_MINIMUM_SENDS;

                Grade referenceMaxFlash;
                int allowedDelta;

                if (adaptiveMode) {
                        /*
                        * AB 15 FLASHES:
                        *
                        * Die höchste Schwierigkeit im 60-Tage-Flash-Profil
                        * ist die Referenz. Das All-Time-MaxFlashGrade
                        * steuert die normale Kandidatenauswahl nicht.
                        */
                        referenceMaxFlash = getHighestGrade(profile);

                        if (referenceMaxFlash == null) {
                        return Optional.empty();
                        }

                        allowedDelta =
                                calculateAllowedHardSendDelta(
                                        referenceMaxFlash,
                                        profile
                                );

                } else {
                        /*
                        * UNTER 15 FLASHES:
                        *
                        * Fester Fallback relativ zum bisherigen
                        * All-Time-MaxFlashGrade.
                        *
                        * Neue Rekorde wurden bereits oben behandelt.
                        */
                        referenceMaxFlash = maxFlashBeforePeriod;
                        allowedDelta = HARD_SEND_FALLBACK_DELTA;
                }

                if (allowedDelta < 0) {
                        return Optional.empty();
                }

                int referenceIndex = getGradeIndex(referenceMaxFlash);

                if (referenceIndex < 0) {
                        return Optional.empty();
                }

                /*
                * PHASE 5:
                * Nur aktuelle Flashes aus den letzten 14 Tagen
                * dürfen Kandidaten sein.
                */
                List<Attempts> candidates = recentFlashAttempts.stream()
                        .filter(a ->
                                a.getBoulder() != null
                                        && a.getBoulder().getGrade() != null
                        )
                        .filter(a -> {
                                int gradeIndex =
                                        getGradeIndex(a.getBoulder().getGrade());

                                if (gradeIndex < 0) {
                                return false;
                                }

                                int delta = referenceIndex - gradeIndex;

                                return delta >= 0 && delta <= allowedDelta;
                        })
                        .toList();

                if (candidates.isEmpty()) {
                        return Optional.empty();
                }

                /*
                * PHASE 6:
                * Schwierigkeit zuerst, Rarity danach,
                * bei Gleichstand gewinnt der neuere Flash.
                */
                Attempts best = candidates.stream()
                        .max(
                                Comparator
                                        .comparingInt(this::getBoulderGradeValue)
                                        .thenComparingDouble(a ->
                                                calculateGradeRarity(
                                                        a.getBoulder().getGrade(),
                                                        recentFlashAttempts
                                                )
                                        )
                                        .thenComparing(this::getSessionDate)
                                        .thenComparing(this::getSessionId)
                                        .thenComparing(this::getAttemptId)
                        )
                        .orElse(null);

                if (best == null) {
                        return Optional.empty();
                }

                Grade flashGrade = best.getBoulder().getGrade();

                int delta =
                        referenceIndex - getGradeIndex(flashGrade);

                double rarity = calculateGradeRarity(
                        flashGrade,
                        recentFlashAttempts
                );

                double significance = calculateHardSendSignificance(
                        flashGrade,
                        referenceMaxFlash,
                        delta,
                        rarity
                );

                String referenceDescription = adaptiveMode
                        ? "aktuellen Flash-Leistungsreferenzgrad von "
                        : "bisherigen MaxFlashGrade von ";

                String description =
                        "Du hast "
                                + flashGrade.getDisplayName()
                                + " direkt im ersten Versuch geschafft – "
                                + describeGradeDelta(delta)
                                + " unter deinem "
                                + referenceDescription
                                + referenceMaxFlash.getDisplayName()
                                + ".";

                return Optional.of(createHighlight(
                        HighlightType.HARD_FLASH,
                        "Starker Flash",
                        description,
                        best,
                        significance
                ));
        }

        // =====================================================
        // 3. (hard) Project Zone Reached
        // =====================================================
        private Optional<HighlightResponseDTO> calculateProjectZone(
                        List<Attempts> allAttempts,
                        List<Attempts> successfulAttempts60Days,
                        LocalDate recentStart,
                        LocalDate latestDate) {

                if (allAttempts.isEmpty()) {
                        return Optional.empty();
                }

                // Bisheriger All-Time-MaxGrade aus erfolgreichen Tops.
                Grade allTimeMaxGrade =
                        getMaxGrade(
                                allAttempts.stream()
                                        .filter(Attempts::isDone)
                                        .filter(a -> a.getBoulder() != null)
                                        .filter(a -> a.getBoulder().getGrade() != null)
                                        .toList()
                        );

                // Höchster tatsächlich abgeschlossener Top der letzten 60 Tage.
                Grade highestCompletedTopInLast60Days =
                        getMaxGrade(
                                successfulAttempts60Days.stream()
                                        .filter(Attempts::isDone)
                                        .filter(a -> a.getBoulder() != null)
                                        .filter(a -> a.getBoulder().getGrade() != null)
                                        .toList()
                        );

                // Alle Versuche je Boulder gruppieren.
                Map<Boulder, List<Attempts>> attemptsByBoulder =
                        allAttempts.stream()
                                .filter(a -> a.getBoulder() != null)
                                .collect(Collectors.groupingBy(Attempts::getBoulder));

                List<ProjectZoneCandidate> candidates =
                        attemptsByBoulder.entrySet().stream()
                                .map(entry -> createProjectZoneCandidate(
                                        entry.getKey(),
                                        entry.getValue(),
                                        allTimeMaxGrade,
                                        highestCompletedTopInLast60Days,
                                        recentStart,
                                        latestDate
                                ))
                                .filter(Objects::nonNull)
                                .toList();

                if (candidates.isEmpty()) {
                        return Optional.empty();
                }

                ProjectZoneCandidate best =
                        candidates.stream()
                                .max(
                                        Comparator
                                                .comparingInt(
                                                        ProjectZoneCandidate::criteriaMet
                                                )
                                                .thenComparing(
                                                        ProjectZoneCandidate::grade,
                                                        this::compareGrades
                                                )
                                                .thenComparing(
                                                        ProjectZoneCandidate::totalTries
                                                )
                                                .thenComparing(
                                                        ProjectZoneCandidate::zoneDate
                                                )
                                )
                                .orElse(null);

                if (best == null) {
                        return Optional.empty();
                }

                double significance =
                        Math.min(
                                95,
                                55
                                        + getGradeValue(best.grade()) * 0.8
                                        + Math.min(15, best.totalTries())
                                        + best.criteriaMet() * 5
                        );

                return Optional.of(
                        createHighlight(
                                HighlightType.PROJECT_ZONE,
                                "Projekt-Zone erreicht",
                                "Du hast bei deinem "
                                        + best.grade().getDisplayName()
                                        + "-Projekt erstmals die Zone erreicht"
                                        + " – nach insgesamt "
                                        + best.totalTries()
                                        + " Versuchen.",
                                best.zoneAttempt(),
                                significance
                        )
                );
                }

                private ProjectZoneCandidate createProjectZoneCandidate(
                        Boulder boulder,
                        List<Attempts> attempts,
                        Grade allTimeMaxGrade,
                        Grade highestCompletedTopInLast60Days,
                        LocalDate recentStart,
                        LocalDate latestDate) {

                if (boulder == null || boulder.getGrade() == null) {
                        return null;
                }

                List<Attempts> sorted =
                        attempts.stream()
                                .sorted(
                                        Comparator
                                                .comparing(this::getSessionDate)
                                                .thenComparing(this::getSessionId)
                                                .thenComparing(this::getAttemptId)
                                )
                                .toList();

                // Das Boulder-Projekt darf noch nie getoppt worden sein.
                boolean everTopped =
                        sorted.stream().anyMatch(Attempts::isDone);

                if (everTopped) {
                        return null;
                }

                // Zone-Erfolge chronologisch bestimmen.
                List<Attempts> zoneAttempts =
                        sorted.stream()
                                .filter(this::isZoneReached)
                                .toList();

                if (zoneAttempts.isEmpty()) {
                        return null;
                }

                Attempts firstZoneAttempt = zoneAttempts.get(0);
                LocalDate firstZoneDate = getSessionDate(firstZoneAttempt);

                // Nur die erste Zone darf zählen – und sie muss aktuell sein.
                if (firstZoneDate.isBefore(recentStart)
                        || firstZoneDate.isAfter(latestDate)) {
                        return null;
                }

                int totalTries =
                        sorted.stream()
                                .mapToInt(Attempts::getTries)
                                .sum();

                boolean criterion1 =
                        allTimeMaxGrade == null
                                || compareGrades(
                                        boulder.getGrade(),
                                        allTimeMaxGrade
                                ) >= 0;

                boolean criterion2 =
                        highestCompletedTopInLast60Days != null
                                && getGradeIndex(boulder.getGrade())
                                == getGradeIndex(highestCompletedTopInLast60Days) + 1;

                boolean criterion3 = totalTries >= 10;

                int criteriaMet =
                        (criterion1 ? 1 : 0)
                                + (criterion2 ? 1 : 0)
                                + (criterion3 ? 1 : 0);

                if (criteriaMet == 0) {
                        return null;
                }

                return new ProjectZoneCandidate(
                        boulder.getGrade(),
                        totalTries,
                        firstZoneDate,
                        firstZoneAttempt,
                        criteriaMet
                );
                }

                
                private boolean isZoneReached(Attempts attempt) {
                        return attempt.isZone();
                }

        private record ProjectZoneCandidate(
                        Grade grade,
                        int totalTries,
                        LocalDate zoneDate,
                        Attempts zoneAttempt,
                        int criteriaMet
                ) {

        }

        // =========================================================
        // 4. NEW MaxGrade For Attributes
        // =========================================================
        private List<HighlightResponseDTO> calculateNewMaxGradeForAttribute(
                List<Attempts> allSuccessfulAttempts,
                List<Attempts> recentSuccessfulAttempts,
                LocalDate recentStart,
                LocalDate latestDate) {

        if (recentSuccessfulAttempts.isEmpty()) {
                return List.of();
        }

        List<AttributeCandidate> candidates = new ArrayList<>();

        calculateAttributeMax(
                allSuccessfulAttempts,
                recentSuccessfulAttempts,
                recentStart,
                latestDate,
                "Wall Angle",
                Boulder::getWallAngle
        ).ifPresent(candidates::add);

        calculateAttributeMax(
                allSuccessfulAttempts,
                recentSuccessfulAttempts,
                recentStart,
                latestDate,
                "Grip Type",
                Boulder::getGripType
        ).ifPresent(candidates::add);

        calculateAttributeMax(
                allSuccessfulAttempts,
                recentSuccessfulAttempts,
                recentStart,
                latestDate,
                "Route Character",
                Boulder::getRouteCharacter
        ).ifPresent(candidates::add);

        calculateAttributeMax(
                allSuccessfulAttempts,
                recentSuccessfulAttempts,
                recentStart,
                latestDate,
                "Climbing Style",
                Boulder::getClimbingStyle
        ).ifPresent(candidates::add);

        return candidates.stream()
                .map(candidate -> {
                        double significance =
                                calculateAttributeSignificance(
                                        candidate.delta(),
                                        candidate.newGradeValue()
                                );

                        return createHighlight(
                                HighlightType.NEW_MAXGRADE_FOR_ATTRIBUTE,
                                "Neuer Attribut-Bestwert",
                                "Neuer Bestwert für "
                                        + candidate.attributeName()
                                        + " „"
                                        + candidate.attributeValue()
                                        + "“: "
                                        + candidate.grade().getDisplayName()
                                        + " (vorher "
                                        + candidate.previousGrade().getDisplayName()
                                        + ").",
                                candidate.attempt(),
                                significance
                        );
                })
                .toList();
        }

        private <T> Optional<AttributeCandidate> calculateAttributeMax(
                        List<Attempts> allSuccessfulAttempts,
                        List<Attempts> recentSuccessfulAttempts,
                        LocalDate recentStart,
                        LocalDate latestDate,
                        String attributeName,
                        Function<Boulder, T> attributeGetter) {

                /*
                * Bisherigen persönlichen MaxGrade je Attributwert bestimmen.
                * Berücksichtigt werden nur erfolgreiche Tops vor dem
                * aktuellen 14-Tage-Zeitraum.
                */
                Map<T, Grade> maxBeforeByAttribute =
                        allSuccessfulAttempts.stream()
                                .filter(a ->
                                        a.getBoulder() != null
                                                && a.getBoulder().getGrade() != null
                                )
                                .filter(a ->
                                        getSessionDate(a).isBefore(recentStart)
                                )
                                .map(a -> new AbstractMap.SimpleEntry<>(
                                        attributeGetter.apply(a.getBoulder()),
                                        a.getBoulder().getGrade()
                                ))
                                .filter(entry -> entry.getKey() != null)
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (grade1, grade2) ->
                                                compareGrades(grade1, grade2) >= 0
                                                        ? grade1
                                                        : grade2
                                ));

                /*
                * Nur erfolgreiche Tops im gewünschten Zeitraum betrachten
                * und chronologisch sortieren.
                */
                List<Attempts> recentValidAttempts =
                        recentSuccessfulAttempts.stream()
                                .filter(a ->
                                        a.getBoulder() != null
                                                && a.getBoulder().getGrade() != null
                                )
                                .filter(a -> {
                                        LocalDate date = getSessionDate(a);
                                        return !date.isBefore(recentStart)
                                                && !date.isAfter(latestDate);
                                })
                                .filter(a ->
                                        attributeGetter.apply(a.getBoulder()) != null
                                )
                                .sorted(
                                        Comparator
                                                .comparing(this::getSessionDate)
                                                .thenComparing(this::getSessionId)
                                                .thenComparing(this::getAttemptId)
                                )
                                .toList();

                if (recentValidAttempts.isEmpty()) {
                        return Optional.empty();
                }

                AttributeCandidate best = null;

                for (Attempts attempt : recentValidAttempts) {

                        T attribute =
                                attributeGetter.apply(attempt.getBoulder());

                        Grade newGrade =
                                attempt.getBoulder().getGrade();

                        /*
                        * Aktueller persönlicher Bestwert für genau diesen
                        * Attributwert – inklusive früherer Tops im Zeitraum.
                        */
                        Grade previousGrade =
                                maxBeforeByAttribute.get(attribute);

                        boolean firstRecordedGrade =
                                previousGrade == null;

                        /*
                        * Für einen erstmaligen Attribut-Bestwert verlangen wir
                        * weiterhin mindestens FB4+.
                        */
                        if (firstRecordedGrade
                                && compareGrades(
                                        newGrade,
                                        HARD_SEND_PLATEAU_GRADE
                                ) < 0) {

                        maxBeforeByAttribute.put(attribute, newGrade);
                        continue;
                        }

                        Grade comparisonGrade =
                                firstRecordedGrade
                                        ? getLowestGrade()
                                        : previousGrade;

                        boolean isNewRecord =
                                firstRecordedGrade
                                        || compareGrades(newGrade, previousGrade) > 0;

                        if (isNewRecord) {

                        int delta =
                                getGradeValue(newGrade)
                                        - getGradeValue(comparisonGrade);

                        AttributeCandidate candidate =
                                new AttributeCandidate(
                                        attributeName,
                                        formatAttributeValue(attribute),
                                        newGrade,
                                        comparisonGrade,
                                        delta,
                                        getSessionDate(attempt),
                                        attempt,
                                        getGradeValue(newGrade)
                                );

                        if (best == null
                                || compareAttributeCandidates(candidate, best) > 0) {
                                best = candidate;
                        }
                        }

                        /*
                        * Der höchste bisher erreichte Grad dieses Attributwerts
                        * wird für spätere Tops innerhalb des Zeitraums aktualisiert.
                        */
                        if (previousGrade == null
                                || compareGrades(newGrade, previousGrade) > 0) {
                        maxBeforeByAttribute.put(attribute, newGrade);
                        }
                }

                return Optional.ofNullable(best);
        }

        private String formatAttributeValue(Object attribute) {
                if (attribute == null) {
                        return "Unbekannt";
                }

                if (attribute instanceof Enum<?> enumValue) {
                        String value = enumValue.name()
                                .toLowerCase(Locale.ROOT)
                                .replace('_', ' ');

                        return value.substring(0, 1).toUpperCase(Locale.ROOT)
                                + value.substring(1);
                }

                return attribute.toString();
        }

        private record AttributeCandidate(
                String attributeName,
                String attributeValue,
                Grade grade,
                Grade previousGrade,
                int delta,
                LocalDate date,
                Attempts attempt,
                int newGradeValue
        ) {
        }

        // =========================================================
        // 5. PROJECT SEND
        // =========================================================

                private Optional<HighlightResponseDTO> calculateProjectSend(
                        Map<Boulder, List<Attempts>> attemptsByBoulder,
                        LocalDate recentStart,
                        LocalDate latestDate) {

                        List<ProjectCandidate> candidates =
                                attemptsByBoulder.entrySet()
                                        .stream()
                                        .map(entry ->
                                                createProjectCandidate(
                                                        entry.getKey(),
                                                        entry.getValue(),
                                                        recentStart,
                                                        latestDate
                                                )
                                        )
                                        .filter(Objects::nonNull)
                                        .toList();

                        if (candidates.isEmpty()) {
                        return Optional.empty();
                        }

                        /*
                        * Das Projekt mit der höchsten Kombination aus:
                        *
                        * 1. Grad
                        * 2. relativer Anzahl Versuche
                        * 3. Dauer
                        * 4. Abschlussdatum
                        *
                        * gewinnt.
                        */
                        ProjectCandidate best =
                                candidates.stream()
                                        .max(
                                                Comparator
                                                        .comparing(
                                                                ProjectCandidate::grade,
                                                                this::compareGrades
                                                        )
                                                        .thenComparing(
                                                                ProjectCandidate::projectDifficultyScore
                                                        )
                                                        .thenComparing(
                                                                ProjectCandidate::totalTries
                                                        )
                                                        .thenComparing(
                                                                ProjectCandidate::sendDate
                                                        )
                                        )
                                        .orElse(null);

                        if (best == null) {
                        return Optional.empty();
                        }

                        double significance =
                                calculateProjectSignificance(
                                        best.grade(),
                                        best.totalTries(),
                                        best.projectSessions(),
                                        best.projectDurationDays()
                                );

                        return Optional.of(
                                createHighlight(
                                        HighlightType.PROJECT_SEND,
                                        "Projekt abgeschlossen",
                                        "Du hast "
                                                + best.grade().getDisplayName()
                                                + " nach "
                                                + best.totalTries()
                                                + " Versuchen geschafft."
                                                + (best.projectDurationDays() > 0
                                                ? " Das Projekt lief über "
                                                + best.projectDurationDays()
                                                + " Tage."
                                                : ""),
                                        best.successfulAttempt(),
                                        significance
                                )
                        );
                }

                private ProjectCandidate createProjectCandidate(
                        Boulder boulder,
                        List<Attempts> attempts,
                        LocalDate recentStart,
                        LocalDate latestDate) {

                        List<Attempts> sorted =
                                attempts.stream()
                                        .sorted(
                                                Comparator
                                                        .comparing(this::getSessionDate)
                                                        .thenComparing(this::getSessionId)
                                                        .thenComparing(this::getAttemptId)
                                        )
                                        .toList();

                        /*
                        * Wir brauchen den ERSTEN erfolgreichen Top überhaupt.
                        */
                        Attempts firstSuccessfulAttempt =
                                sorted.stream()
                                        .filter(Attempts::isDone)
                                        .findFirst()
                                        .orElse(null);

                        if (firstSuccessfulAttempt == null) {
                        return null;
                        }

                        LocalDate sendDate =
                                getSessionDate(firstSuccessfulAttempt);

                        /*
                        * Der erste Top des Boulders muss in den letzten 14 Tagen
                        * stattgefunden haben.
                        */
                        if (sendDate.isBefore(recentStart)
                                || sendDate.isAfter(latestDate)) {
                        return null;
                        }

                        /*
                        * Vor dem ersten Top muss es mindestens eine Session
                        * gegeben haben, in der der Boulder versucht,
                        * aber nicht getoppt wurde.
                        */
                        boolean hadPreviousUnsuccessfulSession =
                                sorted.stream()
                                        .filter(a ->
                                                getSessionDate(a)
                                                        .isBefore(sendDate)
                                        )
                                        .collect(
                                                Collectors.groupingBy(
                                                        Attempts::getSession
                                                )
                                        )
                                        .values()
                                        .stream()
                                        .anyMatch(sessionAttempts ->
                                                sessionAttempts.stream()
                                                        .noneMatch(
                                                                Attempts::isDone
                                                        )
                                        );

                        if (!hadPreviousUnsuccessfulSession) {
                        return null;
                        }

                        /*
                        * Alle Versuche bis einschließlich des ersten Tops.
                        */
                        List<Attempts> projectAttempts =
                                sorted.stream()
                                        .takeWhile(
                                                a ->
                                                        !a.equals(
                                                                firstSuccessfulAttempt
                                                        )
                                        )
                                        .toList();

                        int totalTries =
                                projectAttempts.stream()
                                        .mapToInt(Attempts::getTries)
                                        .sum()
                                        + firstSuccessfulAttempt.getTries();

                        int projectSessions =
                                (int) sorted.stream()
                                        .filter(a ->
                                                !getSessionDate(a)
                                                        .isAfter(sendDate)
                                        )
                                        .map(Attempts::getSession)
                                        .distinct()
                                        .count();

                        LocalDate projectStart =
                                sorted.stream()
                                        .map(this::getSessionDate)
                                        .filter(date ->
                                                date.isBefore(sendDate)
                                        )
                                        .min(LocalDate::compareTo)
                                        .orElse(sendDate);

                        long projectDurationDays =
                                java.time.temporal.ChronoUnit.DAYS.between(
                                        projectStart,
                                        sendDate
                                );

                        /*
                        * Variabler Schwellenwert:
                        *
                        * Je höher der Grad, desto mehr Versuche erwarten wir
                        * normalerweise. Ein Projekt muss deshalb relativ zu
                        * seinem Grad genügend Versuche haben.
                        */
                        int minimumTries =
                                calculateProjectMinimumTries(
                                        boulder.getGrade()
                                );

                        if (totalTries < minimumTries) {
                        return null;
                        }

                        double projectDifficultyScore =
                                calculateProjectDifficultyScore(
                                        boulder.getGrade(),
                                        totalTries
                                );

                        return new ProjectCandidate(
                                boulder.getGrade(),
                                totalTries,
                                projectSessions,
                                projectStart,
                                sendDate,
                                firstSuccessfulAttempt,
                                projectDurationDays,
                                projectDifficultyScore
                        );
        }

        // =========================================================
        // SESSION TRIES HIGHLIGHT
        // =========================================================

        private Optional<HighlightResponseDTO> calculateSessionTriesHighlight(
                Map<Session, List<Attempts>> attemptsBySession,
                LocalDate latestDate) {

                if (attemptsBySession.isEmpty() || latestDate == null) {
                        return Optional.empty();
                }

                LocalDate recentStart =
                        latestDate.minusDays(SESSION_TRIES_RECENT_DAYS - 1);

                /*
                * Alle Sessions mit Datum und Gesamt-Tries ermitteln.
                */
                List<SessionTriesCandidate> sessions =
                        attemptsBySession.entrySet()
                                .stream()
                                .filter(entry ->
                                        entry.getKey() != null
                                                && entry.getKey().getDate() != null
                                )
                                .map(entry -> new SessionTriesCandidate(
                                        entry.getKey().getId(),
                                        entry.getKey().getDate(),
                                        calculateTotalSessionTries(entry.getValue())
                                ))
                                .sorted(
                                        Comparator
                                                .comparing(SessionTriesCandidate::date)
                                                .thenComparing(
                                                        SessionTriesCandidate::sessionId,
                                                        Comparator.nullsFirst(
                                                                Comparator.naturalOrder()
                                                        )
                                                )
                                )
                                .toList();

                SessionTriesCandidate bestCandidate = null;
                double bestRatio = 0.0;

                /*
                * Nur Sessions aus den letzten 30 Tagen bewerten.
                */
                for (SessionTriesCandidate session : sessions) {

                        if (session.date().isBefore(recentStart)
                                || session.date().isAfter(latestDate)) {
                        continue;
                        }

                        /*
                        * Absoluter Mindestwert:
                        * Weniger als 18 Tries ergeben kein Highlight.
                        */
                        if (session.totalTries() < SESSION_TRIES_MINIMUM) {
                        continue;
                        }

                        /*
                        * Frühere Sessions ermitteln.
                        * Die aktuelle Session darf nicht im Durchschnitt
                        * enthalten sein.
                        */
                        List<SessionTriesCandidate> previousSessions =
                                sessions.stream()
                                        .filter(previous ->
                                                previous.date().isBefore(session.date())
                                                        || (
                                                        previous.date().isEqual(session.date())
                                                                && compareSessionIds(
                                                                        previous.sessionId(),
                                                                        session.sessionId()
                                                                ) < 0
                                                )
                                        )
                                        .filter(previous ->
                                                previous.totalTries() > 0
                                        )
                                        .toList();

                        /*
                        * Mindestens fünf vorherige Sessions benötigen wir
                        * für einen aussagekräftigen persönlichen Vergleich.
                        */
                        if (previousSessions.size() < SESSION_TRIES_MIN_HISTORY) {
                        continue;
                        }

                        /*
                        * Die letzten maximal zehn Sessions verwenden.
                        */
                        List<SessionTriesCandidate> referenceSessions =
                                previousSessions.stream()
                                        .skip(
                                                Math.max(
                                                        0,
                                                        previousSessions.size()
                                                                - SESSION_TRIES_LOOKBACK
                                                )
                                        )
                                        .toList();

                        double averageTries =
                                referenceSessions.stream()
                                        .mapToInt(SessionTriesCandidate::totalTries)
                                        .average()
                                        .orElse(0.0);

                        if (averageTries <= 0) {
                        continue;
                        }

                        /*
                        * Die Session muss deutlich über dem Durchschnitt liegen.
                        */
                        double ratio =
                                session.totalTries() / averageTries;

                        if (ratio < SESSION_TRIES_THRESHOLD) {
                        continue;
                        }

                        /*
                        * Bei mehreren passenden Sessions wählen wir diejenige
                        * mit dem größten Verhältnis zum persönlichen Durchschnitt.
                        */
                        if (bestCandidate == null || ratio > bestRatio) {
                        bestCandidate = session;
                        bestRatio = ratio;
                        }
                }

                if (bestCandidate == null) {
                        return Optional.empty();
                }

                /*
                * Significance: je stärker das Volumen den Durchschnitt
                * übersteigt, desto höher der Wert.
                */
                double significance = Math.min(30, 10 + (bestRatio - 1.0) * 20);

                HighlightResponseDTO dto =
                        new HighlightResponseDTO();

                dto.setType(HighlightType.SESSION_TRIES);

                dto.setTitle("Besonders hohes Trainingsvolumen");

                dto.setDescription(
                        "Du hast in einer Session "
                                + bestCandidate.totalTries()
                                + " Versuche gesammelt – "
                                + String.format(
                                        Locale.GERMAN,
                                        "%.1f",
                                        bestRatio
                                )
                                + "-mal so viele wie in deinen letzten "
                                + "vergleichbaren Sessions."
                );

                dto.setDate(bestCandidate.date());
                dto.setSignificance(significance);

                return Optional.of(dto);
        }

        private int calculateTotalSessionTries(List<Attempts> attempts) {

                if (attempts == null || attempts.isEmpty()) {
                        return 0;
                }

                return attempts.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(attempt ->
                                Math.max(0, attempt.getTries())
                        )
                        .sum();
        }

        // =========================================================
        // SESSION PEAK
        // =========================================================

        private Optional<HighlightResponseDTO> calculateSessionPeak(
                Map<Session, List<Attempts>> attemptsBySession,
                List<Attempts> recentSuccessfulAttempts,
                Grade maxGrade,
                LocalDate latestDate) {

                if (attemptsBySession.isEmpty()
                        || recentSuccessfulAttempts.isEmpty()
                        || maxGrade == null
                        || latestDate == null) {
                        return Optional.empty();
                }

                LocalDate recentStart = latestDate.minusDays(SESSION_TRIES_RECENT_DAYS - 1);

                int allowedDelta =
                        calculateAllowedHardSendDelta(
                                maxGrade,
                                recentSuccessfulAttempts
                        );

                SessionPeakCandidate bestCandidate = null;

                for (Map.Entry<Session, List<Attempts>> entry
                        : attemptsBySession.entrySet()) {

                        Session session = entry.getKey();

                        if (session == null || session.getDate() == null) {
                        continue;
                        }

                        LocalDate sessionDate = session.getDate();

                        if (sessionDate.isBefore(recentStart)
                                || sessionDate.isAfter(latestDate)) {
                        continue;
                        }

                        List<Attempts> successfulAttempts =
                                entry.getValue().stream()
                                        .filter(Objects::nonNull)
                                        .filter(Attempts::isDone)
                                        .filter(a ->
                                                a.getBoulder() != null
                                                        && a.getBoulder().getGrade() != null
                                        )
                                        .toList();

                        if (successfulAttempts.isEmpty()) {
                        continue;
                        }

                        /*
                        * Hard Sends dieser Session zählen.
                        */
                        List<Attempts> hardSends =
                                successfulAttempts.stream()
                                        .filter(attempt -> {
                                        int delta =
                                                getGradeValue(maxGrade)
                                                        - getBoulderGradeValue(attempt);

                                        return delta >= 0
                                                && delta <= allowedDelta;
                                        })
                                        .toList();

                        int hardSendCount = hardSends.size();

                        if (hardSendCount < SESSION_PEAK_MIN_HARD_SENDS) {
                        continue;
                        }

                        SessionPeakCandidate candidate = new SessionPeakCandidate( session.getId(), sessionDate, hardSendCount );

                        if (bestCandidate == null
                                || candidate.hardSendCount()
                                        > bestCandidate.hardSendCount()
                                || (
                                        candidate.hardSendCount()
                                                == bestCandidate.hardSendCount()
                                                && candidate.date()
                                                        .isAfter(bestCandidate.date())
                                )) {
                        bestCandidate = candidate;
                        }
                }

                if (bestCandidate == null) {
                        return Optional.empty();
                }

                /*
                * Significance steigt mit der Zahl der Hard Sends.
                */
                double significance = Math.min(58, 38 + (bestCandidate.hardSendCount() - SESSION_PEAK_MIN_HARD_SENDS) * 5.0);

                HighlightResponseDTO dto = new HighlightResponseDTO();
                dto.setType(HighlightType.SESSION_PEAK);
                dto.setTitle("Peak Session");
                dto.setDescription(
                        "Starke Session: Du hast "
                                + bestCandidate.hardSendCount()
                                + " Hard Sends geschafft."
                );
                dto.setDate(bestCandidate.date());
                dto.setSignificance(significance);
                return Optional.of(dto);
        }

        // =========================================================
        // SESSION FUN HIGHLIGHT
        // =========================================================

        private Optional<HighlightResponseDTO> calculateSessionFunHighlight(
                Map<Session, List<Attempts>> attemptsBySession,
                LocalDate latestDate) {

                if (attemptsBySession.isEmpty() || latestDate == null) {
                        return Optional.empty();
                }

                LocalDate recentStart =
                        latestDate.minusDays(SESSION_TRIES_RECENT_DAYS - 1);

                List<SessionFunCandidate> sessions =
                        attemptsBySession.entrySet()
                                .stream()
                                .filter(entry ->
                                        entry.getKey() != null
                                                && entry.getKey().getDate() != null
                                )
                                .map(entry -> new SessionFunCandidate(
                                        entry.getKey().getId(),
                                        entry.getKey().getDate(),
                                        entry.getValue() == null
                                                ? 0
                                                : entry.getValue().size()
                                ))
                                .sorted(
                                        Comparator
                                                .comparing(SessionFunCandidate::date)
                                                .thenComparing(
                                                        SessionFunCandidate::sessionId,
                                                        Comparator.nullsFirst(
                                                                Comparator.naturalOrder()
                                                        )
                                                )
                                )
                                .toList();

                SessionFunCandidate bestCandidate = null;
                double bestRatio = 0.0;

                for (SessionFunCandidate session : sessions) {

                        if (session.date().isBefore(recentStart)
                                || session.date().isAfter(latestDate)) {
                        continue;
                        }

                        List<SessionFunCandidate> previousSessions =
                                sessions.stream()
                                        .filter(previous ->
                                                previous.date().isBefore(session.date())
                                                        || (
                                                        previous.date().isEqual(session.date())
                                                                && compareSessionIds(
                                                                        previous.sessionId(),
                                                                        session.sessionId()
                                                                ) < 0
                                                )
                                        )
                                        .filter(previous ->
                                                previous.attempts() > 0
                                        )
                                        .toList();

                        if (previousSessions.size() < SESSION_TRIES_MIN_HISTORY) {
                        continue;
                        }

                        List<SessionFunCandidate> referenceSessions =
                                previousSessions.stream()
                                        .skip(
                                                Math.max(
                                                        0,
                                                        previousSessions.size()
                                                                - SESSION_TRIES_LOOKBACK
                                                )
                                        )
                                        .toList();

                        double averageAttempts =
                                referenceSessions.stream()
                                        .mapToInt(SessionFunCandidate::attempts)
                                        .average()
                                        .orElse(0.0);

                        if (averageAttempts <= 0) {
                                continue;
                        }

                        double ratio = session.attempts() / averageAttempts;

                        if (ratio < SESSION_TRIES_THRESHOLD) {
                                continue;
                        }

                        if (bestCandidate == null || ratio > bestRatio) {
                                bestCandidate = session;
                                bestRatio = ratio;
                        }
                }

                if (bestCandidate == null) {
                        return Optional.empty();
                }

                double significance = Math.min(29, 9 + (bestRatio - 1.0) * 25);

                HighlightResponseDTO dto = new HighlightResponseDTO();
                dto.setType(HighlightType.SESSION_FUN);
                dto.setTitle("Session mit besonders vielen Attempts");
                dto.setDescription(
                        "In dieser Session hast du "
                                + bestCandidate.attempts()
                                + " Attempts gesammelt – "
                                + String.format(
                                        Locale.GERMAN,
                                        "%.1f",
                                        bestRatio
                                )
                                + "-mal so viele wie in deinen letzten "
                                + "vergleichbaren Sessions."
                );

                dto.setDate(bestCandidate.date());
                dto.setSignificance(significance);

                return Optional.of(dto);
        }

        // =========================================================
        // 9. SESSION PROJECT HIGHLIGHT
        // =========================================================
        private Optional<HighlightResponseDTO> calculateSessionProjectHighlight(
                Map<Session, List<Attempts>> attemptsBySession,
                Grade maxGrade,
                LocalDate latestDate) {

                if (attemptsBySession.isEmpty()
                        || maxGrade == null
                        || latestDate == null) {
                        return Optional.empty();
                }

                LocalDate recentStart =
                        latestDate.minusDays(SESSION_TRIES_RECENT_DAYS - 1);

                int maxGradeValue = getGradeValue(maxGrade);

                // MaxGrade - 1: schwierige Projekt-Boulder
                int projectGradeThreshold = maxGradeValue - 1;

                // MaxGrade - 2: schwierige Boulder für die Top-Quote
                int hardBoulderThreshold = maxGradeValue - 2;

                SessionProjectCandidate bestCandidate =
                        attemptsBySession.entrySet()
                                .stream()
                                .filter(entry ->
                                        entry.getKey() != null
                                                && entry.getKey().getDate() != null
                                                && entry.getValue() != null
                                )
                                .filter(entry -> {
                                        LocalDate date = entry.getKey().getDate();

                                        return !date.isBefore(recentStart)
                                                && !date.isAfter(latestDate);
                                })
                                .map(entry -> {
                                        List<Attempts> sessionAttempts =
                                                entry.getValue();

                                        /*
                                        * Harte Projekt-Boulder:
                                        * MaxGrade - 1 oder schwerer.
                                        */
                                        List<Attempts> hardProjects =
                                                sessionAttempts.stream()
                                                        .filter(Objects::nonNull)
                                                        .filter(a ->
                                                                a.getBoulder() != null
                                                                        && a.getBoulder().getGrade() != null
                                                        )
                                                        .filter(a ->
                                                                getGradeValue(
                                                                        a.getBoulder().getGrade()
                                                                ) >= projectGradeThreshold
                                                        )
                                                        .toList();

                                        /*
                                        * Mindestens ein harter Boulder muss
                                        * in dieser Session mindestens 5 Tries haben.
                                        *
                                        * Tries werden pro Boulder aufsummiert.
                                        */
                                        boolean hasHardProject =
                                                hardProjects.stream()
                                                        .filter(a ->
                                                                a.getBoulder().getId() != null
                                                        )
                                                        .collect(Collectors.groupingBy(
                                                                a -> a.getBoulder().getId(),
                                                                Collectors.summingInt(a ->
                                                                        Math.max(0, a.getTries())
                                                                )
                                                        ))
                                                        .values()
                                                        .stream()
                                                        .anyMatch(tries ->
                                                                tries >= SESSION_PROJECT_MIN_TRIES
                                                        );

                                        /*
                                        * Für die Top-Quote zählen alle Attempts
                                        * an Bouldern ab MaxGrade - 2.
                                        */
                                        List<Attempts> hardBoulderAttempts =
                                                sessionAttempts.stream()
                                                        .filter(Objects::nonNull)
                                                        .filter(a ->
                                                                a.getBoulder() != null
                                                                        && a.getBoulder().getGrade() != null
                                                        )
                                                        .filter(a ->
                                                                getGradeValue(
                                                                        a.getBoulder().getGrade()
                                                                ) >= hardBoulderThreshold
                                                        )
                                                        .toList();

                                        long nonTops =
                                                hardBoulderAttempts.stream()
                                                        .filter(a -> !a.isDone())
                                                        .count();

                                        long tops =
                                                hardBoulderAttempts.stream()
                                                        .filter(Attempts::isDone)
                                                        .count();

                                        boolean mostlyNonTops =
                                                !hardBoulderAttempts.isEmpty()
                                                        && nonTops > tops;

                                        return new SessionProjectCandidate(
                                                entry.getKey().getId(),
                                                entry.getKey().getDate(),
                                                hasHardProject,
                                                mostlyNonTops,
                                                hardProjects.size(),
                                                nonTops,
                                                tops
                                        );
                                })
                                .filter(candidate ->
                                        candidate.hasHardProject()
                                                && candidate.mostlyNonTops()
                                )
                                .max(
                                        Comparator
                                                .comparingLong(
                                                        SessionProjectCandidate::nonTops
                                                )
                                                .thenComparing(
                                                        SessionProjectCandidate::date
                                                )
                                )
                                .orElse(null);

                if (bestCandidate == null) {
                        return Optional.empty();
                }
                double significance = Math.min(75, 55 + bestCandidate.nonTops() * 2.0);
                HighlightResponseDTO dto = new HighlightResponseDTO();
                dto.setType(HighlightType.SESSION_PROJECT);
                dto.setTitle("Intensive Projekt-Session");
                dto.setDescription(
                        "Du hast intensiv an schwierigen Bouldern gearbeitet: "
                                + bestCandidate.nonTops()
                                + " nicht erfolgreiche Attempts und "
                                + bestCandidate.tops()
                                + " Tops an Bouldern ab "
                                + maxGrade.getDisplayName()
                                + " - 2. Mindestens ein hartes Projekt hatte "
                                + "5 oder mehr Tries."
                );
                dto.setDate(bestCandidate.date());
                dto.setSignificance(significance);

                return Optional.of(dto);
        }


        // =========================================================
        // 10. SESSION HOT STREAK
        // =========================================================
        private Optional<HighlightResponseDTO> calculateSessionHotStreak(
                Map<Session, List<Attempts>> attemptsBySession) {

                if (attemptsBySession == null || attemptsBySession.isEmpty()) {
                        return Optional.empty();
                }

                LocalDate today = LocalDate.now();

                /*
                * Alle vorhandenen Session-Daten sammeln.
                * Pro Kalenderwoche zählt es nur, ob mindestens
                * eine Session stattgefunden hat.
                */
                Set<LocalDate> sessionDates =
                        attemptsBySession.keySet()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(Session::getDate)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                if (sessionDates.isEmpty()) {
                        return Optional.empty();
                }

                Set<WeekKey> activeWeeks =
                        sessionDates.stream()
                                .map(this::toWeekKey)
                                .collect(Collectors.toSet());

                WeekKey currentWeek = toWeekKey(today);

                long sessionsThisWeek =
                        sessionDates.stream()
                                .filter(date ->
                                        toWeekKey(date).equals(currentWeek)
                                )
                                .count();

                /*
                * Streak aufeinanderfolgender aktiver Kalenderwochen
                * ab der aktuellen Woche zählen.
                */
                int consecutiveWeeks = 0;
                WeekKey weekToCheck = currentWeek;

                while (activeWeeks.contains(weekToCheck)) {
                        consecutiveWeeks++;

                        weekToCheck = new WeekKey(
                                weekToCheck.year(),
                                weekToCheck.week()
                        ).previous();
                }

                /*
                * Monats-Streak:
                * Wir zählen aufeinanderfolgende Vier-Wochen-Blöcke
                * mit mindestens einer Session in jeder Kalenderwoche.
                *
                * Diese Definition ist bewusst eine rollierende
                * Wochen-Streak und keine Kalender-Monats-Auswertung.
                */
                int consecutiveFourWeekBlocks = consecutiveWeeks / HOT_STREAK_WEEKS_PER_MONTH;

                SessionHotStreakCandidate bestCandidate = null;

                if (consecutiveFourWeekBlocks >= 1) {
                        int months = consecutiveFourWeekBlocks;

                        bestCandidate = new SessionHotStreakCandidate(
                                "Monatliche Trainingsserie",
                                "Du hast mindestens "
                                        + months
                                        + (months == 1
                                        ? " Monat lang"
                                        : " Monate lang")
                                        + " jede Woche trainiert.",
                                consecutiveWeeks,
                                Math.min(54, 34 + months * 3.0)
                        );
                } else if (consecutiveWeeks >= 2) {
                        bestCandidate = new SessionHotStreakCandidate(
                                "Wochen-Streak",
                                "Du hast "
                                        + consecutiveWeeks
                                        + " Wochen in Folge mindestens eine Session absolviert.",
                                consecutiveWeeks,
                                Math.min(50, 30 + consecutiveWeeks * 2.0)
                        );
                } else if (sessionsThisWeek > 0) {
                        bestCandidate = new SessionHotStreakCandidate(
                                "Trainingswoche",
                                "Du hast diese Woche bereits "
                                        + sessionsThisWeek
                                        + (sessionsThisWeek == 1
                                        ? " Session"
                                        : " Sessions")
                                        + "absolviert.",
                                (int) sessionsThisWeek,
                                Math.min(40, 20 + sessionsThisWeek * 5.0)
                        );
                }

                if (bestCandidate == null) {
                        return Optional.empty();
                }

                HighlightResponseDTO dto = new HighlightResponseDTO();
                dto.setType(HighlightType.SESSION_HOT_STREAK);
                dto.setTitle(bestCandidate.title());
                dto.setDescription(bestCandidate.description());
                dto.setDate(today);
                dto.setSignificance(bestCandidate.significance());

                return Optional.of(dto);
        }

        // Hilfsmethoden für Hot Streaks bei sessions
        private WeekKey toWeekKey(LocalDate date) {
                WeekFields iso = WeekFields.ISO;

                return new WeekKey(
                        date.get(iso.weekBasedYear()),
                        date.get(iso.weekOfWeekBasedYear())
                );
        }

        private record WeekKey(int year, int week) {
        private WeekKey previous() {
                LocalDate monday = LocalDate.now()
                        .with(WeekFields.ISO.weekBasedYear(), year)
                        .with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                        .with(WeekFields.ISO.dayOfWeek(), 1)
                        .minusWeeks(1);

                WeekFields iso = WeekFields.ISO;

                return new WeekKey(
                        monday.get(iso.weekBasedYear()),
                        monday.get(iso.weekOfWeekBasedYear())
                );
        }
        }

        // Developement Highlights

        // 11. Verbesserung der Avg Performance
        private Optional<HighlightResponseDTO> calculateProgressHighlight( List<Attempts> successfulAttempts, LocalDate latestDate) {

                LocalDate currentStart = latestDate.minusDays(PROGRESS_PERIOD_DAYS);
                LocalDate previousStart = currentStart.minusDays(PROGRESS_PERIOD_DAYS);

                List<Attempts> currentAttempts = successfulAttempts.stream()
                        .filter(a -> {
                                LocalDate date = getSessionDate(a);
                                return date != null
                                        && !date.isBefore(currentStart)
                                        && !date.isAfter(latestDate);
                        })
                        .toList();

                List<Attempts> previousAttempts = successfulAttempts.stream()
                        .filter(a -> {
                                LocalDate date = getSessionDate(a);
                                return date != null
                                        && !date.isBefore(previousStart)
                                        && date.isBefore(currentStart);
                        })
                        .toList();

                if (currentAttempts.size() < PROGRESS_MINIMUM_SENDS
                        || previousAttempts.size() < PROGRESS_MINIMUM_SENDS) {
                        return Optional.empty();
                }

                double currentAverageGrade = currentAttempts.stream()
                        .mapToInt(a -> getGradeValue(a.getBoulder().getGrade()))
                        .average()
                        .orElse(0.0);

                double previousAverageGrade = previousAttempts.stream()
                        .mapToInt(a -> getGradeValue(a.getBoulder().getGrade()))
                        .average()
                        .orElse(0.0);

                // Nur ausspielen, wenn sich die durchschnittliche Schwierigkeit verbessert hat.
                if (currentAverageGrade <= previousAverageGrade) {
                        return Optional.empty();
                }

                double currentAverageTries = currentAttempts.stream()
                        .mapToInt(Attempts::getTries)
                        .average()
                        .orElse(0.0);

                Grade representativeGrade = Arrays.stream(Grade.values())
                        .min(Comparator.comparingDouble(
                                grade -> Math.abs(
                                        getGradeValue(grade) - currentAverageGrade)))
                        .orElseThrow();

                HighlightResponseDTO highlight = new HighlightResponseDTO();
                highlight.setType(HighlightType.PROGRESS);
                highlight.setTitle("Dein Fortschritt");
                highlight.setDescription(
                        String.format(
                                Locale.GERMAN,
                                "Du kletterst jetzt im Schnitt %s mit %.1f Versuchen pro Boulder. Deine durchschnittliche Schwierigkeit ist gestiegen.",
                                representativeGrade.getDisplayName(),
                                currentAverageTries
                        )
                );
                highlight.setDate(latestDate);
                highlight.setSignificance(68.0);
                highlight.setGrade(representativeGrade.getDisplayName());
                highlight.setTries((int) Math.round(currentAverageTries));

                return Optional.of(highlight);
        }

        // 12. Verbesserung der AvgTriesTilTop für einen Grade
        private Optional<HighlightResponseDTO> calculateEfficiencyHighlight(List<Attempts> successfulAttempts, LocalDate latestDate) {

                LocalDate currentStart = latestDate.minusDays(PROGRESS_PERIOD_DAYS);
                LocalDate previousStart = currentStart.minusDays(PROGRESS_PERIOD_DAYS);
                List<Attempts> currentAttempts = successfulAttempts.stream()
                        .filter(a -> {
                                LocalDate date = getSessionDate(a);
                                return date != null
                                        && !date.isBefore(currentStart)
                                        && !date.isAfter(latestDate);
                        })
                        .toList();
                List<Attempts> previousAttempts = successfulAttempts.stream()
                        .filter(a -> {
                                LocalDate date = getSessionDate(a);
                                return date != null
                                        && !date.isBefore(previousStart)
                                        && date.isBefore(currentStart);
                        })
                        .toList();
                Map<Integer, List<Attempts>> currentByGrade =
                        currentAttempts.stream()
                                .collect(Collectors.groupingBy(
                                        a -> getGradeValue(a.getBoulder().getGrade())
                                ));
                Map<Integer, List<Attempts>> previousByGrade =
                        previousAttempts.stream()
                                .collect(Collectors.groupingBy(
                                        a -> getGradeValue(a.getBoulder().getGrade())
                                ));

                Integer bestGradeValue = null;
                double bestPreviousAverage = 0.0;
                double bestCurrentAverage = 0.0;
                double bestImprovement = 0.0;
                for (Map.Entry<Integer, List<Attempts>> entry
                        : currentByGrade.entrySet()) {

                        Integer gradeValue = entry.getKey();

                        List<Attempts> currentGradeAttempts = entry.getValue();
                        List<Attempts> previousGradeAttempts =
                                previousByGrade.getOrDefault(gradeValue, List.of());

                        if (currentGradeAttempts.size() < PROGRESS_MINIMUM_SENDS
                                || previousGradeAttempts.size() < PROGRESS_MINIMUM_SENDS) {
                        continue;
                        }

                        double currentAverage = currentGradeAttempts.stream()
                                .mapToInt(Attempts::getTries)
                                .average()
                                .orElse(Double.MAX_VALUE);

                        double previousAverage = previousGradeAttempts.stream()
                                .mapToInt(Attempts::getTries)
                                .average()
                                .orElse(Double.MAX_VALUE);

                        double improvement = previousAverage - currentAverage;

                        if (improvement >= EFFICIENCY_MINIMUM_IMPROVEMENT
                                && improvement > bestImprovement) {

                        bestGradeValue = gradeValue;
                        bestPreviousAverage = previousAverage;
                        bestCurrentAverage = currentAverage;
                        bestImprovement = improvement;
                        }
                }
                if (bestGradeValue == null) {
                        return Optional.empty();
                }
                final int selectedGradeValue = bestGradeValue;
                Grade representativeGrade = Arrays.stream(Grade.values())
                        .min(Comparator.comparingInt(
                                grade -> Math.abs(
                                        getGradeValue(grade) - selectedGradeValue)))
                        .orElseThrow();

                HighlightResponseDTO highlight = new HighlightResponseDTO();
                highlight.setType(HighlightType.EFFICIENCY);
                highlight.setTitle("Effizienter geklettert");
                highlight.setDescription(String.format(
                        Locale.GERMAN,
                        "Bei %s hast du deine durchschnittlichen Versuche bis zum Top von %.1f auf %.1f gesenkt.",
                        representativeGrade.getDisplayName(),
                        bestPreviousAverage,
                        bestCurrentAverage
                ));
                highlight.setDate(latestDate);
                highlight.setSignificance(Math.min(67.0, 40.0 + bestImprovement * 5.0));
                highlight.setGrade(representativeGrade.getDisplayName());
                highlight.setTries((int) Math.round(bestCurrentAverage));

                return Optional.of(highlight);
        }

        // 13. Meilenstein für Konstante Tops auf harten Bouldern
        private Optional<HighlightResponseDTO> calculateConsistencyHighlight( List<Attempts> attempts, LocalDate latestDate) {

                WeekFields iso = WeekFields.ISO;

                LocalDate currentWeekStart = latestDate
                        .with(iso.dayOfWeek(), 1);

                LocalDate historyStart = currentWeekStart
                        .minusWeeks(CONSISTENCY_HISTORY_WEEKS);

                LocalDate recentStart = latestDate.minusDays(HARD_SEND_PROFILE_DAYS);

                List<Attempts> successfulAttempts = attempts.stream()
                        .filter(Attempts::isDone)
                        .filter(a -> a.getBoulder() != null)
                        .filter(a -> a.getBoulder().getGrade() != null)
                        .filter(a -> getSessionDate(a) != null)
                        .toList();

                /*
                * 1. Hard-Send-Schwelle aus dem individuellen Leistungsprofil
                *    ableiten – mit derselben adaptiven Logik wie bei Hard Sends.
                */
                List<Attempts> successfulAttempts60Days = successfulAttempts.stream()
                        .filter(a -> {
                                LocalDate date = getSessionDate(a);
                                return !date.isBefore(recentStart)
                                        && !date.isAfter(latestDate);
                        })
                        .toList();

                PerformanceProfile profile =
                        buildPerformanceProfile(successfulAttempts60Days);

                Grade hardSendFloor;

                if (profile.totalTops() >= HARD_SEND_MINIMUM_SENDS) {
                        // Adaptive Hard-Send-Logik bei ausreichender Datenmenge.
                        hardSendFloor = calculateHardSendFloor(profile);

                        if (hardSendFloor == null) {
                        return Optional.empty();
                        }
                } else {
                        /*
                        * Fallback wie bei calculateHardSend():
                        * zwei Grade unter dem bisherigen persönlichen Maximum
                        * vor dem aktuellen Recent-Zeitraum.
                        */
                        LocalDate earliestRecentDate = successfulAttempts.stream()
                                .map(this::getSessionDate)
                                .filter(date -> !date.isBefore(recentStart))
                                .filter(date -> !date.isAfter(latestDate))
                                .min(LocalDate::compareTo)
                                .orElse(null);

                        if (earliestRecentDate == null) {
                        return Optional.empty();
                        }

                        Grade maxGradeBeforePeriod = getMaxGrade(
                                successfulAttempts.stream()
                                        .filter(a -> getSessionDate(a)
                                                .isBefore(earliestRecentDate))
                                        .toList()
                        );

                        if (maxGradeBeforePeriod == null) {
                        return Optional.empty();
                        }

                        int maxIndex = getGradeIndex(maxGradeBeforePeriod);

                        if (maxIndex < 0) {
                        return Optional.empty();
                        }

                        int floorIndex = Math.max(
                                0,
                                maxIndex - HARD_SEND_FALLBACK_DELTA
                        );

                        hardSendFloor = GRADE_ORDER.get(floorIndex);
                }

                int floorIndex = getGradeIndex(hardSendFloor);

                if (floorIndex < 0) {
                        return Optional.empty();
                }

                /*
                * 2. Hard-Send-Boulder je Kalenderwoche zählen.
                *    Jeder Boulder wird innerhalb einer Woche nur einmal gezählt.
                */
                Map<LocalDate, Set<Long>> hardSendsByWeek = new HashMap<>();

                // Wochen, in denen der Nutzer überhaupt trainiert hat.
                Set<LocalDate> trainingWeeks = new HashSet<>();

                for (Attempts attempt : attempts) {
                        LocalDate date = getSessionDate(attempt);

                        if (date == null
                                || date.isBefore(historyStart)
                                || date.isAfter(latestDate)) {
                        continue;
                        }

                        LocalDate weekStart = date.with(iso.dayOfWeek(), 1);
                        trainingWeeks.add(weekStart);

                        if (!attempt.isDone()
                                || attempt.getBoulder() == null
                                || attempt.getBoulder().getGrade() == null
                                || attempt.getBoulder().getId() == null) {
                        continue;
                        }

                        Grade grade = attempt.getBoulder().getGrade();
                        int gradeIndex = getGradeIndex(grade);

                        if (gradeIndex < floorIndex) {
                        continue;
                        }

                        hardSendsByWeek
                                .computeIfAbsent(weekStart, key -> new HashSet<>())
                                .add(attempt.getBoulder().getId());
                }

                /*
                * 3. Genügend persönliche Historie verlangen.
                *    Gezählt werden Wochen mit mindestens einer Session,
                *    nicht nur Wochen mit Hard Sends.
                */
                long weeksWithTraining = trainingWeeks.stream()
                        .filter(week -> week.isBefore(currentWeekStart))
                        .filter(week -> !week.isBefore(historyStart))
                        .count();

                if (weeksWithTraining < CONSISTENCY_MIN_HISTORY_WEEKS) {
                        return Optional.empty();
                }

                /*
                * 4. Durchschnitt der letzten vollständigen Wochen.
                *    Wochen ohne Hard Sends zählen als null.
                */
                int totalHistoricalHardSends = 0;

                for (int i = 1; i <= CONSISTENCY_HISTORY_WEEKS; i++) {
                        LocalDate weekStart = currentWeekStart.minusWeeks(i);

                        totalHistoricalHardSends += hardSendsByWeek
                                .getOrDefault(weekStart, Set.of())
                                .size();
                }

                double averageWeeklyHardSends =
                        (double) totalHistoricalHardSends
                                / CONSISTENCY_HISTORY_WEEKS;

                /*
                * 5. Aktuelle Woche mit dem persönlichen Durchschnitt vergleichen.
                */
                int currentWeekHardSends = hardSendsByWeek
                        .getOrDefault(currentWeekStart, Set.of())
                        .size();

                if (currentWeekHardSends <= averageWeeklyHardSends
                        || currentWeekHardSends
                                < averageWeeklyHardSends
                                * CONSISTENCY_MIN_IMPROVEMENT) {
                        return Optional.empty();
                }

                double improvementPercent =
                        averageWeeklyHardSends > 0
                                ? ((currentWeekHardSends - averageWeeklyHardSends)
                                / averageWeeklyHardSends) * 100.0
                                : 100.0;

                HighlightResponseDTO highlight = new HighlightResponseDTO();
                highlight.setType(HighlightType.CONSISTENCY);
                highlight.setTitle("Deine Konsistenz steigt");
                highlight.setDescription(String.format(
                        Locale.GERMAN,
                        "Du hast diese Woche %d unterschiedliche Hard-Send-Boulder geschafft – dein Durchschnitt liegt bei %.1f pro Woche. Das sind %.0f %% mehr als gewöhnlich.",
                        currentWeekHardSends,
                        averageWeeklyHardSends,
                        improvementPercent
                ));
                highlight.setDate(latestDate);
                highlight.setSignificance(Math.min(55.0, 35.0 + improvementPercent * 0.2));

                return Optional.of(highlight);
        }

        // 14. MeilenSteine für erreichte Tops
        private Optional<HighlightResponseDTO> calculateMilestoneHighlight( List<Attempts> attempts, LocalDate latestDate ) {
                LocalDate startDate = latestDate.minusDays(MILESTONE_LOOKBACK_DAYS - 1L);

                // Nur erfolgreiche Attempts mit gültigem Boulder berücksichtigen.
                List<Attempts> successfulAttempts = attempts.stream()
                        .filter(Attempts::isDone)
                        .filter(a -> a.getBoulder() != null)
                        .filter(a -> a.getBoulder().getId() != null)
                        .filter(a -> getSessionDate(a) != null)
                        .filter(a -> !getSessionDate(a).isAfter(latestDate))
                        .toList();

                // Alle einzigartigen Boulder, die jemals erfolgreich geklettert wurden.
                Set<Long> allUniqueTops = successfulAttempts.stream()
                        .map(a -> a.getBoulder().getId())
                        .collect(Collectors.toSet());

                int totalUniqueTops = allUniqueTops.size();

                // Prüfen, ob ein 50er-Meilenstein erreicht wurde.
                if (totalUniqueTops < MILESTONE_TOPS_INTERVAL) {
                        return Optional.empty();
                }

                int milestone = (totalUniqueTops / MILESTONE_TOPS_INTERVAL)
                        * MILESTONE_TOPS_INTERVAL;

                // Datum bestimmen, an dem der Meilenstein erstmals erreicht wurde.
                Map<Long, LocalDate> firstTopDates = successfulAttempts.stream()
                        .collect(Collectors.groupingBy(
                                a -> a.getBoulder().getId(),
                                Collectors.mapping(
                                        this::getSessionDate,
                                        Collectors.minBy(LocalDate::compareTo)
                                )
                        ))
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().isPresent())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().orElseThrow()
                        ));

                List<LocalDate> uniqueTopDates = firstTopDates.values().stream()
                        .sorted()
                        .toList();

                // Das Datum des Tops, mit dem der Meilenstein erreicht wurde.
                LocalDate milestoneDate = uniqueTopDates.get(milestone - 1);

                if (milestoneDate.isBefore(startDate)) {
                        return Optional.empty();
                }

                HighlightResponseDTO highlight = new HighlightResponseDTO();
                highlight.setType(HighlightType.MILESTONE);
                highlight.setTitle("Neuer Kletter-Meilenstein!");
                highlight.setDescription(String.format(
                        Locale.GERMAN,
                        "Du hast deinen %d. unterschiedlichen Boulder erfolgreich geklettert. Weiter so!",
                        milestone
                ));
                highlight.setDate(milestoneDate);
                highlight.setSignificance(71.0);

                return Optional.of(highlight);
        }

        // 15. verbesserung der Flashrate
        private Optional<HighlightResponseDTO> calculateFlashrateHighlight( List<Attempts> attempts, LocalDate latestDate ) {
                LocalDate recentStart = latestDate.minusDays(13);

                // Gültige Versuche bis einschließlich latestDate.
                List<Attempts> validAttempts = attempts.stream()
                        .filter(a -> a.getBoulder() != null)
                        .filter(a -> a.getBoulder().getId() != null)
                        .filter(a -> getSessionDate(a) != null)
                        .filter(a -> !getSessionDate(a).isAfter(latestDate))
                        .toList();

                // Alle Versuche eines Boulders zusammenfassen.
                Map<Long, List<Attempts>> attemptsByBoulder =
                        validAttempts.stream()
                                .collect(Collectors.groupingBy(
                                        a -> a.getBoulder().getId()
                                ));

                int previousFlashes = 0;
                int previousBoulders = 0;
                int overallFlashes = 0;
                int overallBoulders = 0;
                int recentBoulders = 0;

                for (List<Attempts> boulderAttempts : attemptsByBoulder.values()) {
                        List<Attempts> ordered = boulderAttempts.stream()
                                .sorted(Comparator.comparing(this::getSessionDate))
                                .toList();

                        Attempts firstAttempt = ordered.get(0);
                        LocalDate firstDate = getSessionDate(firstAttempt);

                        boolean isFlash = firstAttempt.isDone()
                                && firstAttempt.getTries() == 1;

                        // Gesamt-Flashrate: alle einzigartigen Boulder.
                        overallBoulders++;

                        if (isFlash) {
                        overallFlashes++;
                        }

                        // Nur Boulder, deren erster Versuch vor dem 14-Tage-Zeitraum lag,
                        // zählen zur bisherigen Flashrate.
                        if (firstDate.isBefore(recentStart)) {
                        previousBoulders++;

                        if (isFlash) {
                                previousFlashes++;
                        }
                        } else {
                        recentBoulders++;
                        }
                }

                // Es braucht eine aussagekräftige Basis und neue Boulder.
                if (previousBoulders == 0 || recentBoulders == 0) {
                        return Optional.empty();
                }

                double previousFlashrate =
                        100.0 * previousFlashes / previousBoulders;

                double overallFlashrate =
                        100.0 * overallFlashes / overallBoulders;

                double improvement = overallFlashrate - previousFlashrate;

                if (improvement < FLASHRATE_MINIMUM_IMPROVEMENT) {
                        return Optional.empty();
                }

                HighlightResponseDTO highlight = new HighlightResponseDTO();
                highlight.setType(HighlightType.FLASHRATE);
                highlight.setTitle("Deine Flashrate verbessert sich!");
                highlight.setDescription(String.format(
                        Locale.GERMAN,
                        "Deine Overall-Flashrate ist von %.1f %% auf %.1f %% gestiegen – eine Verbesserung um %.1f Prozentpunkte in den letzten 14 Tagen.",
                        previousFlashrate,
                        overallFlashrate,
                        improvement
                ));
                highlight.setDate(latestDate);
                highlight.setSignificance(Math.min(56.0, 36.0 + improvement));

                return Optional.of(highlight);
        }

        // =========================================================
        // HARD SEND - DYNAMIC DELTA
        // =========================================================

        /**
        * Bestimmt dynamisch, wie weit ein Send unterhalb
        * des MaxGrades noch als "Hard Send" gilt.
        *
        * Grundlage ist die Grade-Verteilung im aktuellen
        * 14-Tage-Zeitraum.
        *
        * Je dichter die Leistungen am MaxGrade liegen,
        * desto kleiner wird das erlaubte Delta.
        *
        * Je weiter die Verteilung gestreut ist,
        * desto größer darf das Delta werden.
        */
        private int calculateAllowedHardSendDelta(
                Grade maxGrade,
                List<Attempts> recentSuccessfulAttempts) {

                if (maxGrade == null
                        || recentSuccessfulAttempts.isEmpty()) {
                return 1;
                }

                List<Integer> gradeValues =
                        recentSuccessfulAttempts.stream()
                                .map(a ->
                                        getGradeValue(
                                                a.getBoulder().getGrade()
                                        )
                                )
                                .sorted()
                                .toList();

                if (gradeValues.isEmpty()) {
                return 1;
                }

                int maxValue =
                        getGradeValue(maxGrade);

                /*
                * Wir betrachten die Verteilung relativ zum MaxGrade.
                */
                List<Integer> deltas =
                        gradeValues.stream()
                                .map(value ->
                                        Math.max(
                                                0,
                                                maxValue - value
                                        )
                                )
                                .toList();

                /*
                * 75%-Quantil der beobachteten Delta-Verteilung.
                *
                * Dadurch werden typische Sends berücksichtigt,
                * aber extreme Ausreißer nach unten nicht automatisch
                * zu Hard Sends.
                */
                int index =
                        Math.min(
                                deltas.size() - 1,
                                (int) Math.floor(
                                        deltas.size() * 0.75
                                )
                        );

                int densityDelta =
                        deltas.get(index);

                /*
                * Mindest-/Maximalgrenzen verhindern extreme Ergebnisse.
                */
                return Math.max(
                        1,
                        Math.min(
                                3,
                                densityDelta
                        )
                );
        }

        /**
         * Wie häufig wurde ein bestimmter Grad
         * innerhalb des Recent-Zeitraums geschafft?
         *
         * Rückgabe:
         *
         * 0.0 = sehr häufig
         * 1.0 = sehr selten
         */
        private double calculateGradeRarity(
                Grade grade,
                List<Attempts> attempts) {

                if (attempts.isEmpty()) {
                return 0.0;
                }

                long count =
                        attempts.stream()
                                .filter(a ->
                                        a.getBoulder().getGrade()
                                                == grade
                                )
                                .count();

                return 1.0
                        - ((double) count / attempts.size());
        }

        private double calculateHardSendSignificance(Grade grade, Grade maxGrade, int delta, double rarity) {

                int gradeValue = getGradeValue(grade);

                /*
                * Basis.
                */
                double significance = 70;

                /*
                * Absolute Schwierigkeit.
                */
                significance += Math.min(20, gradeValue * 0.8);

                /*
                * Nähe zum persönlichen Maximum.
                *
                * Delta 0 = sehr stark
                * Delta 1 = stark
                * Delta 2 = gut
                * Delta 3 = eher schwach
                */
                significance += Math.max(0, 15 - delta * 5);

                /*
                * Seltenheit des Grades.
                */
                significance += rarity * 10;

                return Math.min(100, significance);
        }

        // =========================================================
        // PROJECT SIGNIFICANCE
        // =========================================================

        private int calculateProjectMinimumTries(
                Grade grade) {

                int value = getGradeValue(grade);

                /*
                * Der Schwellenwert steigt mit dem Schwierigkeitsgrad.
                *
                * 4+  -> 4 Versuche
                * 5   -> 5
                * 6a  -> 7
                * 7a  -> 10
                * usw.
                */
                return Math.max(4, 3 + (int) Math.ceil(value * 0.35));
        }

        private double calculateProjectDifficultyScore(Grade grade, int totalTries) {

                int minimum = calculateProjectMinimumTries(grade);

                if (minimum == 0) {
                        return 0;
                }
                return (double) totalTries / minimum;
        }

        private double calculateProjectSignificance(
                Grade grade,
                int totalTries,
                int sessions,
                long durationDays) {

                int gradeValue = getGradeValue(grade);

                double significance = 60;

                significance += Math.min(20, gradeValue * 0.8);

                /*
                * Je stärker die Versuche den
                * gradeabhängigen Schwellenwert überschreiten,
                * desto relevanter.
                */
                double minimum = calculateProjectMinimumTries(grade);

                significance += Math.min(20, (totalTries / minimum) * 8);

                /*
                * Mehrere Sessions zeigen echtes Projektverhalten.
                */
                significance += Math.min(5, Math.max(0, sessions - 1));

                /*
                * Sehr lange Projekte bekommen einen kleinen Bonus.
                */
                significance += Math.min(5, durationDays / 30.0);

                return Math.min(72, significance);
        }

        // =========================================================
        // ATTRIBUTE SIGNIFICANCE
        // =========================================================

        private double calculateAttributeSignificance(int delta, int gradeValue) {
                return Math.min(70, 55 + delta * 10 + Math.min(20, gradeValue * 0.8));
        }

        // =========================================================
        // GRADE HELPERS
        // =========================================================

        private Grade getMaxGrade(
                List<Attempts> attempts) {

                return attempts.stream()
                        .map(a ->
                                a.getBoulder().getGrade()
                        )
                        .filter(Objects::nonNull)
                        .max(this::compareGrades)
                        .orElse(null);
        }

        private Grade getLowestGrade() {
                return Grade.FB1;
        }

        private int getBoulderGradeValue(
                Attempts attempt) {

                return getGradeValue(
                        attempt.getBoulder().getGrade()
                );
        }

        private int compareGrades(
                Grade grade1,
                Grade grade2) {

                return Integer.compare(
                        getGradeValue(grade1),
                        getGradeValue(grade2)
                );
        }

        private int getGradeValue(
                Grade grade) {

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

        // =========================================================
        // GENERAL HELPERS
        // =========================================================

        private boolean isFlash(
                Attempts attempt) {

                return attempt.isDone()
                        && attempt.getTries() == 1;
        }

        private LocalDate getSessionDate(
                Attempts attempt) {

                if (attempt.getSession() == null
                        || attempt.getSession().getDate() == null) {

                return LocalDate.MIN;
                }

                return attempt.getSession().getDate();
        }

        private Long getSessionId(
                Attempts attempt) {

                if (attempt.getSession() == null) {
                return Long.MIN_VALUE;
                }

                return attempt.getSession().getId();
        }

        private Long getAttemptId(
                Attempts attempt) {

                if (attempt.getId() == null) {
                return Long.MIN_VALUE;
                }

                return attempt.getId();
        }

        private String describeGradeDelta(
                int delta) {

                if (delta == 0) {
                return "auf deinem aktuellen MaxGrade";
                }

                if (delta == 1) {
                return "nur einen Grad unter deinem aktuellen MaxGrade";
                }

                return delta
                        + " Grade unter deinem aktuellen MaxGrade";
        }

        private double calculateNewMaxGradeSignificance(
                Grade grade) {

                return Math.min(100, 80 + getGradeValue(grade) * 0.8);
        }

        private int compareAttributeCandidates(
                AttributeCandidate a,
                AttributeCandidate b) {

                int result =
                        Integer.compare(
                                a.delta(),
                                b.delta()
                        );

                if (result != 0) {
                return result;
                }

                result =
                        Integer.compare(
                                a.newGradeValue(),
                                b.newGradeValue()
                        );

                if (result != 0) {
                return result;
                }

                return a.date().compareTo(b.date());
        }

        // =========================================================
        // DTO CREATION
        // =========================================================

        private HighlightResponseDTO createHighlight(
                HighlightType type,
                String title,
                String description,
                Attempts attempt,
                double significance) {

                HighlightResponseDTO dto = new HighlightResponseDTO();

                dto.setType(type);
                dto.setTitle(title);
                dto.setDescription(description);

                dto.setDate(
                        getSessionDate(attempt)
                );

                if (attempt.getBoulder() != null
                        && attempt.getBoulder().getGrade() != null) {

                dto.setGrade(
                        attempt.getBoulder()
                                .getGrade()
                                .getDisplayName()
                );
                }

                dto.setTries(attempt.getTries());

                dto.setSignificance(Math.min(100, significance));

                if (attempt.getSession() != null) {
                        dto.setSessionId(attempt.getSession().getId());
                        dto.setAttemptId(attempt.getId());
                }

                return dto;
        }

        //Methode für Session Stats. Listet alle Highlights auf die von dieser Session erzeugt wurden.
        public List<HighlightResponseDTO> getHighlightsCreatedBySession(Session session, User user) {
                return getHighlights(user).stream()
                        .filter(highlight ->
                                Objects.equals(
                                        highlight.getSessionId(),
                                        session.getId()
                                )
                        )
                        .toList();
        }

        // =========================================================
        // INTERNAL DATA CLASSES
        // =========================================================

        private record ProjectCandidate(
                Grade grade,
                int totalTries,
                int projectSessions,
                LocalDate projectStart,
                LocalDate sendDate,
                Attempts successfulAttempt,
                long projectDurationDays,
                double projectDifficultyScore
        ) {
        }

        private record SessionTriesCandidate(
                Long sessionId,
                LocalDate date,
                int totalTries
        ) {
        }

        private record SessionFunCandidate(
                Long sessionId,
                LocalDate date,
                int attempts
        ) {
        }

        private int compareSessionIds(Long id1, Long id2) {
                if (id1 == null && id2 == null) {
                        return 0;
                }
                if (id1 == null) {
                        return -1;
                }
                if (id2 == null) {
                        return 1;
                }
                return id1.compareTo(id2);
        }

        private record SessionPeakCandidate(
                Long sessionId,
                LocalDate date,
                int hardSendCount
        ) {
        }

        private record SessionProjectCandidate(
                Long sessionId,
                LocalDate date,
                boolean hasHardProject,
                boolean mostlyNonTops,
                int hardProjectAttempts,
                long nonTops,
                long tops
        ) {
        }

        private record SessionHotStreakCandidate(
                String title,
                String description,
                int streakLength,
                double significance
        ) {
        }
}