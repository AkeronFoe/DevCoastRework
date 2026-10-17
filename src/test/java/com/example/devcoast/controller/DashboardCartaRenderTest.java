package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

// ronda 5: la carta de navegación del Dashboard con solicitudes reales
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardCartaRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
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
        s.setAttribute("usuarioActualId", "t1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    private Solicitud solicitud(String id, String cargo, String empresa, String categoria, String estado) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoTitulo(cargo);
        s.setEmpleoEmpresaNombre(empresa);
        s.setEmpleoCategoria(categoria);
        s.setEmpleoStack("Java, Docker, Kafka, AWS");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date());
        return s;
    }

    @Test
    void dibujaUnBarcoPorSolicitudActivaEnLaZonaDeSuEtapa() throws Exception {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setStack(List.of("Java", "Docker"));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of(
                solicitud("s1", "Desarrollo Backend", "Nordic Labs", "Programacion · Backend", "revision"),
                solicitud("s2", "Diseño de interfaces", "Estudio Lumen", "Diseño · UI/UX", "revision"),
                solicitud("s3", "Desarrollo móvil", "Capsula", "Programacion · Mobile", "aprobada"),
                solicitud("s4", "API de pagos", "Faro", "Programacion · Backend", "rechazada")));

        MvcResult r = mockMvc.perform(get("/dashboard/carta").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EN ALTA MAR")))
                .andExpect(content().string(containsString("PUERTO")))
                .andExpect(content().string(containsString("aria-label=\"En revisión: 2\"")))
                .andExpect(content().string(containsString("aria-label=\"Decisión: 1\"")))
                .andExpect(content().string(containsString("Desarrollo Backend, Nordic Labs, en revisión, match 50 por ciento, área Programación")))
                .andExpect(content().string(containsString("Desarrollo móvil, Capsula, aceptada, match 50 por ciento, área Programación")))
                .andExpect(content().string(containsString("--s1:#2DD4BF")))
                .andExpect(content().string(containsString("href=\"/solicitudes/s1\"")))
                .andExpect(content().string(containsString("<use href=\"#vessel\"/>")))
                .andExpect(content().string(containsString("Nordic Labs · 50%")))
                // la que no avanzó no se dibuja
                .andExpect(content().string(not(containsString("API de pagos"))))
                .andReturn();
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("dashboard-carta.html"), r.getResponse().getContentAsString());
    }

    @Test
    void conMasDeCuatroEnUnaZonaMuestraElResto() throws Exception {
        Usuario u = new Usuario();
        u.setStack(List.of("Java", "Docker", "Kafka"));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        String[] cats = { "Programacion · Backend", "Diseño · UI/UX", "Programacion · Frontend", "Diseño · Grafico", "Marketing digital", "Programacion · Mobile" };
        Solicitud[] varias = new Solicitud[8];
        for (int i = 0; i < 6; i++)
            varias[i] = solicitud("s" + i, "Cargo " + i, "Empresa " + i, cats[i], "revision");
        varias[6] = solicitud("a1", "Desarrollo móvil", "Capsula", "Programacion · Mobile", "aprobada");
        varias[7] = solicitud("a2", "Diseño gráfico", "Faro", "Diseño · Grafico", "aprobada");
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of(varias));

        MvcResult r = mockMvc.perform(get("/dashboard/carta").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("+2 más")))
                .andExpect(content().string(containsString("/solicitudes?estado=proceso")))
                .andExpect(content().string(not(containsString("Sin solicitudes en esta etapa."))))
                .andExpect(content().string(not(containsString("Cargo 5"))))
                .andReturn();
        Files.createDirectories(Path.of("target", "render"));
        Files.writeString(Path.of("target", "render", "dashboard-carta-llena.html"), r.getResponse().getContentAsString());
    }

    @Test
    void sinSolicitudesInvitaAExplorarEmpleos() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(new Usuario()));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/dashboard/carta").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía no has zarpado.")))
                .andExpect(content().string(containsString("Explora empleos")))
                .andReturn();
        Files.writeString(Path.of("target", "render", "dashboard-carta-vacia.html"), r.getResponse().getContentAsString());
    }

    @Test
    void soloParaTalentos() throws Exception {
        mockMvc.perform(get("/dashboard/carta").session(sesion("empresa")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(get("/dashboard/carta").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
