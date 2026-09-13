package boulderino.boulderino.config;

import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.entity.WallAngle;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.BoulderRepository;
import boulderino.boulderino.repository.SessionRepository;
import boulderino.boulderino.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;

@Configuration
public class DataSeeder {

        private final Random random = new Random();

        @Bean
        CommandLineRunner seedDatabase(
                UserRepository userRepository,
                BoulderRepository boulderRepository,
                SessionRepository sessionRepository,
                AttemptsRepository attemptsRepository,
                PasswordEncoder passwordEncoder
        ){

                return args -> {

                        // Verhindert mehrfaches Seeden beim Neustart
                        if (userRepository.count() > 2) {
                                return;
                        }

                        // =========================
                        // USER
                        // =========================

                        User user = new User();

                        user.setName("Test3 Climber");
                        user.setEmail("test3@example.com");
                        user.setPassword(passwordEncoder.encode("bigBoi5050"));

                        userRepository.save(user);


                        // =========================
                        // SESSIONS
                        // =========================

                        for (int i = 0; i < 20; i++) {

                                Session session = new Session();

                                session.setUser(user);
                                session.setDate(
                                        LocalDate.now()
                                                .minusDays(i * 3)
                                );

                                session.setLocation("Test Boulderhalle");

                                sessionRepository.save(session);
                        }


                        // =========================
                        // BOULDERS
                        // =========================

                        Grade[] grades = Grade.values();
                        GripType[] gripTypes = GripType.values();
                        WallAngle[] wallAngles = WallAngle.values();
                        RouteCharacter[] routeCharacters =
                                RouteCharacter.values();
                        ClimbingStyle[] climbingStyles =
                                ClimbingStyle.values();

                        for (int i = 0; i < 100; i++) {

                                Boulder boulder = new Boulder();

                                boulder.setUser(user);
                                boulder.setName("Test Boulder " + (i + 1));

                                boulder.setGrade(
                                        grades[
                                                random.nextInt(
                                                        grades.length
                                                )
                                        ]
                                );

                                boulder.setGripType(
                                        gripTypes[
                                                random.nextInt(
                                                        gripTypes.length
                                                )
                                        ]
                                );

                                boulder.setWallAngle(
                                        wallAngles[
                                                random.nextInt(
                                                        wallAngles.length
                                                )
                                        ]
                                );

                                boulder.setRouteCharacter(
                                        routeCharacters[
                                                random.nextInt(
                                                        routeCharacters.length
                                                )
                                        ]
                                );

                                boulder.setClimbingStyle(
                                        climbingStyles[
                                                random.nextInt(
                                                        climbingStyles.length
                                                )
                                        ]
                                );


                                // =========================
                                // SUCCESS / FAILURE
                                // =========================

                                boolean topped =
                                        random.nextDouble() < 0.65;

                                boulder.setFirstAscent(topped);


                                if (topped) {

                                boulder.setFirstAscentDate(
                                        LocalDate.now()
                                                .minusDays(
                                                        random.nextInt(120)
                                                )
                                );

                                boolean flashed =
                                        random.nextDouble() < 0.25;

                                boulder.setFlashed(flashed);

                                } else {

                                boulder.setFirstAscentDate(null);
                                boulder.setFlashed(false);
                                }


                                boulderRepository.save(boulder);
                        }


                        // =========================
                        // ATTEMPTS
                        // =========================

                        for (Boulder boulder :
                                boulderRepository.findByUser(user)) {

                                Session session =
                                        sessionRepository
                                                .findAll()
                                                .get(
                                                        random.nextInt(
                                                                sessionRepository
                                                                        .findAll()
                                                                        .size()
                                                        )
                                                );


                                int numberOfAttempts =
                                        random.nextInt(4) + 1;


                                for (int i = 0;
                                i < numberOfAttempts;
                                i++) {

                                Attempts attempt =
                                        new Attempts();

                                attempt.setBoulder(boulder);

                                attempt.setSession(session);

                                attempt.setTries(
                                        random.nextInt(5) + 1
                                );


                                // Wenn Boulder geschafft:
                                // Letzter Attempt kann done sein

                                if (Boolean.TRUE.equals(
                                        boulder.getFirstAscent())
                                        && i == numberOfAttempts - 1) {

                                        attempt.setDone(true);

                                } else {

                                        attempt.setDone(false);
                                }


                                attempt.setZone(
                                        random.nextBoolean()
                                );


                                attemptsRepository.save(attempt);
                                }
                        }


                        System.out.println(
                                "Testdaten erfolgreich erstellt!"
                        );

                };
        }
}