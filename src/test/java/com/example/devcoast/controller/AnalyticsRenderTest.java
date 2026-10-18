package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
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

// Analytics con el shell náutico: una versión para Talento ("Mi actividad") y otra para Empresa
@WebMvcTest(PageController.class)
@Import(SecurityConfig.class)
class AnalyticsRenderTest {

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
    private CobroRepository cobroRepository;
    @MockitoBean
    private PlanService planService;

    private MockHttpSession sesion(String id, String tipo, String nombre) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualTipo", tipo);
        session.setAttribute("usuarioActualNombre", nombre);
        session.setAttribute("usuarioActualCorreo", "cuenta@correo.com");
        return session;
    }

    @Test
    void talentoVeMiActividadConSusGraficas() throws Exception {
        mockMvc.perform(get("/analytics").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("analytics"))
                .andExpect(content().string(containsString("Mi actividad")))
                .andExpect(content().string(containsString("id=\"talChartEstado\"")))
                .andExpect(content().string(containsString("Plataforma DevCoast")))
                .andExpect(content().string(matchesPattern("(?s).*/CSS/nautico-analytics(-[0-9a-f]{32})?\\.css.*")))
                .andExpect(content().string(not(containsString("empChartEstado"))))
                .andExpect(content().string(not(containsString("ws-top"))));
    }

    @Test
    void empresaVeMisAnalyticsConSusGraficas() throws Exception {
        mockMvc.perform(get("/analytics").session(sesion("e1", "empresa", "Astillero Digital")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mis analytics")))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("id=\"empChartEstado\"")))
                .andExpect(content().string(containsString("Aún no tienes empleos publicados.")))
                .andExpect(content().string(not(containsString("talChartEstado"))));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/analytics"))
                .andExpect(status().is3xxRedirection());
    }
}
