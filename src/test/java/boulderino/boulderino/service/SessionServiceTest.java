package boulderino.boulderino.service;

import boulderino.boulderino.dto.SessionUpdateDTO;
import boulderino.boulderino.dto.SessionResponseDTO;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import boulderino.boulderino.dto.SessionCreateDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

        @Mock
        private SessionRepository sessionRepository;

        @InjectMocks
        private SessionService sessionService;


        @Test
        void getAllSessionsByUser_shouldReturnSessions() {

                User user = new User();
                user.setId(1L);

                Session session1 = new Session();
                session1.setId(1L);
                session1.setDate(LocalDate.of(2026, 9, 4));
                session1.setLocation("Kletterhalle Duisburg");
                session1.setUser(user);

                Session session2 = new Session();
                session2.setId(2L);
                session2.setDate(LocalDate.of(2026, 9, 5));
                session2.setLocation("Kletterhalle Essen");
                session2.setUser(user);

                when(sessionRepository.findByUser(user))
                        .thenReturn(List.of(session1, session2));

                List<SessionResponseDTO> result =
                        sessionService.getAllSessionsByUser(user);

                assertEquals(2, result.size());

                assertEquals(1L, result.get(0).getId());
                assertEquals("Kletterhalle Duisburg", result.get(0).getLocation());
                assertEquals(LocalDate.of(2026, 9, 4), result.get(0).getDate());

                assertEquals(2L, result.get(1).getId());
                assertEquals("Kletterhalle Essen", result.get(1).getLocation());
                assertEquals(LocalDate.of(2026, 9, 5), result.get(1).getDate());

                verify(sessionRepository).findByUser(user);
        }


        @Test
        void getSessionById_shouldReturnSession() {

                User user = new User();
                user.setId(1L);

                Session session = new Session();
                session.setId(1L);
                session.setDate(LocalDate.of(2026, 9, 4));
                session.setLocation("Kletterhalle Duisburg");
                session.setUser(user);

                when(sessionRepository.findById(1L))
                        .thenReturn(Optional.of(session));

                SessionResponseDTO result =
                        sessionService.getSessionById(1L, user);

                assertEquals(1L, result.getId());
                assertEquals("Kletterhalle Duisburg", result.getLocation());
                assertEquals(LocalDate.of(2026, 9, 4), result.getDate());

                verify(sessionRepository).findById(1L);
        }


        @Test
        void createSession_shouldCreateSession() {

        User user = new User();
        user.setId(1L);

        SessionCreateDTO request = new SessionCreateDTO();
        request.setLocation("Kletterhalle Duisburg");

        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Session result =
                sessionService.createSession(request, user);

        assertEquals("Kletterhalle Duisburg", result.getLocation());
        assertEquals(LocalDate.now(), result.getDate());
        assertEquals(user, result.getUser());

        verify(sessionRepository).save(any(Session.class));
        }


        @Test
        void updateSession_shouldUpdateSession() {

                User user = new User();
                user.setId(1L);

                Session existingSession = new Session();
                existingSession.setId(1L);
                existingSession.setDate(LocalDate.of(2026, 9, 4));
                existingSession.setLocation("Kletterhalle Duisburg");
                existingSession.setUser(user);

                SessionUpdateDTO request = new SessionUpdateDTO();
                request.setDate(LocalDate.of(2026, 9, 5));
                request.setLocation("Kletterhalle Essen");

                when(sessionRepository.findById(1L))
                        .thenReturn(Optional.of(existingSession));

                when(sessionRepository.save(existingSession))
                        .thenReturn(existingSession);

                SessionResponseDTO result =
                        sessionService.updateSession(1L, request, user);

                assertEquals(1L, result.getId());
                assertEquals(
                        LocalDate.of(2026, 9, 5),
                        result.getDate()
                );
                assertEquals(
                        "Kletterhalle Essen",
                        result.getLocation()
                );

                verify(sessionRepository).findById(1L);
                verify(sessionRepository).save(existingSession);
        }


        @Test
        void deleteSession_shouldDeleteSession() {

                User user = new User();
                user.setId(1L);

                Session session = new Session();
                session.setId(1L);
                session.setUser(user);

                when(sessionRepository.findById(1L))
                        .thenReturn(Optional.of(session));

                sessionService.deleteSession(1L, user);

                verify(sessionRepository).findById(1L);
                verify(sessionRepository).delete(session);
        }
}