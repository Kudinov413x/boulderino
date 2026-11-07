package boulderino.boulderino.service;

import boulderino.boulderino.dto.DashboardStatDTO;
import boulderino.boulderino.dto.GradeDistributionDTO;
import boulderino.boulderino.dto.GradeDistributionResponseDTO;
import boulderino.boulderino.dto.StatResponseDTO;
import boulderino.boulderino.dto.UntoppedGradeDistributionDTO;
import boulderino.boulderino.dto.UntoppedGradeDistributionResponseDTO;
import boulderino.boulderino.dto.SessionStatsDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.SessionRepository;
import org.springframework.stereotype.Service;
import boulderino.boulderino.entity.Session;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StatsService {

        private final SessionRepository sessionRepository;
        private final AttemptsRepository attemptsRepository;
        private final SessionClassificationService sessionClassificationService;

        public StatsService(SessionRepository sessionRepository, AttemptsRepository attemptsRepository, SessionClassificationService sessionClassificationService) {
                this.sessionRepository = sessionRepository;
                this.attemptsRepository = attemptsRepository;
                this.sessionClassificationService = sessionClassificationService;
        }

        // Zusammenfassung aller Statistiken in eine DTO für den Controller
        public StatResponseDTO getStats(User user) {
                List<Attempts> attempts = getUserAttempts(user);
                StatResponseDTO response = new StatResponseDTO();

                response.setTotalSessions(calculateTotalSessions(user));
                response.setSessionsThisWeek(calculateSessionsThisWeek(user));
                response.setSessionsThisMonth(calculateSessionsThisMonth(user));
                response.setTotalBoulders(calculateTotalBoulders(attempts));
                response.setTotalTries(calculateTotalTries(attempts));
                response.setTotalTops(calculateTotalTops(attempts));
                response.setTotalZones(calculateTotalZones(attempts));
                response.setSuccessRate(calculateSuccessRate(attempts));

                return response;
        }

        public SessionStatsDTO getSessionStats(Long sessionId, User user) {
                Session session = sessionRepository.findById(sessionId)
                        .filter(s -> s.getUser().equals(user))
                        .orElseThrow();
                List<Attempts> attempts = getSessionAttempts(session);
                SessionStatsDTO response = new SessionStatsDTO();

                response.setSessionBoulders(calculateTotalBoulders(attempts));
                response.setSessionTries(calculateTotalTries(attempts));
                response.setSessionTops(calculateTotalTops(attempts));
                response.setSessionZones(calculateTotalZones(attempts));
                response.setSuccessRate(calculateSuccessRate(attempts));
                response.setSessionFlashes(calculateTotalFlashes(attempts));
                response.setFlashRate(calculateFlashRate(attempts));

                response.setLabels(
                        sessionClassificationService.classify(
                                session,
                                attempts,
                                response,
                                user
                        )
                );

                return response;
        }

        public DashboardStatDTO getDashboardStats(User user) {
                DashboardStatDTO response = new DashboardStatDTO();
                LocalDate today = LocalDate.now();
                // Heute + die 29 vorherigen Tage = 30 Tage
                LocalDate startDate = today.minusDays(29);
                response.setSessionsLastThirtyDays(sessionRepository.countByUserAndDateBetween(user, startDate, today));
                response.setTopsLastThirtyDays(attemptsRepository.countUniqueTopsLastThirtyDays(user, startDate, today));
                response.setProjectsLastThirtyDays(attemptsRepository.countUniqueProjectsLastThirtyDays(user, startDate, today));
                response.setTotalTriesLastThirtyDays(attemptsRepository.sumTriesLastThirtyDays(user, startDate, today));
                return response;
        }

        private long calculateTotalSessions(User user) {
                return sessionRepository.countByUser(user);
        }

        private long calculateSessionsThisWeek(User user) {
                LocalDate today = LocalDate.now();
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                LocalDate endOfWeek = startOfWeek.plusDays(6);
                return sessionRepository.countByUserAndDateBetween(user, startOfWeek, endOfWeek);
        }

        private long calculateSessionsThisMonth(User user) {
                LocalDate today = LocalDate.now();
                LocalDate startOfMonth = today.withDayOfMonth(1);
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                return sessionRepository.countByUserAndDateBetween(user, startOfMonth, endOfMonth);
        }

        private long calculateTotalBoulders(List<Attempts> attempts) {
                return attempts.stream()
                        .filter(attempt -> attempt.getTries() >= 1)
                        .map(attempt -> attempt.getBoulder().getId())
                        .distinct()
                        .count();
        }

        private long calculateTotalTries(List<Attempts> attempts) {
                return attempts.stream()
                        .mapToLong(attempt -> attempt.getTries())
                        .sum();
        }

        private long calculateTotalFlashes(List<Attempts> attempts) {
                return attempts.stream()
                        .filter(attempt -> Boolean.TRUE.equals(attempt.isDone()))
                        .filter(attempt -> attempt.getTries() == 1)
                        .map(attempt -> attempt.getBoulder().getId())
                        .distinct()
                        .count();
        }

        private long calculateTotalTops(List<Attempts> attempts) {
                return attempts.stream()
                        .filter(attempt ->
                                Boolean.TRUE.equals(attempt.isDone()))
                        .map(attempt -> attempt.getBoulder().getId())
                        .distinct()
                        .count();
        }

        private long calculateTotalZones(List<Attempts> attempts) {
                return attempts.stream()
                        .filter(attempt ->
                                Boolean.TRUE.equals(attempt.isZone()))
                        .map(attempt -> attempt.getBoulder().getId())
                        .distinct()
                        .count();
        }

        private double calculateSuccessRate(List<Attempts> attempts) {
                long totalBoulders = calculateTotalBoulders(attempts);
                if (totalBoulders == 0) {
                        return 0.0;
                }
                long totalTops = calculateTotalTops(attempts);
                return (double) totalTops / totalBoulders * 100;
        }

        private double calculateFlashRate(List<Attempts> attempts) {
                long totalBoulders = calculateTotalBoulders(attempts);
                if (totalBoulders == 0) {
                        return 0.0;
                }
                long totalFlashes = calculateTotalFlashes(attempts);
                return (double) totalFlashes / totalBoulders * 100;
        }

        //Methode to calculate the Grade Distribution of a specific Session
        public GradeDistributionResponseDTO getSessionGradeDistribution(Long sessionId, User user) {
                Session session = sessionRepository.findById(sessionId)
                        .filter(s -> s.getUser().equals(user))
                        .orElseThrow();
                List<Attempts> sessionAttempts = getSessionAttempts(session);
                Map<Grade, Long> topsByGrade = new EnumMap<>(Grade.class);
                Set<Grade> triedGrades = EnumSet.noneOf(Grade.class);
                Set<Long> toppedBoulderIds = new HashSet<>();

                for (Attempts attempt : sessionAttempts) {
                        if (attempt.getTries() < 1 || attempt.getBoulder() == null) {
                                continue;
                        }

                        Boulder boulder = attempt.getBoulder();
                        Grade grade = boulder.getGrade();

                        if (grade == null) {
                                continue;
                        }

                        triedGrades.add(grade);

                        if (attempt.isDone() && toppedBoulderIds.add(boulder.getId())) {
                                topsByGrade.merge(grade, 1L, Long::sum);
                        }
                }

                long totalTops = topsByGrade.values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum();

                List<GradeDistributionDTO> gradeDistribution =
                        Arrays.stream(Grade.values())
                                .filter(triedGrades::contains)
                                .map(grade -> {
                                        long topsOfGrade = topsByGrade.getOrDefault(grade, 0L);

                                        double percentage = totalTops > 0
                                                ? (double) topsOfGrade / totalTops * 100
                                                : 0.0;

                                        GradeDistributionDTO dto = new GradeDistributionDTO();
                                        dto.setGrade(grade);
                                        dto.setPercentage(percentage);
                                        dto.setAbsoluteTops(topsOfGrade);

                                        return dto;
                                })
                                .toList();

                GradeDistributionResponseDTO response = new GradeDistributionResponseDTO();

                response.setTotalTops(totalTops);
                response.setGrades(gradeDistribution);

                return response;
        }

        //Methode to claculate the grade distribution of untopped Attempts in a specific session
        public UntoppedGradeDistributionResponseDTO getSessionUntoppedGradeDistribution(Long sessionId, User user) {
                Session session = sessionRepository.findById(sessionId)
                        .filter(s -> s.getUser().equals(user))
                        .orElseThrow();

                List<Attempts> sessionAttempts = getSessionAttempts(session);

                Map<Grade, Long> untoppedByGrade = new EnumMap<>(Grade.class);
                Set<Grade> untoppedGrades = EnumSet.noneOf(Grade.class);
                Set<Long> countedBoulderIds = new HashSet<>();
                Set<Long> zoneBoulderIds = new HashSet<>();

                for (Attempts attempt : sessionAttempts) {

                        if (attempt.getTries() < 1 || attempt.getBoulder() == null) {
                        continue;
                        }

                        Boulder boulder = attempt.getBoulder();
                        Long boulderId = boulder.getId();

                        // Only consider boulders that were not topped
                        if (attempt.isDone()) {
                        continue;
                        }

                        // Count each boulder only once
                        if (!countedBoulderIds.add(boulderId)) {
                        continue;
                        }

                        // Count boulders where the zone was reached
                        if (attempt.isZone()) {
                        zoneBoulderIds.add(boulderId);
                        }

                        Grade grade = boulder.getGrade();

                        if (grade == null) {
                        continue;
                        }

                        untoppedGrades.add(grade);
                        untoppedByGrade.merge(grade, 1L, Long::sum);
                }

                long totalUntopped = countedBoulderIds.size();
                long zoneCount = zoneBoulderIds.size();

                double zoneRate = totalUntopped > 0
                        ? (double) zoneCount / totalUntopped * 100
                        : 0.0;

                List<UntoppedGradeDistributionDTO> gradeDistribution =
                        Arrays.stream(Grade.values())
                                .filter(untoppedGrades::contains)
                                .map(grade -> {
                                        long count = untoppedByGrade.getOrDefault(grade, 0L);

                                        double percentage = totalUntopped > 0
                                                ? (double) count / totalUntopped * 100
                                                : 0.0;

                                        UntoppedGradeDistributionDTO dto =
                                                new UntoppedGradeDistributionDTO();

                                        dto.setGrade(grade);
                                        dto.setPercentage(percentage);
                                        dto.setAbsoluteCount(count);

                                        return dto;
                                })
                                .toList();

                UntoppedGradeDistributionResponseDTO response =
                        new UntoppedGradeDistributionResponseDTO();

                response.setTotalUntopped(totalUntopped);
                response.setZoneCount(zoneCount);
                response.setZoneRate(zoneRate);
                response.setGrades(gradeDistribution);

                return response;
        }

        private List<Attempts> getUserAttempts(User user) {
                return attemptsRepository.findBySessionUser(user);
        }

        private List<Attempts> getSessionAttempts(Session session) {
                return attemptsRepository.findBySession(session);
        }

}