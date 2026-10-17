package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.CobroRepository;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.PlanService;

// ronda 4 del rediseño náutico: /solicitudes con pases de estado, pestañas, invitaciones y retirar
@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class SolicitudesPaseRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private ProyectoRepository proyectoRepository;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private CleanupService cleanupService;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private CobroRepository cobroRepository;
    @MockitoBean
    private PlanService planService;
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

    private Solicitud solicitud(String id, String cargo, String empresa, String estado, int dias) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoId("e-" + id);
        s.setEmpleoTitulo(cargo);
        s.setEmpleoEmpresaNombre(empresa);
        s.setEmpleoCategoria("Programacion · Backend");
        s.setEmpleoStack("Java, Docker, Kafka");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date(System.currentTimeMillis() - dias * 86_400_000L));
        s.setHistorial(new ArrayList<>(List.of(new EventoEstado(estado, new Date(), "x"))));
        return s;
    }

    private Invitacion invitacion(String id, String estado) {
        Invitacion i = new Invitacion();
        i.setId(id);
        i.setEmpresaNombre("Estudio Lumen");
        i.setEmpleoId("e9");
        i.setEmpleoTitulo("Diseño de producto");
        i.setEmpleoCategoria("Diseño · UI/UX");
        i.setEmpleoModalidad("Remoto");
        i.setEmpleoMostrarSalario(true);
        i.setEmpleoMoneda("COP");
        i.setEmpleoSalarioMin(4_000_000);
        i.setEmpleoSalarioMax(6_000_000);
        i.setMensaje("Nos encantó tu portafolio, ¿te interesaría conversar?");
        i.setEstado(estado);
        i.setFechaEnvio(new Date());
        return i;
    }

    @BeforeEach
    void datos() {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setStack(List.of("Java", "Docker"));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of(
                solicitud("s1", "Desarrollo Backend", "Nordic Labs", "revision", 2),
                solicitud("s2", "Diseño de interfaces", "Estudio Lumen", "aprobada", 5),
                solicitud("s3", "Desarrollo móvil", "Capsula", "rechazada", 9),
                solicitud("s4", "API de pagos", "Faro", "descartado", 12)));
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc("t1")).thenReturn(List.of(
                invitacion("i1", "pendiente"), invitacion("i2", "aceptada")));
        when(invitacionRepository.countByTalentoIdAndEstado("t1", "pendiente")).thenReturn(1L);
    }

    private String volcar(MvcResult r, String nombre) throws Exception {
        String html = r.getResponse().getContentAsString();
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), html);
        return html;
    }

    @Test
    void cadaEstadoTieneSuTalonSuLineaDeProgresoYSuSello() throws Exception {
        MvcResult r = mockMvc.perform(get("/solicitudes").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4 solicitudes · 1 en proceso · 1 invitación por responder")))
                // talones
                .andExpect(content().string(containsString("jt-stub s-rev")))
                .andExpect(content().string(containsString("jt-stub s-ace")))
                .andExpect(content().string(containsString("jt-stub s-rec")))
                .andExpect(content().string(containsString("En revisión")))
                .andExpect(content().string(containsString("Aceptada")))
                .andExpect(content().string(containsString("No avanzó")))
                // línea de progreso con texto oculto y etapa actual
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("(etapa actual)")))
                .andExpect(content().string(containsString("(aquí terminó)")))
                .andExpect(content().string(containsString("(completado)")))
                .andExpect(content().string(containsString("SIGUIENTE PASO")))
                .andExpect(content().string(containsString("ACEPTADA")))
                .andExpect(content().string(containsString("NO AVANZÓ")))
                .andExpect(content().string(containsString("MATCH 67%")))
                // retirar solo en revisión, con confirmación a la vista
                .andExpect(content().string(containsString("Retirar solicitud de Desarrollo Backend")))
                .andExpect(content().string(containsString("/solicitudes/s1/delete")))
                .andExpect(content().string(not(containsString("/solicitudes/s2/delete"))))
                .andExpect(content().string(containsString("Sí, retirar")))
                // ver oferta para las que ya no se pueden retirar
                .andExpect(content().string(containsString("/empleos/e-s2")))
                // aviso de acción pendiente real + pestañas
                .andExpect(content().string(containsString("Tienes 1 invitación por responder.")))
                .andExpect(content().string(containsString("Invitaciones <b>2</b>")))
                .andExpect(content().string(containsString("Todas <b>4</b>")))
                .andExpect(content().string(containsString("Cerradas <b>2</b>")))
                // shell
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn();
        volcar(r, "solicitudes-talento.html");
    }

    @Test
    void laPestanaEnProcesoSoloMuestraLasQueEstanEnRevision() throws Exception {
        mockMvc.perform(get("/solicitudes").param("estado", "proceso").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Desarrollo Backend")))
                .andExpect(content().string(not(containsString("Diseño de interfaces"))))
                .andExpect(content().string(not(containsString("Desarrollo móvil"))));
        mockMvc.perform(get("/solicitudes").param("estado", "cerradas").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Desarrollo móvil")))
                .andExpect(content().string(containsString("API de pagos")))
                .andExpect(content().string(not(containsString("Desarrollo Backend"))));
    }

    @Test
    void porEstadoPoneLaDeRevisionPrimero() throws Exception {
        String html = mockMvc.perform(get("/solicitudes").param("orden", "estado").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(html.indexOf("Desarrollo Backend") < html.indexOf("Diseño de interfaces"));
        org.junit.jupiter.api.Assertions.assertTrue(html.indexOf("Diseño de interfaces") < html.indexOf("Desarrollo móvil"));
    }

    @Test
    void laBusquedaFiltraPorCargoOEmpresaSinImportarTildes() throws Exception {
        mockMvc.perform(get("/solicitudes").param("q", "interfaces").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Diseño de interfaces")))
                .andExpect(content().string(not(containsString("Desarrollo Backend"))));
        mockMvc.perform(get("/solicitudes").param("q", "movil").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Desarrollo móvil")));
    }

    @Test
    void invitacionesSeMuestranComoPasesConAceptarYRechazar() throws Exception {
        MvcResult r = mockMvc.perform(get("/solicitudes").param("tab", "invitaciones").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estudio Lumen")))
                .andExpect(content().string(containsString("MENSAJE DE LA EMPRESA")))
                .andExpect(content().string(containsString("Nos encantó tu portafolio")))
                .andExpect(content().string(containsString("/invitaciones/i1/aceptar")))
                .andExpect(content().string(containsString("/invitaciones/i1/rechazar")))
                .andExpect(content().string(not(containsString("/invitaciones/i2/aceptar"))))
                .andExpect(content().string(containsString("COP 4.000.000 – 6.000.000")))
                // en esta pestaña no se repite el aviso
                .andExpect(content().string(not(containsString("Tienes 1 invitación por responder."))))
                .andReturn();
        volcar(r, "solicitudes-invitaciones.html");
    }

    @Test
    void sinSolicitudesInvitaAPostularse() throws Exception {
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of());
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc("t1")).thenReturn(List.of());
        when(invitacionRepository.countByTalentoIdAndEstado("t1", "pendiente")).thenReturn(0L);

        MvcResult r = mockMvc.perform(get("/solicitudes").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nada por aquí.")))
                .andExpect(content().string(containsString("Todavía no te has postulado a ninguna oferta.")))
                .andExpect(content().string(containsString("Ver empleos")))
                .andExpect(content().string(not(containsString("alertbar"))))
                .andReturn();
        volcar(r, "solicitudes-vacio.html");
    }

    @Test
    void categoriaSinResultadosOfreceVerTodas() throws Exception {
        mockMvc.perform(get("/solicitudes").param("estado", "aceptadas").param("q", "zzz").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("No tienes solicitudes en esta categoría.")))
                .andExpect(content().string(containsString("Ver todas")));
    }

    @Test
    void empresaVaAMisEmpleos() throws Exception {
        mockMvc.perform(get("/solicitudes").session(sesion("empresa")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misEmpleos"));
    }

    @Test
    void retirarBorraLaSolicitudPropiaYDejaUnAviso() throws Exception {
        Solicitud s = solicitud("s1", "Desarrollo Backend", "Nordic Labs", "revision", 2);
        s.setUsuarioId("t1");
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/solicitudes/s1/delete").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes"))
                .andExpect(flash().attribute("flashMessage", "Solicitud retirada."));
        verify(cleanupService).limpiarDatosDeSolicitud("s1");
        verify(solicitudRepository).deleteById("s1");
    }

    @Test
    void noSePuedeRetirarLaSolicitudDeOtraPersona() throws Exception {
        Solicitud s = solicitud("s9", "Otro", "Otra", "revision", 2);
        s.setUsuarioId("otro");
        when(solicitudRepository.findById("s9")).thenReturn(Optional.of(s));

        mockMvc.perform(post("/solicitudes/s9/delete").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(solicitudRepository, never()).deleteById("s9");
    }
}
