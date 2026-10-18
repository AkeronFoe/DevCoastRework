package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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

    @MockitoBean
    private InvitacionRepository invitacionRepository;

    @MockitoBean
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private MensajeRepository mensajeRepository;

    @MockitoBean
    private com.example.devcoast.config.LimiteIntentosLogin limiteIntentos;

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

    @Test
    void conDemasiadosIntentosNoSeRevisaLaContrasenaYSeAvisa() throws Exception {
        when(limiteIntentos.bloqueado(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("correo", "ana@correo.com")
                        .param("password", "clave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("flashMessage", org.hamcrest.Matchers.containsString("Demasiados intentos")));

        org.mockito.Mockito.verifyNoInteractions(usuarioRepository);
    }

    @Test
    void cadaFalloSeRegistraYUnLoginCorrectoLimpiaElConteo() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setCorreo("ana@correo.com");
        usuario.setPassword(passwordEncoder.encode("clave123"));
        usuario.setTipo(RoleGuard.TIPO_TALENTO);
        when(usuarioRepository.findByCorreo("ana@correo.com")).thenReturn(usuario);

        mockMvc.perform(post("/login").with(csrf()).param("correo", "ana@correo.com").param("password", "mala"));
        org.mockito.Mockito.verify(limiteIntentos).fallo(anyString(), org.mockito.ArgumentMatchers.eq("ana@correo.com"));

        mockMvc.perform(post("/login").with(csrf()).param("correo", "ana@correo.com").param("password", "clave123"));
        org.mockito.Mockito.verify(limiteIntentos).exito(anyString(), org.mockito.ArgumentMatchers.eq("ana@correo.com"));
    }

    @Test
    void laRespuestaLlevaLasCabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("frame-ancestors 'none'"),
                                org.hamcrest.Matchers.containsString("object-src 'none'"),
                                org.hamcrest.Matchers.containsString("form-action 'self'"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Permissions-Policy",
                        org.hamcrest.Matchers.containsString("camera=()")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void laPaginaTraeElIconoDeLaApp() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.matchesPattern("(?s).*rel=\"icon\" href=\"/favicon(-[0-9a-f]{32})?\\.ico\".*")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.matchesPattern("(?s).*rel=\"apple-touch-icon\" href=\"/img/apple-touch-icon(-[0-9a-f]{32})?\\.png\".*")));
    }
}
