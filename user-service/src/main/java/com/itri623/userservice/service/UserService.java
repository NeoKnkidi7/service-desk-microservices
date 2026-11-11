package com.itri623.userservice.service;

import com.itri623.userservice.model.User;
import com.itri623.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user) {

        if (user.getPassword() != null
                && !user.getPassword().isEmpty()) {

            user.setPassword(
                    passwordEncoder.encode(
                            user.getPassword()
                    )
            );

        } else {

            user.setPassword(
                    passwordEncoder.encode("default123")
            );
        }

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> updateUser(
            Long id,
            User userDetails) {

        return userRepository.findById(id).map(existing -> {

            existing.setName(userDetails.getName());
            existing.setEmail(userDetails.getEmail());
            existing.setRole(userDetails.getRole());

            return userRepository.save(existing);
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }
}