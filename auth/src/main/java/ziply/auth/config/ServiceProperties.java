package ziply.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceProperties {

    private UserService user = new UserService();

    @Getter
    @Setter
    public static class UserService {
        private String baseUrl;
        private Endpoint endpoint = new Endpoint();

        @Getter
        @Setter
        public static class Endpoint {
            private String createUser;
        }
    }
}


