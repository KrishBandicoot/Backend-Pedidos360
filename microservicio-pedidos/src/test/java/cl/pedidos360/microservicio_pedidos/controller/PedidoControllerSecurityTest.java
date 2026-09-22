package cl.pedidos360.microservicio_pedidos.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.pedidos360.microservicio_pedidos.model.Pedido;
import cl.pedidos360.microservicio_pedidos.repository.PedidoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PedidoControllerSecurityTest.TestSecurityConfig.class)
class PedidoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public JwtDecoder jwtDecoder() {
            // Stub para desacoplar llamadas de red a Azure Entra ID durante las pruebas
            return token -> null;
        }
    }

    @BeforeEach
    void setup() {
        pedidoRepository.deleteAll();
    }

    @Test
    @DisplayName("1. [401 UNAUTHORIZED] - Rechazo de petición GET sin token de autorización")
    void getPedidos_SinToken_Retorna401() throws Exception {
        mockMvc.perform(get("/pedidos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. [401 UNAUTHORIZED] - Rechazo de petición POST sin token de autorización")
    void crearPedido_SinToken_Retorna401() throws Exception {
        String jsonPayload = """
            {
                "cliente": "Empresa Retail SpA",
                "estado": "Pendiente"
            }
            """;

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. [403 FORBIDDEN] - Rechazo de eliminación cuando el usuario solo tiene rol USER")
    void eliminarPedido_ConRolUser_Retorna403() throws Exception {
        Pedido pedido = pedidoRepository.save(new Pedido(null, "Transportes Andes", "Pendiente"));

        mockMvc.perform(delete("/pedidos/" + pedido.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. [204 NO CONTENT] - Eliminación exitosa cuando el usuario cuenta con rol ADMIN")
    void eliminarPedido_ConRolAdmin_Retorna204() throws Exception {
        Pedido pedido = pedidoRepository.save(new Pedido(null, "Logística Austral", "Pendiente"));

        mockMvc.perform(delete("/pedidos/" + pedido.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("5. [200 OK] - Lectura de pedidos autorizada con rol USER (Persistencia Real H2)")
    void listarPedidos_ConRolUser_Retorna200() throws Exception {
        pedidoRepository.save(new Pedido(null, "Transportes Norte", "Completado"));

        mockMvc.perform(get("/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].cliente").value("Transportes Norte"))
            .andExpect(jsonPath("$[0].estado").value("Completado"));
    }

    @Test
    @DisplayName("6. [201 CREATED] - Creación de pedido autorizada con rol ADMIN y datos válidos")
    void crearPedido_ConRolAdmin_DatosValidos_Retorna201() throws Exception {
        String jsonPayload = """
            {
                "cliente": "Constructora Central",
                "estado": "En Preparacion"
            }
            """;

        mockMvc.perform(post("/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.cliente").value("Constructora Central"))
            .andExpect(jsonPath("$.estado").value("En Preparacion"));
    }

    @Test
    @DisplayName("7. [400 BAD REQUEST] - Validación de entrada: Rechazo por campos vacíos o inválidos")
    void crearPedido_DatosInvalidos_Retorna400() throws Exception {
        String jsonPayloadInvalido = """
            {
                "cliente": "AB",
                "estado": "Inexistente"
            }
            """;

        mockMvc.perform(post("/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayloadInvalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.details.cliente").exists())
            .andExpect(jsonPath("$.details.estado").exists());
    }

    @Test
    @DisplayName("8. [200 OK] - Endpoint de salud /actuator/health público para API Gateway / Load Balancer")
    void healthCheck_Publico_Retorna200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }
}
