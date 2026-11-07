package boulderino.boulderino.service;

import boulderino.boulderino.dto.ProgressOverTimePointDTO;
import boulderino.boulderino.dto.ProgressOverTimeResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressOverTimeService {

    private final AttemptsRepository attemptsRepository;

    public ProgressOverTimeService(
            AttemptsRepository attemptsRepository) {
        this.attemptsRepository = attemptsRepository;
    }

    public ProgressOverTimeResponseDTO getProgressOverTime(User user) {

        List<Attempts> attempts =
                attemptsRepository.findBySessionUser(user);

        // Versuche nach Session gruppieren
        Map<Long, List<Attempts>> attemptsBySession = new HashMap<>();
        Map<Long, Session> sessionsById = new HashMap<>();

        for (Attempts attempt : attempts) {
            Session session = attempt.getSession();

            if (session == null || session.getId() == null) {
                continue;
            }

            attemptsBySession
                    .computeIfAbsent(
                            session.getId(),
                            id -> new ArrayList<>())
                    .add(attempt);

            sessionsById.put(session.getId(), session);
        }

        List<ProgressOverTimePointDTO> points = new ArrayList<>();

        for (Map.Entry<Long, List<Attempts>> entry
                : attemptsBySession.entrySet()) {

            Session session = sessionsById.get(entry.getKey());

            if (session.getDate() == null) {
                continue;
            }

            List<Attempts> sessionAttempts = entry.getValue();

            Grade maxGrade = null;
            Grade maxFlashGrade = null;
            Grade maxProjectZoneGrade = null;

            // Boulder, die in dieser Session getoppt wurden
            Map<Long, Boolean> toppedByBoulder = new HashMap<>();

            for (Attempts attempt : sessionAttempts) {
                Boulder boulder = attempt.getBoulder();

                if (boulder == null || boulder.getGrade() == null) {
                    continue;
                }

                Long boulderId = boulder.getId();

                if (boulderId == null) {
                    continue;
                }

                if (attempt.isDone()) {
                    toppedByBoulder.put(boulderId, true);

                    maxGrade = higherGrade(
                            maxGrade,
                            boulder.getGrade());
                }

                // Annahme: Ein erfolgreicher Versuch mit genau
                // einem Versuch entspricht einem Flash.
                if (attempt.isDone() && attempt.getTries() == 1) {
                    maxFlashGrade = higherGrade(
                            maxFlashGrade,
                            boulder.getGrade());
                }
            }

            // Höchster Boulder mit Zone, der in dieser Session
            // nicht auch getoppt wurde.
            for (Attempts attempt : sessionAttempts) {
                Boulder boulder = attempt.getBoulder();

                if (boulder == null || boulder.getGrade() == null) {
                    continue;
                }

                Long boulderId = boulder.getId();

                if (boulderId == null) {
                    continue;
                }

                boolean topped = Boolean.TRUE.equals(
                        toppedByBoulder.get(boulderId));

                if (attempt.isZone() && !topped) {
                    maxProjectZoneGrade = higherGrade(
                            maxProjectZoneGrade,
                            boulder.getGrade());
                }
            }

            points.add(new ProgressOverTimePointDTO(
                    session.getDate(),

                    displayName(maxGrade),
                    gradeValue(maxGrade),

                    displayName(maxFlashGrade),
                    gradeValue(maxFlashGrade),

                    displayName(maxProjectZoneGrade),
                    gradeValue(maxProjectZoneGrade)
            ));
        }

        points.sort(
                Comparator.comparing(
                        ProgressOverTimePointDTO::getDate));

        return new ProgressOverTimeResponseDTO(points);
    }

    private Grade higherGrade(Grade current, Grade candidate) {
        if (current == null || candidate.ordinal() > current.ordinal()) {
            return candidate;
        }

        return current;
    }

    private String displayName(Grade grade) {
        return grade == null ? null : grade.getDisplayName();
    }

    private Integer gradeValue(Grade grade) {
        return grade == null ? null : grade.ordinal() + 1;
    }
}