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
    void 이미_존재하는_이메일이면_기존_유저를_반환한다() {

        User existing = new User("test@example.com", "홍길동");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existing));

        UserResponse response = userService.createUser(
                new UserCreateRequest("test@example.com", "홍길동")
        );

        assertThat(response.email()).isEqualTo("test@example.com");
        verify(userRepository, never()).save(any());
    }


    @Test
    void 존재하지_않는_이메일이면_새로_생성해서_저장한다() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        User newUser = new User("test@example.com", "홍길동");

        when(userRepository.save(any())).thenReturn(newUser);

        UserResponse response = userService.createUser(
                new UserCreateRequest("test@example.com", "홍길동")
        );

        assertThat(response.email()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).save(any());
    }


    @Test
    void 유저ID로_조회_성공() {

        User user = new User("test@example.com", "홍길동");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
    }


    @Test
    void 유저ID_조회_실패하면_예외발생() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void 유저_업데이트_성공() {

        User user = new User("test@example.com", "홍길동");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateUser(
                1L,
                new UserUpdateRequest("이순신")
        );

        assertThat(response.name()).isEqualTo("이순신");
    }


    @Test
    void 유저_업데이트_대상_없으면_예외발생() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateUser(1L, new UserUpdateRequest("이순신"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
