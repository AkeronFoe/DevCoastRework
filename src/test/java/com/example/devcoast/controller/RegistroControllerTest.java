package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.LimiteTasa;
import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.UsuarioRepository;

@WebMvcTest(RegistroController.class)
@Import(SecurityConfig.class)
class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private LimiteTasa limiteTasa;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registroTalento() {
        return post("/register/talento").with(csrf())
                .param("nombre", "Ana Torres").param("rolTitulo", "Backend Developer").param("stack", "Java, Spring")
                .param("correo", "ana@correo.com").param("password", "clave-larga-1");
    }

    @Test
    void elEndpointQuePermitiaAveriguarSiUnCorreoExisteYaNoExiste() throws Exception {
        mockMvc.perform(get("/register/disponible").param("correo", "ana@correo.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unRegistroNormalSigueFuncionando() throws Exception {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId("u1");
            return u;
        });

        mockMvc.perform(registroTalento())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void conDemasiadosRegistrosDesdeUnMismoEquipoNoSeCreaLaCuentaYSeAvisa() throws Exception {
        when(limiteTasa.excedido(anyString(), anyInt(), anyLong())).thenReturn(true);

        mockMvc.perform(registroTalento())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/talento?error=limite"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void lomismoVaParaLasEmpresas() throws Exception {
        when(limiteTasa.excedido(anyString(), anyInt(), anyLong())).thenReturn(true);

        mockMvc.perform(post("/register/empresa").with(csrf())
                        .param("nombreEmpresa", "Nordic").param("nombreRepresentante", "Luis").param("cargoRepresentante", "CTO")
                        .param("correo", "hola@nordic.io").param("password", "clave-larga-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/empresa?error=limite"));
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void laPantallaExplicaElLimite() throws Exception {
        mockMvc.perform(get("/register/talento").param("error", "limite"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("demasiadas cuentas")));
    }
}
