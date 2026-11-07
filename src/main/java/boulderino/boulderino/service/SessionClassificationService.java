package boulderino.boulderino.service;

import boulderino.boulderino.dto.SessionStatsDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.BoulderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SessionClassificationService {

    public static final String HIGH_VOLUME = "HIGH_VOLUME";
    public static final String PROJECT = "PROJECT";
    public static final String LIMIT = "LIMIT";
    public static final String FLASHES = "FLASHES";
    public static final String TECHNIQUE = "TECHNIQUE";
    public static final String FUN = "FUN";
    public static final String DEFAULT = "DEFAULT";

    private static final int HIGH_VOLUME_MIN_BOULDERS = 12;
    private static final int PROJECT_MIN_TRIES = 6;
    private static final int FUN_MAX_TRIES = 6;
    private static final int FUN_MIN_GRADE_DIFFERENCE = 3;

    private static final double HIGH_VOLUME_MIN_SUCCESS_RATE = 60.0;
    private static final double FLASHES_MIN_RATE = 60.0;
    private static final double TECHNIQUE_MIN_RATE = 60.0;
    private static final double LIMIT_MAX_SUCCESS_RATE = 40.0;

    private final AttemptsRepository attemptsRepository;
    private final BoulderRepository boulderRepository;

    public SessionClassificationService(AttemptsRepository attemptsRepository, BoulderRepository boulderRepository) {
        this.attemptsRepository = attemptsRepository;
        this.boulderRepository = boulderRepository;
    }

    public List<String> classify(Session session, List<Attempts> sessionAttempts, SessionStatsDTO stats, User user ) {
        List<String> labels = new ArrayList<>();

        if (isHighVolume(stats)) {
            labels.add(HIGH_VOLUME);
        }

        if (hasProject(sessionAttempts)) {
            labels.add(PROJECT);
        }

        if (isLimit(session, sessionAttempts, stats, user)) {
            labels.add(LIMIT);
        }

        if (isFlashSession(stats)) {
            labels.add(FLASHES);
        }

        if (isTechniqueSession(session, sessionAttempts)) {
            labels.add(TECHNIQUE);
        }

        if (isFunSession(sessionAttempts)) {
            labels.add(FUN);
        }

        if (labels.isEmpty()) {
            labels.add(DEFAULT);
        }

        return labels;
    }

    private boolean isHighVolume(SessionStatsDTO stats) {
        return stats.getSessionBoulders() >= HIGH_VOLUME_MIN_BOULDERS
                && stats.getSuccessRate() >= HIGH_VOLUME_MIN_SUCCESS_RATE;
    }

    private boolean hasProject(List<Attempts> attempts) {
        return attempts.stream()
                .anyMatch(attempt ->
                        attempt.getTries() >= PROJECT_MIN_TRIES
                );
    }

    private boolean isLimit(
            Session session,
            List<Attempts> attempts,
            SessionStatsDTO stats,
            User user
    ) {
        if (stats.getSuccessRate() > LIMIT_MAX_SUCCESS_RATE) {
            return false;
        }

        Grade personalMaxGrade = getPersonalMaxGradeBeforeSession(
                session,
                user
        );

        if (personalMaxGrade == null) {
            return false;
        }

        int targetGradeIndex = personalMaxGrade.ordinal() - 1;

        if (targetGradeIndex < 0) {
            return false;
        }

        return attempts.stream()
                .map(Attempts::getBoulder)
                .filter(boulder -> boulder != null)
                .map(Boulder::getGrade)
                .filter(grade -> grade != null)
                .anyMatch(grade -> grade.ordinal() >= targetGradeIndex);
    }

    private Grade getPersonalMaxGradeBeforeSession(
            Session session,
            User user
    ) {
        return boulderRepository.findByUser(user)
                .stream()
                .filter(boulder ->
                        Boolean.TRUE.equals(boulder.getFirstAscent())
                )
                .filter(boulder ->
                        boulder.getFirstAscentDate() != null
                )
                .filter(boulder ->
                        boulder.getFirstAscentDate().isBefore(session.getDate())
                )
                .map(Boulder::getGrade)
                .filter(grade -> grade != null)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(null);
    }

    private boolean isFlashSession(SessionStatsDTO stats) {
        return stats.getFlashRate() >= FLASHES_MIN_RATE;
    }

    private boolean isTechniqueSession(Session session, List<Attempts> attempts) {
        List<Boulder> boulders = attempts.stream()
                .filter(attempt -> attempt.getTries() >= 1)
                .map(Attempts::getBoulder)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Boulder::getId,
                        boulder -> boulder,
                        (existing, duplicate) -> existing
                ))
                .values()
                .stream()
                .toList();

        if (boulders.isEmpty()) {
            return false;
        }

        long previouslyToppedBoulders = boulders.stream()
                .filter(boulder -> boulder.getFirstAscentDate() != null)
                .filter(boulder -> boulder.getFirstAscentDate()
                        .isBefore(session.getDate()))
                .count();

        double rate = (double) previouslyToppedBoulders / boulders.size() * 100;

        return rate >= TECHNIQUE_MIN_RATE;
    }

    private boolean isFunSession(List<Attempts> attempts) {
        List<Grade> grades = attempts.stream()
                .filter(attempt -> attempt.getTries() >= 1)
                .map(Attempts::getBoulder)
                .filter(boulder -> boulder != null)
                .map(Boulder::getGrade)
                .filter(grade -> grade != null)
                .distinct()
                .toList();

        if (grades.size() < 2) {
            return false;
        }

        int minGrade = grades.stream()
                .mapToInt(Enum::ordinal)
                .min()
                .orElse(0);

        int maxGrade = grades.stream()
                .mapToInt(Enum::ordinal)
                .max()
                .orElse(0);

        boolean sufficientGradeDifference =
                maxGrade - minGrade >= FUN_MIN_GRADE_DIFFERENCE;

        boolean maxTriesNotExceeded = attempts.stream()
                .allMatch(attempt -> attempt.getTries() <= FUN_MAX_TRIES);

        return sufficientGradeDifference && maxTriesNotExceeded;
    }
}