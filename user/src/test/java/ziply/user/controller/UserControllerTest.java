package ziply.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ziply.user.domain.user.UserStatus;
import ziply.user.dto.request.UserCreateRequest;
import ziply.user.dto.request.UserUpdateRequest;
import ziply.user.dto.response.UserResponse;
import ziply.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    @InjectMocks
    UserController userController;

    private UserResponse dummy(Long id, String email, String name) {
        return new UserResponse(
                id,
                email,
                name,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createUser_호출시_Service_createUser_결과를_반환한다() {
        // given
        UserCreateRequest request = new UserCreateRequest("test@example.com", "홍길동");
        UserResponse response = dummy(1L, "test@example.com", "홍길동");

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        // when
        UserResponse result = userController.createUser(request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        verify(userService).createUser(request);
    }

    @Test
    void getMe_호출시_Service_getUserById_결과를_반환한다() {
        // given
        Long userId = 1L;
        UserResponse response = dummy(1L, "me@example.com", "나자신");

        when(userService.getUserById(userId)).thenReturn(response);

        // when
        UserResponse result = userController.getMe(userId);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("me@example.com");
        assertThat(result.name()).isEqualTo("나자신");
        verify(userService).getUserById(userId);
    }

    @Test
    void updateUser_호출시_Service_updateUser_결과를_반환한다() {
        // given
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest("이순신");
        UserResponse response = dummy(1L, "test@example.com", "이순신");

        when(userService.updateUser(userId, request)).thenReturn(response);

        // when
        UserResponse result = userController.updateUser(userId, request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("이순신");
        verify(userService).updateUser(userId, request);
    }
}
