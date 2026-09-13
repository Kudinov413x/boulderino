package boulderino.boulderino.service;

import boulderino.boulderino.dto.SessionUpdateDTO;
import boulderino.boulderino.dto.SessionCreateDTO;
import boulderino.boulderino.dto.SessionResponseDTO;
import boulderino.boulderino.dto.SessionDetailsResponseDTO;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.SessionRepository;
import boulderino.boulderino.repository.AttemptsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final AttemptsRepository attemptsRepository;

    public SessionService(SessionRepository sessionRepository, AttemptsRepository attemptsRepository) {
        this.sessionRepository = sessionRepository;
        this.attemptsRepository = attemptsRepository;
    }

    // Alle Sessions eines Users
    public List<SessionResponseDTO> getAllSessionsByUser(User user) {

        return sessionRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Eine Session eines Users anhand der ID finden
    public SessionResponseDTO getSessionById(Long id, User user) {

        Session session = getSessionEntityById(id, user);

        return toResponseDTO(session);
    }

    // Neue Session für einen User erstellen
    public Session createSession(SessionCreateDTO request, User user) {

        Session session = new Session();

        session.setDate(LocalDate.now());
        session.setLocation(request.getLocation());
        session.setUser(user);

        return sessionRepository.save(session);
    }

    // Session eines Users ändern
    public SessionResponseDTO updateSession(
            Long id,
            SessionUpdateDTO request,
            User user) {

        Session existingSession = getSessionEntityById(id, user);

        if (request.getDate() != null) {
            existingSession.setDate(request.getDate());
        }

        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            existingSession.setLocation(request.getLocation());
        }

        Session updatedSession = sessionRepository.save(existingSession);

        return toResponseDTO(updatedSession);
    }

    // Session eines Users löschen
    public void deleteSession(Long id, User user) {

        Session existingSession = getSessionEntityById(id, user);

        sessionRepository.delete(existingSession);
    }

    // Entity -> ResponseDTO
    private SessionResponseDTO toResponseDTO(Session session) {

        SessionResponseDTO response = new SessionResponseDTO();

        response.setId(session.getId());
        response.setDate(session.getDate());
        response.setLocation(session.getLocation());

        return response;
    }

    // Session Entity anhand ID und User holen
    private Session getSessionEntityById(Long id, User user) {

        return sessionRepository.findById(id)
                .filter(session -> session.getUser().equals(user))
                .orElseThrow();
    }

    //Infos über alle Attempts innerhalb einer Session angeben
    public List<SessionDetailsResponseDTO> getSessionDetails(Long sessionId, User user) {

        // Prüfen, ob die Session existiert
        // und dem eingeloggten User gehört
        sessionRepository.findById(sessionId)
                .filter(session -> session.getUser().equals(user))
                .orElseThrow();

        return attemptsRepository
                .findBySessionIdAndSessionUser(sessionId, user)
                .stream()
                .map(this::toSessionDetailsResponseDTO)
                .toList();
    }

    private SessionDetailsResponseDTO toSessionDetailsResponseDTO(Attempts attempt) {

        SessionDetailsResponseDTO response = new SessionDetailsResponseDTO();

        response.setAttemptId(attempt.getId());
        response.setTries(attempt.getTries());
        response.setZone(attempt.isZone());
        response.setDone(attempt.isDone());

        Boulder boulder = attempt.getBoulder();

        response.setBoulderId(boulder.getId());
        response.setBoulderName(boulder.getName());
        response.setGrade(boulder.getGrade());
        response.setGripType(boulder.getGripType());
        response.setWallAngle(boulder.getWallAngle());
        response.setRouteCharacter(boulder.getRouteCharacter());
        response.setClimbingStyle(boulder.getClimbingStyle());

        return response;
    }

}