package boulderino.boulderino.service;

import boulderino.boulderino.dto.StatResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.SessionRepository;
import org.springframework.stereotype.Service;
import boulderino.boulderino.entity.Session;

import java.time.LocalDate;
import java.util.List;

@Service
public class StatsService {

        private final SessionRepository sessionRepository;
        private final AttemptsRepository attemptsRepository;

        public StatsService(SessionRepository sessionRepository, AttemptsRepository attemptsRepository){
                this.sessionRepository = sessionRepository;
                this.attemptsRepository = attemptsRepository;
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

        //TODO: eigener DTO für SessionStats macht Sinn
        public StatResponseDTO getSessionStats(Long sessionId, User user) {

                Session session = sessionRepository.findById(sessionId)
                        .filter(s -> s.getUser().equals(user))
                        .orElseThrow();

                List<Attempts> attempts = getSessionAttempts(session);

                StatResponseDTO response = new StatResponseDTO();

                response.setTotalBoulders(calculateTotalBoulders(attempts));
                response.setTotalTries(calculateTotalTries(attempts));
                response.setTotalTops(calculateTotalTops(attempts));
                response.setTotalZones(calculateTotalZones(attempts));
                response.setSuccessRate(calculateSuccessRate(attempts));

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

        private List<Attempts> getUserAttempts(User user) {
                return attemptsRepository.findBySessionUser(user);
        }

        private List<Attempts> getSessionAttempts(Session session) {

                return attemptsRepository.findBySession(session);
        }

}