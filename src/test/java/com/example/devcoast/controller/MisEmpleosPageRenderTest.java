package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Solicitud;
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

@WebMvcTest(EmpleoController.class)
@Import(SecurityConfig.class)
class MisEmpleosPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;
    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private ProyectoRepository proyectoRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private CleanupService cleanupService;
    @MockitoBean
    private PlanService planService;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;

    private MockHttpSession sesion(String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", "em1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Nordic Labs");
        s.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");
        return s;
    }

    private Empleo oferta(String id, String titulo, String estado, boolean destacar) {
        Empleo e = new Empleo();
        e.setId(id);
        e.setEmpresaId("em1");
        e.setTitulo(titulo);
        e.setEmpresaNombre("Nordic Labs");
        e.setCategoria("Programacion · Backend");
        e.setModalidad("Remoto");
        e.setSeniority("Senior");
        e.setContrato("Indefinido");
        e.setVacantes(2);
        e.setStack("Java, Docker, Kafka");
        e.setEstado(estado);
        e.setDestacar(destacar);
        e.setFechaPublicacion(new Date(System.currentTimeMillis() - 3 * 86_400_000L));
        return e;
    }

    private Solicitud sol(String id, String empleoId, String estado) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoId(empleoId);
        s.setEstado(estado);
        return s;
    }

    private void datos(int limite) {
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "ACTIVO"))
                .thenReturn(List.of(oferta("e1", "Backend Java", "ACTIVO", true), oferta("e2", "Frontend React", "ACTIVO", false)));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "CERRADO"))
                .thenReturn(List.of(oferta("e3", "DevOps", "CERRADO", false)));
        when(solicitudRepository.findByEmpleoIdIn(any())).thenReturn(List.of(
                sol("s1", "e1", "revision"), sol("s2", "e1", "revision"), sol("s3", "e1", "aprobada"),
                sol("s4", "e2", "rechazada"), sol("s5", "e3", null)));
        when(empresaRepository.findById("em1")).thenReturn(Optional.of(new Empresa()));
        when(planService.limiteVacantesActivas(any())).thenReturn(limite);
    }

    private void guardar(String nombre, MvcResult r) throws Exception {
        Files.createDirectories(Path.of("target", "render"));
        Files.writeString(Path.of("target", "render", nombre), r.getResponse().getContentAsString());
    }

    @Test
    void activasConElConteoDePostulacionesPorEstado() throws Exception {
        datos(3);
        MvcResult r = mockMvc.perform(get("/misEmpleos").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("misEmpleos"))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("// mis ofertas")))
                .andExpect(content().string(containsString("2 de 3 ofertas activas · 2 postulaciones en revisión")))
                .andExpect(content().string(containsString("Backend Java")))
                .andExpect(content().string(containsString("DESTACADA")))
                .andExpect(content().string(containsString("2 en revisión · 1 aceptada")))
                .andExpect(content().string(containsString("0 en revisión · 1 rechazada")))
                .andExpect(content().string(containsString("href=\"/empleos/e1/postulaciones\"")))
                .andExpect(content().string(containsString("action=\"/empleos/cerrar/e1\"")))
                .andExpect(content().string(containsString("href=\"/crearEmpleo?editar=e1\"")))
                .andExpect(content().string(containsString("Publicar oferta")))
                .andExpect(content().string(not(containsString("DevOps"))))
                .andExpect(content().string(not(containsString("alertbar"))))
                .andReturn();
        guardar("misofertas-activas.html", r);
    }

    @Test
    void cerradasSePuedenReabrirYEliminar() throws Exception {
        datos(3);
        MvcResult r = mockMvc.perform(get("/misEmpleos").param("tab", "cerradas").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DevOps")))
                .andExpect(content().string(containsString("action=\"/empleos/reabrir/e3\"")))
                .andExpect(content().string(containsString("action=\"/empleos/eliminar/e3\"")))
                .andExpect(content().string(containsString("Se borran la oferta, sus postulaciones y sus invitaciones")))
                .andExpect(content().string(not(containsString("action=\"/empleos/cerrar/"))))
                .andExpect(content().string(not(containsString("Backend Java"))))
                .andReturn();
        guardar("misofertas-cerradas.html", r);
    }

    @Test
    void enElLimiteDelPlanAvisaYOcultaPublicar() throws Exception {
        datos(2);
        MvcResult r = mockMvc.perform(get("/misEmpleos").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Llegaste al límite de 2 ofertas activas.")))
                .andExpect(content().string(not(containsString(">Publicar oferta<"))))
                .andReturn();
        guardar("misofertas-limite.html", r);
    }

    @Test
    void sinOfertasInvitaAPublicar() throws Exception {
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(anyString(), anyString())).thenReturn(List.of());
        when(planService.limiteVacantesActivas(any())).thenReturn(3);
        mockMvc.perform(get("/misEmpleos").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(containsString("Mar en calma.")))
                .andExpect(content().string(containsString("No tienes ofertas activas.")));
    }

    @Test
    void soloLaEmpresaEntra() throws Exception {
        mockMvc.perform(get("/misEmpleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(get("/misEmpleos").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
