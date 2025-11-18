package ziply.user.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 사용자_생성_시_기본값_정상_설정() {
        User user = new User("test@example.com", "홍길동");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void 이름_업데이트_시_이름과_업데이트시간이_변경된다() {
        User user = new User("test@example.com", "홍길동");
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        user.updateName("이순신");

        assertThat(user.getName()).isEqualTo("이순신");
        assertThat(user.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    void 같은_이름으로_업데이트해도_updatedAt_은_변경된다() {
        User user = new User("test@example.com", "홍길동");
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        user.updateName("홍길동");

        assertThat(user.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    void 빈문자열_이름으로_변경하면_예외발생() {
        User user = new User("test@example.com", "홍길동");

        assertThatThrownBy(() -> user.updateName(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void null_이름으로_변경하면_예외발생() {
        User user = new User("test@example.com", "홍길동");

        assertThatThrownBy(() -> user.updateName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
