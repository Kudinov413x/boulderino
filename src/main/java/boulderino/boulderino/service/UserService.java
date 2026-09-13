package boulderino.boulderino.service;

import boulderino.boulderino.dto.RegisterRequestDTO;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequestDTO request) {

        // Prüfen, ob die E-Mail bereits verwendet wird
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-Mail wird bereits verwendet");
        }

        // Neue User-Entity erstellen
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Passwort niemals im Klartext speichern
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden"));

        userRepository.delete(user);
    }
}

