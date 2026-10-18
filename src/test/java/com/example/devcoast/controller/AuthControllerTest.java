package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.UsuarioRepository;

// probamos el login como si fueramos el navegador: sin esto, si alguien cambia
// RoleGuard o el AuthController y rompe el acceso, nadie se entera hasta produccion
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @MockitoBean
    private EmpresaRepository empresaRepository;

    // el interceptor de notificaciones se mete en TODAS las paginas (menos login/register),
    // asi que aunque este test no lo use, Spring lo necesita armado igual para arrancar
    @MockitoBean
    private InvitacionRepository invitacionRepository;

    @MockitoBean
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private MensajeRepository mensajeRepository;

    @Test
    void loginConCredencialesCorrectasRedirigeAlDashboard() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setCorreo("ana@correo.com");
        usuario.setPassword(passwordEncoder.encode("clave123"));
        usuario.setNombre("Ana");
        usuario.setTipo(RoleGuard.TIPO_TALENTO);

        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(usuario);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("correo", "ana@correo.com")
                        .param("password", "clave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginConPasswordIncorrectaVuelveAlLoginConError() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setCorreo("ana@correo.com");
        usuario.setPassword(passwordEncoder.encode("claveBuena"));

        when(usuarioRepository.findByCorreo(anyString())).thenReturn(usuario);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("correo", "ana@correo.com")
                        .param("password", "claveMala"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginConCorreoQueNoExisteVuelveAlLoginConError() throws Exception {
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(null);
        when(empresaRepository.findByCorreo(anyString())).thenReturn(null);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("correo", "no-existe@correo.com")
                        .param("password", "loquesea"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getLoginMuestraElFormularioCuandoNoHaySesion() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }
}
