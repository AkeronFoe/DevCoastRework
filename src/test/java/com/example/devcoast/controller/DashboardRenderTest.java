package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

// ronda 5: Dashboard de talento y de empresa con datos reales
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardRenderTest {

    private static final long DIA = 86_400_000L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;

    private Date hace(int dias) {
        return new Date(System.currentTimeMillis() - dias * DIA);
    }

    private MockHttpSession sesion(String tipo, String id, String nombre) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", id);
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", nombre);
        s.setAttribute("usuarioActualCorreo", "cuenta@correo.com");
        return s;
    }

    private Solicitud solicitud(String id, String cargo, String empresa, String categoria, String estado, int enviadaHace, Integer decididaHace) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoId("e" + id);
        s.setUsuarioId("t1");
        s.setUsuarioNombre("Camila Soto");
        s.setEmpleoTitulo(cargo);
        s.setEmpleoEmpresaNombre(empresa);
        s.setEmpleoCategoria(categoria);
        s.setEmpleoStack("Java, Docker, Kafka, AWS");
        s.setEstado(estado);
        s.setFechaSolicitud(hace(enviadaHace));
        List<EventoEstado> h = new ArrayList<>();
        h.add(new EventoEstado("revision", hace(enviadaHace), "Enviada"));
        if (decididaHace != null)
            h.add(new EventoEstado(estado, hace(decididaHace), "Decisión"));
        s.setHistorial(h);
        return s;
    }

    private Usuario talento(boolean conStack, boolean onboardingCompleto) {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        if (conStack)
            u.setStack(List.of("Java", "Docker"));
        u.setOnboardingCompleto(onboardingCompleto);
        return u;
    }

    private Empleo oferta(String id, String titulo, String empresa, String stack, int publicadaHace) {
        Empleo e = new Empleo();
        e.setId(id);
        e.setTitulo(titulo);
        e.setEmpresaNombre(empresa);
        e.setCategoria("Programacion · Backend");
        e.setModalidad("remoto");
        e.setStack(stack);
        e.setFechaPublicacion(hace(publicadaHace));
        return e;
    }

    private void guardar(String nombre, MvcResult r) throws Exception {
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), r.getResponse().getContentAsString());
    }

    // ───────────── talento ─────────────

    @Test
    void talentoVeLasTresZonasDeLaCartaConSusFechas() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(true, true)));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of(
                solicitud("s1", "Desarrollo Backend", "Nordic Labs", "Programacion · Backend", "revision", 1, null),     // zarpando
                solicitud("s2", "Diseño de interfaces", "Estudio Lumen", "Diseño · UI/UX", "revision", 10, null),         // alta mar
                solicitud("s3", "Desarrollo móvil", "Capsula", "Programacion · Mobile", "aprobada", 20, 5),               // puerto
                solicitud("s4", "API vieja", "Faro", "Programacion · Backend", "aprobada", 90, 70),                       // fuera de puerto
                solicitud("s5", "API de pagos", "Faro", "Programacion · Backend", "rechazada", 3, 1)));                   // no se dibuja

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("talento", "t1", "Valentina Ríos")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Buen viento, Valentina.")))
                .andExpect(content().string(containsString("ZARPANDO")))
                .andExpect(content().string(containsString("EN ALTA MAR")))
                .andExpect(content().string(containsString("PUERTO")))
                .andExpect(content().string(containsString("aria-label=\"Enviada: 1\"")))
                .andExpect(content().string(containsString("aria-label=\"En revisión: 1\"")))
                .andExpect(content().string(containsString("aria-label=\"Decisión: 1\"")))
                .andExpect(content().string(containsString("Desarrollo Backend, Nordic Labs, en revisión")))
                .andExpect(content().string(containsString("Desarrollo móvil, Capsula, aceptada")))
                .andExpect(content().string(not(containsString("API vieja"))))
                .andExpect(content().string(not(containsString("API de pagos, Faro"))))
                .andExpect(content().string(containsString("data-n=\"3\"")))
                // el diálogo de bienvenida no sale si ya completó el onboarding
                .andExpect(content().string(not(containsString("id=\"ob\""))))
                .andReturn();
        guardar("dashboard-talento.html", r);
    }

    @Test
    void talentoConOfertasNuevasVeMejorMatchYAvistadas() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(true, true)));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of(
                solicitud("s1", "Desarrollo Backend", "Nordic Labs", "Programacion · Backend", "revision", 1, null)));
        Invitacion inv = new Invitacion();
        inv.setId("i1");
        inv.setEstado("pendiente");
        inv.setEmpresaNombre("Faro");
        inv.setEmpleoTitulo("Plataforma");
        inv.setFechaEnvio(hace(1));
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc("t1")).thenReturn(List.of(inv));
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(List.of(
                oferta("o1", "Ingeniero Java", "Orion", "Java, Docker", 2),      // 100 %, nueva
                oferta("o2", "Dev Java Senior", "Altamar", "Java, Docker, Kafka", 10),   // medio
                oferta("o3", "Dev Go", "Boreal", "Go, Rust", 3)));                // sin coincidencia

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("talento", "t1", "Valentina Ríos")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MEJOR MATCH DE HOY")))
                .andExpect(content().string(containsString("Ingeniero Java")))
                .andExpect(content().string(containsString("<h2>Avistadas</h2>")))
                .andExpect(content().string(containsString("Dev Java Senior")))
                .andExpect(content().string(not(containsString("Dev Go"))))
                .andExpect(content().string(containsString("1 invitación")))
                .andExpect(content().string(containsString("Faro")))
                .andReturn();
        guardar("dashboard-talento-ofertas.html", r);
    }

    @Test
    void talentoSinStackNoVeMatchYSeLeInvitaAAgregarlo() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(false, true)));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("talento", "t1", "Valentina Ríos")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agrega tu stack en")))
                .andExpect(content().string(not(containsString("MEJOR MATCH DE HOY"))))
                .andExpect(content().string(not(containsString("<h2>Avistadas</h2>"))))
                .andExpect(content().string(containsString("Todavía no has zarpado.")))
                .andExpect(content().string(containsString("aria-label=\"sin datos\"")))
                .andReturn();
        guardar("dashboard-talento-vacio.html", r);
    }

    @Test
    void talentoNuevoVeElDialogoDeBienvenida() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(false, false)));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("talento", "t1", "Valentina Ríos")))
                .andExpect(content().string(containsString("id=\"ob\"")))
                .andExpect(content().string(containsString("Completa tu perfil")))
                .andReturn();
        guardar("dashboard-talento-onboarding.html", r);
    }

    // ───────────── empresa ─────────────

    @Test
    void empresaVeSuCartaDeCandidatosSusNumerosYSusOfertas() throws Exception {
        Empleo activa = oferta("e1", "Backend Java", "Nordic Labs", "Java, Docker, Kafka, AWS", 5);
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "ACTIVO")).thenReturn(List.of(activa));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "CERRADO")).thenReturn(List.of());
        Solicitud a = solicitud("s1", "Backend Java", "Nordic Labs", "Programacion · Backend", "revision", 1, null);
        a.setEmpleoId("e1");
        Solicitud b = solicitud("s2", "Backend Java", "Nordic Labs", "Programacion · Backend", "aprobada", 15, 4);
        b.setEmpleoId("e1");
        b.setUsuarioId("t2");
        b.setUsuarioNombre("Mateo Vega");
        when(solicitudRepository.findByEmpleoIdIn(List.of("e1"))).thenReturn(List.of(a, b));
        Usuario t1 = talento(true, true);
        Usuario t2 = new Usuario();
        t2.setId("t2");
        t2.setNombre("Mateo Vega");
        t2.setStack(List.of("Java", "Docker", "Kafka", "AWS"));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(t1, t2));
        when(mongoTemplate.count(any(Query.class), eq(Solicitud.class))).thenReturn(1L);
        Invitacion inv = new Invitacion();
        inv.setTalentoNombre("Camila Soto");
        inv.setEmpleoTitulo("Backend Java");
        inv.setFechaEnvio(hace(2));
        when(invitacionRepository.findByEmpresaIdOrderByFechaEnvioDesc("em1")).thenReturn(List.of(inv));

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("empresa", "em1", "Nordic Labs")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard-empresa"))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("Buen viento, Nordic.")))
                .andExpect(content().string(containsString("OFERTAS ACTIVAS")))
                .andExpect(content().string(containsString("1 postulación</b> en revisión")))
                .andExpect(content().string(containsString("ZARPANDO")))
                .andExpect(content().string(containsString("PUERTO")))
                .andExpect(content().string(containsString("Mateo Vega")))
                .andExpect(content().string(containsString("href=\"/empleos/e1/postulaciones/s2\"")))
                .andExpect(content().string(containsString("<h2>Mejores candidatos</h2>")))
                .andExpect(content().string(containsString("POSTULACIONES POR OFERTA")))
                .andExpect(content().string(containsString("Invitaste a Camila Soto a Backend Java.")))
                .andReturn();
        guardar("dashboard-empresa.html", r);
    }

    @Test
    void empresaSinOfertasInvitaAPublicar() throws Exception {
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "ACTIVO")).thenReturn(List.of());
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "CERRADO")).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/dashboard").session(sesion("empresa", "em1", "Nordic Labs")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Publica tu primera oferta")))
                .andExpect(content().string(containsString("Todavía no recibes postulaciones.")))
                .andExpect(content().string(not(containsString("<h2>Mejores candidatos</h2>"))))
                .andExpect(content().string(containsString("Aún no tienes ofertas activas.")))
                .andReturn();
        guardar("dashboard-empresa-vacio.html", r);
    }

    @Test
    void sinSesionVaAlLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
