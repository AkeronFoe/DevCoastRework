package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;

// ronda 1 del rediseño náutico: el shell (fragments/nautico) renderiza y toma los datos del menú
@WebMvcTest(ShellPreviewController.class)
@Import(SecurityConfig.class)
class ShellPreviewRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;

    private MockHttpSession sesion(String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", "u1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    private void volcar(String nombre, String html) throws Exception {
        // para revisar el resultado en un navegador (target/ no se versiona)
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), html);
    }

    @Test
    void sinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/shell-preview").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void talentoVeMenuConInsigniasReales() throws Exception {
        when(mongoTemplate.count(any(Query.class), eq(Empleo.class))).thenReturn(8L);
        Usuario u = new Usuario();
        u.setId("u1");
        u.setNombre("Valentina Ríos");
        u.setRolTitulo("Frontend");
        when(mongoTemplate.findOne(any(Query.class), eq(Usuario.class))).thenReturn(u);
        when(invitacionRepository.countByTalentoIdAndEstado("u1", "pendiente")).thenReturn(2L);
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse("u1")).thenReturn(1L);
        Notificacion n = new Notificacion();
        n.setId("n1");
        n.setTitulo("Tu postulación fue aceptada");
        n.setMensaje("Nordic Labs aceptó tu postulación");
        when(notificacionRepository.findTop5ByDestinatarioIdOrderByFechaCreacionDesc("u1")).thenReturn(List.of(n));

        var res = mockMvc.perform(get("/shell-preview").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mi perfil")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("8 ofertas activas")))
                .andExpect(content().string(containsString("2 invitaciones pendientes")))
                .andExpect(content().string(containsString("role=\"progressbar\"")))
                .andExpect(content().string(containsString("Tu postulación fue aceptada")))
                .andExpect(content().string(containsString("hullclip")))
                .andExpect(content().string(not(containsString("Mis ofertas"))))
                .andReturn();
        volcar("shell-talento.html", res.getResponse().getContentAsString());
    }

    @Test
    void empresaVeSuMenuYAcentoTeal() throws Exception {
        var res = mockMvc.perform(get("/shell-preview").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mis ofertas")))
                .andExpect(content().string(containsString("Postulaciones")))
                .andExpect(content().string(containsString("Perfil de empresa")))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(not(containsString("role=\"progressbar\""))))
                .andReturn();
        volcar("shell-empresa.html", res.getResponse().getContentAsString());
    }

    @Test
    void sinDatosDelInterceptorElShellRendizaSinInsigniaNiProgreso() throws Exception {
        // si los conteos fallan el menú se muestra igual, sin esas piezas
        when(mongoTemplate.count(any(Query.class), eq(Empleo.class))).thenThrow(new RuntimeException("sin conexión"));
        when(mongoTemplate.findOne(any(Query.class), eq(Usuario.class))).thenThrow(new RuntimeException("sin conexión"));

        mockMvc.perform(get("/shell-preview").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ofertas activas"))))
                .andExpect(content().string(not(containsString("role=\"progressbar\""))));
    }
}
