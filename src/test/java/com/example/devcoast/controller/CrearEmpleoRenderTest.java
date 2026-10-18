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
import com.example.devcoast.model.Empleo;
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

// Publicar / editar oferta con el shell náutico: el formulario por pasos conserva los campos que espera el servidor
@WebMvcTest(EmpleoController.class)
@Import(SecurityConfig.class)
class CrearEmpleoRenderTest {

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

    private MockHttpSession sesion(String id, String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", id);
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Nordic Labs");
        s.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");
        return s;
    }

    @Test
    void empresaVeElFormularioPorPasosEnElShellNautico() throws Exception {
        when(planService.limiteDestacados(null)).thenReturn(3);

        mockMvc.perform(get("/crearEmpleo").session(sesion("em1", "empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("crearEmpleo"))
                .andExpect(content().string(containsString("// publicar vacante")))
                .andExpect(content().string(containsString("Publicar oferta")))
                .andExpect(content().string(matchesPattern("(?s).*/CSS/nautico-formulario(-[0-9a-f]{32})?\\.css.*")))
                // el formulario sigue enviándose al mismo endpoint, con los campos que valida el servidor
                .andExpect(content().string(containsString("action=\"/empleos/guardar\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"empresaNombre\"")))
                .andExpect(content().string(containsString("name=\"titulo\"")))
                .andExpect(content().string(containsString("name=\"stack\"")))
                .andExpect(content().string(containsString("id=\"btn-publicar\"")))
                .andExpect(content().string(containsString("id=\"paso-3\"")))
                .andExpect(content().string(matchesPattern("(?s).*/JS/steps(-[0-9a-f]{32})?\\.js.*")))
                .andExpect(content().string(not(containsString("ws-top"))))
                .andExpect(content().string(not(containsString("wsjobs"))));
    }

    @Test
    void editarCargaLosDatosDeLaOferta() throws Exception {
        Empleo e = new Empleo();
        e.setId("emp1");
        e.setEmpresaId("em1");
        e.setTitulo("Ingeniera Backend");
        e.setEmpresaNombre("Nordic Labs");
        when(empleoRepository.findById("emp1")).thenReturn(Optional.of(e));

        mockMvc.perform(get("/crearEmpleo?editar=emp1").session(sesion("em1", "empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar oferta")))
                .andExpect(content().string(containsString("value=\"Ingeniera Backend\"")))
                .andExpect(content().string(containsString("name=\"id\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    void elTalentoNoPuedePublicar() throws Exception {
        mockMvc.perform(get("/crearEmpleo").session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleos"));
    }
}
