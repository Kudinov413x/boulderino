package boulderino.boulderino.service;

import boulderino.boulderino.dto.AttemptsCreateDTO;
import boulderino.boulderino.dto.AttemptsResponseDTO;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.BoulderRepository;
import boulderino.boulderino.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AttemptsService {

    private final AttemptsRepository attemptsRepository;
    private final BoulderRepository boulderRepository;
    private final SessionRepository sessionRepository;

    public AttemptsService(
            AttemptsRepository attemptsRepository,
            BoulderRepository boulderRepository,
            SessionRepository sessionRepository) {

        this.attemptsRepository = attemptsRepository;
        this.boulderRepository = boulderRepository;
        this.sessionRepository = sessionRepository;
    }

    public List<AttemptsResponseDTO> getAllAttemptsByUser(User user) {
        return attemptsRepository.findBySessionUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AttemptsResponseDTO getAttemptById(Long id, User user) {
        Attempts attempts = getAttemptEntityById(id, user);

        return toResponseDTO(attempts);
    }

    // Absichtlich Entity zurückgeben:
    // zum Debuggen
    public Attempts createAttempt(
            AttemptsCreateDTO request,
            User user) {

        Session session = sessionRepository.findById(request.getSessionId())
                .filter(s -> s.getUser().equals(user))
                .orElseThrow();

        Boulder boulder = boulderRepository.findById(request.getBoulderId())
                .filter(b -> b.getUser().equals(user))
                .orElseThrow();

        Attempts attempts = new Attempts();

        attempts.setTries(request.getTries());
        attempts.setDone(request.isDone());
        attempts.setZone(request.isZone());
        attempts.setSession(session);
        attempts.setBoulder(boulder);

        Attempts savedAttempt = attemptsRepository.save(attempts);

        updateFirstTryProgress(boulder);

        return savedAttempt;
    }

    public AttemptsResponseDTO updateAttempt(Long id, AttemptsResponseDTO request,User user){

        Attempts existingAttempt = getAttemptEntityById(id, user);
        Boulder oldBoulder = existingAttempt.getBoulder();

        if (request.getTries() != null) {
            existingAttempt.setTries(request.getTries());
        }
        if (request.getDone() != null) {
            existingAttempt.setDone(request.getDone());
        }
        if (request.getZone() != null) {
            existingAttempt.setZone(request.getZone());
        }
        if (request.getBoulderId() != null) {
            Boulder newBoulder = boulderRepository
                    .findById(request.getBoulderId())
                    .filter(b -> b.getUser().equals(user))
                    .orElseThrow();
            existingAttempt.setBoulder(newBoulder);
        }

        Attempts updatedAttempt = attemptsRepository.save(existingAttempt);

        updateFirstTryProgress(oldBoulder);
        updateFirstTryProgress(updatedAttempt.getBoulder());

        return toResponseDTO(updatedAttempt);
    }

    public void deleteAttempt(Long id, User user) {
        Attempts existingAttempt = getAttemptEntityById(id, user);
        attemptsRepository.delete(existingAttempt);
        updateFirstTryProgress(existingAttempt.getBoulder());
    }

    private AttemptsResponseDTO toResponseDTO(Attempts attempts) {

        AttemptsResponseDTO response = new AttemptsResponseDTO();

        response.setId(attempts.getId());
        response.setTries(attempts.getTries());
        response.setDone(attempts.isDone());
        response.setZone(attempts.isZone());
        response.setSessionId(attempts.getSession().getId());
        response.setBoulderId(attempts.getBoulder().getId());

        return response;
    }

    private Attempts getAttemptEntityById(Long id, User user) {

        return attemptsRepository.findById(id)
                .filter(attempt ->
                        attempt.getSession().getUser().equals(user))
                .orElseThrow();
    }

    // überprüfe ob die Route zum ersten mal geschafft wurde und wenn ja vermerke das im Boulder
    // überprüfe zusätzlich ob es sich um einen Flash handelt
    private void updateFirstTryProgress(Boulder boulder) {

        List<Attempts> attempts = attemptsRepository.findByBoulder(boulder);
        
        //Für Delete
        if (attempts.isEmpty()) {
            boulder.setFirstAscent(false);
            boulder.setFirstAscentDate(null);
            boulder.setFlashed(false);
            boulderRepository.save(boulder);
            return;
        }

        // FIRST ASCENT
        Attempts firstSuccessfulAttempt = attempts.stream()
                .filter(attempt ->
                        Boolean.TRUE.equals(attempt.isDone()))
                .min(Comparator.comparing(Attempts::getCreatedAt))
                .orElse(null);
        if (firstSuccessfulAttempt != null) {
            boulder.setFirstAscent(true);
            boulder.setFirstAscentDate(
                    firstSuccessfulAttempt
                            .getSession()
                            .getDate()
            );
        } else {
            // Es gibt Attempts, aber keinen erfolgreichen
            boulder.setFirstAscent(false);
            boulder.setFirstAscentDate(null);
        }

        // FLASH
        Attempts firstAttempt = attempts.stream()
                .min(Comparator.comparing(Attempts::getCreatedAt))
                .orElseThrow();

        boolean isFlash = firstAttempt.getTries() == 1 && Boolean.TRUE.equals(firstAttempt.isDone());
        boulder.setFlashed(isFlash);

        // Alle Änderungen speichern
        boulderRepository.save(boulder);
    }

    public void recalculateProgressForUser(User user) {

    List<Boulder> boulders = boulderRepository.findByUser(user);

    for (Boulder boulder : boulders) {
        updateFirstTryProgress(boulder);
    }
}
}