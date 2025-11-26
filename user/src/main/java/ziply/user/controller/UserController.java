package ziply.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import ziply.user.dto.response.UserNameResponse;
import ziply.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ziply.user.dto.request.UserCreateRequest;
import ziply.user.dto.request.UserUpdateRequest;
import ziply.user.dto.response.UserResponse;

@Tag(name = "User API", description = "유저 정보 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    @Operation(summary = "유저 생성 (AUTH 내부 호출용)")
    @PostMapping
    public UserResponse createUser(@RequestBody @Valid UserCreateRequest request) {
        return userService.createUser(request);
    }

    @Operation(summary = "유저 단건 조회 (JWT)")
    @GetMapping("/me")
    public UserResponse getMe(@AuthenticationPrincipal Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "유저 이름 조회 (JWT)")
    @GetMapping("/name")
    public UserNameResponse getName(@AuthenticationPrincipal Long userId) {
        return userService.getUserName(userId);
    }


    @Operation(summary = "유저 정보 수정 (JWT)")
    @PatchMapping("/me")
    public UserResponse updateUser(
            @AuthenticationPrincipal  Long userId,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        return userService.updateUser(userId, request);
    }

}
