package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.EventoEstado;
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

// Postulaciones de una sola oferta con el shell náutico: los mismos pases que /candidatos, fase del proceso y filtros
@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class PostulacionesOfertaRenderTest {

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

    private MockHttpSession sesionEmpresa() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", "em1");
        s.setAttribute("usuarioActualTipo", "empresa");
        s.setAttribute("usuarioActualNombre", "Nordic Labs");
        s.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");
        return s;
    }

    private Solicitud solicitud(String id, String usuarioId, String nombre, String estado) {
        Solicitud s = new Solicitud();
        s.setUsuarioNombre(nombre);
        s.setId(id);
        s.setEmpleoId("emp1");
        s.setUsuarioId(usuarioId);
        s.setEmpleoTitulo("Desarrollo Backend");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date());
        s.setHistorial(new ArrayList<>(List.of(new EventoEstado(estado, new Date(), "x"))));
        return s;
    }

    private Usuario talento(String id, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setTipo("talento");
        u.setStack(List.of("Java", "Docker"));
        return u;
    }

    @BeforeEach
    void datos() {
        Empleo e = new Empleo();
        e.setId("emp1");
        e.setEmpresaId("em1");
        e.setTitulo("Desarrollo Backend");
        e.setCategoria("Programacion · Backend");
        e.setEstado("ACTIVO");
        e.setFaseReclutamiento("ENTREVISTAS");
        e.setStack("Java, Docker");
        when(empleoRepository.findById("emp1")).thenReturn(Optional.of(e));
        when(solicitudRepository.findByEmpleoIdOrderByFechaSolicitudDesc("emp1")).thenReturn(List.of(
                solicitud("s1", "t1", "Valentina Ríos", "revision"),
                solicitud("s2", "t2", "Mateo Salazar", "aprobada"),
                solicitud("s3", "t3", "Camila Ortiz", "rechazada")));
        when(usuarioRepository.findAllById(org.mockito.ArgumentMatchers.<Iterable<String>>any())).thenReturn(List.of(
                talento("t1", "Valentina Ríos"), talento("t2", "Mateo Salazar"), talento("t3", "Camila Ortiz")));
    }

    @Test
    void muestraLosPasesDeLaOfertaConLosContadoresYLaFaseActual() throws Exception {
        mockMvc.perform(get("/empleos/emp1/postulaciones").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("postulaciones-empleo"))
                .andExpect(content().string(containsString("// postulaciones de la oferta")))
                .andExpect(content().string(containsString("Desarrollo Backend")))
                .andExpect(content().string(containsString("Valentina Ríos")))
                .andExpect(content().string(containsString("Mateo Salazar")))
                .andExpect(content().string(containsString("Camila Ortiz")))
                .andExpect(content().string(containsString("id=\"cTotal\">3<")))
                .andExpect(content().string(containsString("href=\"/empleos/emp1/postulaciones/s1\"")))
                // fase del proceso: la de la oferta queda marcada
                .andExpect(content().string(containsString("data-fase=\"ENTREVISTAS\" aria-pressed=\"true\"")))
                .andExpect(content().string(containsString("data-fase=\"ABIERTO\" aria-pressed=\"false\"")))
                .andExpect(content().string(matchesPattern("(?s).*/JS/postulaciones-fase(-[0-9a-f]{32})?\\.js.*")))
                // ya no hay tablero kanban ni el shell anterior
                .andExpect(content().string(not(containsString("pe-board"))))
                .andExpect(content().string(not(containsString("ws-top"))));
    }

    @Test
    void elFiltroDeEstadoSoloDejaLasQueCumplen() throws Exception {
        mockMvc.perform(get("/empleos/emp1/postulaciones?estado=aceptadas").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mateo Salazar")))
                .andExpect(content().string(not(containsString("Valentina Ríos"))))
                .andExpect(content().string(not(containsString("Camila Ortiz"))))
                .andExpect(content().string(containsString("limpiar filtros")));
    }

    @Test
    void aceptaLosValoresDeEstadoDeAntes() throws Exception {
        mockMvc.perform(get("/empleos/emp1/postulaciones?estado=rechazada").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Camila Ortiz")))
                .andExpect(content().string(not(containsString("Mateo Salazar"))));
    }

    @Test
    void sinPostulacionesMuestraElEstadoVacio() throws Exception {
        when(solicitudRepository.findByEmpleoIdOrderByFechaSolicitudDesc("emp1")).thenReturn(List.of());

        mockMvc.perform(get("/empleos/emp1/postulaciones").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía nadie se ha postulado a esta oferta")));
    }

    @Test
    void laOfertaDeOtraEmpresaRedirige() throws Exception {
        MockHttpSession otra = sesionEmpresa();
        otra.setAttribute("usuarioActualId", "otra");
        mockMvc.perform(get("/empleos/emp1/postulaciones").session(otra).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misEmpleos"));
    }
}
