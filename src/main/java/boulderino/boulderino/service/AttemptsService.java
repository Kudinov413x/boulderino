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

        return attemptsRepository.save(attempts);
    }

    public AttemptsResponseDTO updateAttempt(
            Long id,
            AttemptsResponseDTO request,
            User user) {

        Attempts existingAttempt = getAttemptEntityById(id, user);

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
            Boulder boulder = boulderRepository.findById(request.getBoulderId())
                    .filter(b -> b.getUser().equals(user))
                    .orElseThrow();
            existingAttempt.setBoulder(boulder);
        }

        Attempts updatedAttempt =
                attemptsRepository.save(existingAttempt);

        return toResponseDTO(updatedAttempt);
    }

    public void deleteAttempt(Long id, User user) {
        Attempts existingAttempt = getAttemptEntityById(id, user);

        attemptsRepository.delete(existingAttempt);
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
}