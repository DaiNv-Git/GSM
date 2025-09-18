package com.example.gsm.controller;

import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.entity.User;
import com.example.gsm.entity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.example.gsm.comon.Constants.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Create
    @PostMapping("/create")
    public ResponseCommon<User> createUser(@RequestBody User user) {
        try {
            User saved = userRepository.save(user);
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, saved);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // Read all
    @GetMapping
    public ResponseCommon<List<User>> getAllUsers() {
        try {
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, userRepository.findAll());
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // Read by id
    @GetMapping("/find/{id}")
    public ResponseCommon<User> getUserById(@PathVariable String id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            return userOpt
                    .map(user -> new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, user))
                    .orElseGet(() -> new ResponseCommon<>(CORE_ERROR_CODE, "User not found", null));
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // Update
    @PutMapping("/update/{id}")
    public ResponseCommon<User> updateUser(@PathVariable String id, @RequestBody User user) {
        try {
            return userRepository.findById(id)
                    .map(existing -> {
                        user.setId(existing.getId());
                        User updated = userRepository.save(user);
                        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, updated);
                    })
                    .orElseGet(() -> new ResponseCommon<>(CORE_ERROR_CODE, "User not found", null));
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // Delete
    @DeleteMapping("/delete/{id}")
    public ResponseCommon<Void> deleteUser(@PathVariable String id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
            }
            return new ResponseCommon<>(CORE_ERROR_CODE, "User not found", null);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // ===== Search by Role =====
    @GetMapping("/search")
    public ResponseCommon<List<User>> searchByRole(@RequestParam String role) {
        try {
            List<User> result;
            switch (role.toLowerCase()) {
                case "admin":
                    result = userRepository.findByIsAdminTrueAndIsActiveTrue();
                    break;
                case "dev":
                    result = userRepository.findByIsDevTrueAndIsActiveTrue();
                    break;
                case "partner":
                    result = userRepository.findByIsPartnerTrueAndIsActiveTrue();
                    break;
                case "agent":
                    result = userRepository.findByIsAgentTrueAndIsActiveTrue();
                    break;
                default:
                    return new ResponseCommon<>(CORE_ERROR_CODE, "Invalid role: " + role, null);
            }
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, result);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
