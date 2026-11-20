package cl.pedidos360.microservicio_kafka_admin.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import cl.pedidos360.microservicio_kafka_admin.config.JwtAuthConverter;
import cl.pedidos360.microservicio_kafka_admin.config.SecurityConfig;
import cl.pedidos360.microservicio_kafka_admin.exception.GlobalExceptionHandler;
import cl.pedidos360.microservicio_kafka_admin.exception.KafkaAdminException;
import cl.pedidos360.microservicio_kafka_admin.service.KafkaAdminService;

@WebMvcTest(KafkaAdminController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class, GlobalExceptionHandler.class})
class KafkaAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaAdminService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("401 sin token")
    void listarTopics_SinToken_Retorna401() throws Exception {
        mockMvc.perform(get("/api/kafka/admin/topics")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 con rol USER")
    void listarTopics_ConRolUser_Retorna403() throws Exception {
        mockMvc.perform(get("/api/kafka/admin/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 con rol ADMIN")
    void listarTopics_ConRolAdmin_Retorna200() throws Exception {
        when(service.listarTopics(false)).thenReturn(List.of("pedidos.eventos"));

        mockMvc.perform(get("/api/kafka/admin/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("pedidos.eventos"));
    }

    @Test
    @DisplayName("201 al crear topico valido")
    void crearTopic_Valido_Retorna201() throws Exception {
        String body = "{\"name\":\"prueba.topic\",\"partitions\":3,\"replicationFactor\":2,"
                + "\"configs\":{\"retention.ms\":\"86400000\"}}";

        mockMvc.perform(post("/api/kafka/admin/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("prueba.topic"));
    }

    @Test
    @DisplayName("400 al crear topico con datos invalidos")
    void crearTopic_Invalido_Retorna400() throws Exception {
        String body = "{\"name\":\"nombre con espacios\",\"partitions\":0,\"replicationFactor\":2}";

        mockMvc.perform(post("/api/kafka/admin/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.partitions").exists());
    }

    @Test
    @DisplayName("El status del error de Kafka se propaga (404 topico inexistente)")
    void describirTopic_NoExiste_Retorna404() throws Exception {
        when(service.describirTopic("no.existe"))
                .thenThrow(new KafkaAdminException(org.springframework.http.HttpStatus.NOT_FOUND, "no existe"));

        mockMvc.perform(get("/api/kafka/admin/topics/no.existe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Eliminar topico delega en el servicio")
    void eliminarTopic_ConRolAdmin_LlamaAlServicio() throws Exception {
        mockMvc.perform(delete("/api/kafka/admin/topics/prueba.topic")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        verify(service).eliminarTopic("prueba.topic");
    }
}
