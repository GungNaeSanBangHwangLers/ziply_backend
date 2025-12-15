package ziply.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ziply.user.dto.request.UserCreateRequest;
import ziply.user.dto.request.UserUpdateRequest;
import ziply.user.dto.response.UserNameResponse;
import ziply.user.dto.response.UserResponse;
import ziply.user.domain.user.User;
import ziply.user.domain.user.UserRepository;
import ziply.user.exception.ErrorCode;
import ziply.user.exception.UserException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("[USER] Creating user with email: {}", request.email());

        return userRepository.findByEmail(request.email())
                .map(existingUser -> {
                    log.info("[USER] User already exists with email: {}, returning existing user", request.email());
                    return UserResponse.from(existingUser);
                })
                .orElseGet(() -> {
                    User user = new User(request.email(), request.name());
                    User saved = userRepository.save(user);
                    log.info("[USER] New user created with id: {}, email: {}", saved.getId(), saved.getEmail());
                    return UserResponse.from(saved);
                });
    }

    public UserResponse getUserById(Long userId) {
        log.debug("[USER] Getting user by id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[USER] User not found with id: {}", userId);
                    return new UserException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMessage());
                });
        return UserResponse.from(user);
    }

    public UserNameResponse getUserName(Long userId) {
        log.debug("[USER] Getting user name by id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[USER] User not found with id: {}", userId);
                    return new UserException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMessage());
                });
        return UserNameResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.info("[USER] Updating user id: {}, new name: {}", userId, request.name());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[USER] User not found with id: {}", userId);
                    return new UserException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMessage());
                });

        user.updateName(request.name());
        log.info("[USER] User updated successfully, id: {}", userId);
        return UserResponse.from(user);
    }
}
