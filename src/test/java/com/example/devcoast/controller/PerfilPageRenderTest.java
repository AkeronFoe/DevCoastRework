package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;

// smoke test temporal de la migracion de perfil.html al layout ws-: cubre
// talento con datos, talento recien creado (todo null) y empresa, que es
// justo donde una expresion de Thymeleaf mal protegida contra null revienta
@WebMvcTest(PageController.class)
@Import(SecurityConfig.class)
class PerfilPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private ProyectoRepository proyectoRepository;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private CleanupService cleanupService;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private com.example.devcoast.repository.CobroRepository cobroRepository;
    @MockitoBean
    private com.example.devcoast.service.PlanService planService;

    @Test
    void perfilDeTalentoConDatosCompletosRendizaSinReventar() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780001");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setRolTitulo("Frontend Developer");
        u.setBio("Construyo interfaces claras.");
        u.setStack(List.of("React", "TypeScript"));
        u.setHabilidadesBlandas(List.of("Comunicación"));
        u.setUbicacion("Santiago, Chile");
        u.setModalidad("Remoto");
        u.setDisponibilidad("Full-time");
        u.setEspecialidad("Frontend");
        u.setLinkedinUrl("https://linkedin.com/in/valentina");
        u.setGithubUrl("https://github.com/valentina");
        u.setPerfilPublico(true);
        u.setDestacar(true);
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of());
        when(invitacionRepository.countByTalentoIdAndEstado(anyString(), anyString())).thenReturn(0L);
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(anyString())).thenReturn(List.of());
        when(solicitudRepository.countByUsuarioIdAndEstado(anyString(), anyString())).thenReturn(0L);
        when(solicitudRepository.countByUsuarioId(anyString())).thenReturn(0L);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t1");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");

        mockMvc.perform(get("/perfil").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void perfilDeTalentoReciénCreadoSinDatosRendizaSinReventar() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780002");
        u.setNombre("Nuevo Talento");
        u.setTipo("talento");
        when(usuarioRepository.findById("t2")).thenReturn(Optional.of(u));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t2")).thenReturn(List.of());
        when(invitacionRepository.countByTalentoIdAndEstado(anyString(), anyString())).thenReturn(0L);
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(anyString())).thenReturn(List.of());
        when(solicitudRepository.countByUsuarioIdAndEstado(anyString(), anyString())).thenReturn(0L);
        when(solicitudRepository.countByUsuarioId(anyString())).thenReturn(0L);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t2");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Nuevo Talento");
        session.setAttribute("usuarioActualCorreo", "nuevo@correo.com");

        mockMvc.perform(get("/perfil").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void perfilDeEmpresaRendizaSinReventar() throws Exception {
        Empresa e = new Empresa();
        e.setId("e1");
        e.setNombreEmpresa("Nordic Labs");
        e.setCorreo("contacto@nordiclabs.io");
        when(empresaRepository.findById("e1")).thenReturn(Optional.of(e));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(anyString(), anyString())).thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "e1");
        session.setAttribute("usuarioActualTipo", "empresa");
        session.setAttribute("usuarioActualNombre", "Nordic Labs");
        session.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");

        mockMvc.perform(get("/perfil").session(session).with(csrf()))
                .andExpect(status().isOk());
    }
}
