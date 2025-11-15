package ziply.user.service;

import org.springframework.stereotype.Service;
import ziply.user.domain.user.User;
import ziply.user.domain.user.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(IllegalArgumentException::new);
    }
}