package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
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

@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class PostularEndpointTest {

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

    private Empleo oferta(String estado) {
        Empleo e = new Empleo();
        e.setId("e1");
        e.setTitulo("Backend Java");
        e.setEmpresaId("emp1");
        e.setEmpresaNombre("Nordic Labs");
        e.setEstado(estado);
        return e;
    }

    @BeforeEach
    void base() {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(solicitudRepository.findByUsuarioIdAndEmpleoId("t1", "e1")).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            if (s.getId() == null)
                s.setId("sol1");
            return s;
        });
    }

    @Test
    void postularCreaLaSolicitudYDevuelveSuId() throws Exception {
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(oferta("ACTIVO")));

        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.solicitudId").value("sol1"))
                .andExpect(jsonPath("$.estado").value("revision"))
                .andExpect(jsonPath("$.descartado").value(false));
        verify(notificacionRepository).save(any());
    }

    @Test
    void conScreeningQueNoPasaAvisaQueFueDescartada() throws Exception {
        Empleo e = oferta("ACTIVO");
        e.setScreeningActivo(true);
        e.setScreeningKeywords("kubernetes");
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(e));

        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.estado").value("descartado"))
                .andExpect(jsonPath("$.descartado").value(true));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void siYaAplicoResponde409() throws Exception {
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(oferta("ACTIVO")));
        Solicitud previa = new Solicitud();
        previa.setId("prev");
        when(solicitudRepository.findByUsuarioIdAndEmpleoId("t1", "e1")).thenReturn(Optional.of(previa));

        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).session(sesion("talento")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.yaAplicaste").value(true));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void ofertaInactivaOInexistenteNoSeAplica() throws Exception {
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(oferta("CERRADO")));
        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).session(sesion("talento")).with(csrf()))
                .andExpect(status().isConflict());

        when(empleoRepository.findById(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/empleos/zzz/postular").accept(MediaType.APPLICATION_JSON).session(sesion("talento")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void empresaYVisitanteNoPuedenPostular() throws Exception {
        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).session(sesion("empresa")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/empleos/e1/postular").accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isUnauthorized());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void elFormularioClasicoSigueRedirigiendoAlDetalleDeLaSolicitud() throws Exception {
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(oferta("ACTIVO")));

        mockMvc.perform(post("/empleos/e1/aplicar").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes/sol1"));
    }

    @Test
    void elFormularioClasicoConOfertaInactivaVuelveAlDetalleDeLaOferta() throws Exception {
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(oferta("CERRADO")));

        mockMvc.perform(post("/empleos/e1/aplicar").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleos/e1"));
    }
}
