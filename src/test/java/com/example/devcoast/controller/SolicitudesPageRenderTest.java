package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;

// smoke test temporal de la migracion de solicitudes.html al layout ws-
@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class SolicitudesPageRenderTest {

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
    private com.example.devcoast.repository.MensajeRepository mensajeRepository;
    @MockitoBean
    private com.example.devcoast.repository.EmpresaRepository empresaRepository;
    @MockitoBean
    private com.example.devcoast.repository.CobroRepository cobroRepository;
    @MockitoBean
    private com.example.devcoast.service.PlanService planService;

    private MockHttpSession sesionTalento() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t1");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return session;
    }

    private Solicitud solicitudDeEjemplo() {
        Solicitud s = new Solicitud();
        s.setId("s1");
        s.setEmpleoTitulo("Frontend Developer");
        s.setEmpleoCategoria("Frontend");
        s.setEmpleoModalidad("Remoto");
        s.setEmpleoEmpresaNombre("Nordic Labs");
        s.setEstado("revision");
        s.setFechaSolicitud(new Date());
        return s;
    }

    private Invitacion invitacionDeEjemplo() {
        Invitacion inv = new Invitacion();
        inv.setId("i1");
        inv.setEmpresaNombre("Nordic Labs");
        inv.setEmpresaSector("Software");
        inv.setEmpresaTamano("10-50");
        inv.setEmpleoId("e1");
        inv.setEmpleoTitulo("Frontend Developer");
        inv.setEmpleoModalidad("Remoto");
        inv.setEmpleoMostrarSalario(true);
        inv.setEmpleoSalarioMin(1800);
        inv.setEmpleoSalarioMax(2500);
        inv.setEmpleoMoneda("USD");
        inv.setMensaje("Nos encantó tu portafolio, ¿te interesaría conversar?");
        inv.setEstado("pendiente");
        inv.setFechaEnvio(new Date());
        return inv;
    }

    @Test
    void tabSolicitudesConDatosRendizaSinReventar() throws Exception {
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1"))
                .thenReturn(List.of(solicitudDeEjemplo()));
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc("t1")).thenReturn(List.of());

        mockMvc.perform(get("/solicitudes").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void tabInvitacionesConDatosRendizaSinReventar() throws Exception {
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc("t1")).thenReturn(List.of());
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc("t1"))
                .thenReturn(List.of(invitacionDeEjemplo()));

        mockMvc.perform(get("/solicitudes").param("tab", "invitaciones").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void detalleDeSolicitudSinHistorialRendizaSinReventar() throws Exception {
        Solicitud s = solicitudDeEjemplo();
        s.setEmpleoColorIcono(null);
        s.setEmpleoIcono(null);
        when(solicitudRepository.findByIdAndUsuarioId("s1", "t1")).thenReturn(Optional.of(s));

        mockMvc.perform(get("/solicitudes/s1").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void detalleDeSolicitudConHistorialYProyectosRendizaSinReventar() throws Exception {
        Solicitud s = solicitudDeEjemplo();
        s.setEmpleoColorIcono("#4fa0e8");
        s.setEmpleoIcono("code");
        s.setEmpleoMostrarSalario(true);
        s.setEmpleoSalarioMin(1800);
        s.setEmpleoSalarioMax(2500);
        s.setEmpleoMoneda("USD");
        s.setMensajeMotivacion("Me encantaría formar parte del equipo.");
        s.setProyectosIds(List.of("p1", "p2"));
        s.setHistorial(new java.util.ArrayList<>(List.of(
                new com.example.devcoast.model.EventoEstado("revision", new Date(), "Postulación enviada"),
                new com.example.devcoast.model.EventoEstado("aprobada", new Date(), "Aceptada por la empresa"))));
        when(solicitudRepository.findByIdAndUsuarioId("s1", "t1")).thenReturn(Optional.of(s));

        mockMvc.perform(get("/solicitudes/s1").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void ambasVaciasMuestranEmptyStateSinReventar() throws Exception {
        when(solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(anyString())).thenReturn(List.of());
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/solicitudes").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/solicitudes").param("tab", "invitaciones").session(sesionTalento()).with(csrf()))
                .andExpect(status().isOk());
    }
}
