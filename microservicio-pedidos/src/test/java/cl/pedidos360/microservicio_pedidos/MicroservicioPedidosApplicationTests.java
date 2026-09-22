package cl.pedidos360.microservicio_pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MicroservicioPedidosApplicationTests.TestSecurityConfig.class)
class MicroservicioPedidosApplicationTests {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public JwtDecoder jwtDecoder() {
            return token -> null;
        }
    }

    @Test
    void contextLoads() {
    }

}
