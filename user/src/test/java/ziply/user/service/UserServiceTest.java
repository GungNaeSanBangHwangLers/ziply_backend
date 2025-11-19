package ziply.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;
import ziply.user.domain.user.User;
import ziply.user.domain.user.UserRepository;
import ziply.user.dto.request.UserCreateRequest;
import ziply.user.dto.request.UserUpdateRequest;
import ziply.user.dto.response.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    void returnsExistingUserIfEmailAlreadyExists() {
        User existing = new User("test@example.com", "Hong Gil-dong");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existing));

        UserResponse response = userService.createUser(
                new UserCreateRequest("test@example.com", "Hong Gil-dong")
        );

        assertThat(response.email()).isEqualTo("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsAndSavesNewUserIfEmailDoesNotExist() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        User newUser = new User("test@example.com", "Hong Gil-dong");

        when(userRepository.save(any())).thenReturn(newUser);

        UserResponse response = userService.createUser(
                new UserCreateRequest("test@example.com", "Hong Gil-dong")
        );

        assertThat(response.email()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void getUserByIdReturnsUserWhenExists() {
        User user = new User("test@example.com", "Hong Gil-dong");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void getUserByIdThrowsExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateUserUpdatesUserSuccessfully() {
        User user = new User("test@example.com", "Hong Gil-dong");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateUser(
                1L,
                new UserUpdateRequest("Yi Sun-shin")
        );

        assertThat(response.name()).isEqualTo("Yi Sun-shin");
    }

    @Test
    void updateUserThrowsExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateUser(1L, new UserUpdateRequest("Yi Sun-shin"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
