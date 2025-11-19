package ziply.user.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void userIsCreatedWithDefaultValues() {
        User user = new User("test@example.com", "Hong Gil-dong");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("Hong Gil-dong");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateNameChangesNameAndUpdatedAt() {
        User user = new User("test@example.com", "Hong Gil-dong");
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        user.updateName("Yi Sun-shin");

        assertThat(user.getName()).isEqualTo("Yi Sun-shin");
        assertThat(user.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    void updateNameWithSameValueStillUpdatesUpdatedAt() {
        User user = new User("test@example.com", "Hong Gil-dong");
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        user.updateName("Hong Gil-dong");

        assertThat(user.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    void updateNameWithEmptyStringThrowsException() {
        User user = new User("test@example.com", "Hong Gil-dong");

        assertThatThrownBy(() -> user.updateName(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateNameWithNullThrowsException() {
        User user = new User("test@example.com", "Hong Gil-dong");

        assertThatThrownBy(() -> user.updateName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
