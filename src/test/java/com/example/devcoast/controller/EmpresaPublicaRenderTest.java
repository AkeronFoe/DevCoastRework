package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
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

// Página de una empresa con el shell náutico: ficha y ofertas activas como pases
@WebMvcTest(EmpleoController.class)
@Import(SecurityConfig.class)
class EmpresaPublicaRenderTest {

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
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    private Empleo oferta(String id, String titulo) {
        Empleo e = new Empleo();
        e.setId(id);
        e.setEmpresaId("em1");
        e.setEmpresaNombre("Nordic Labs");
        e.setTitulo(titulo);
        e.setCategoria("Programacion · Backend");
        e.setModalidad("Remoto");
        e.setEstado("ACTIVO");
        e.setStack("Java, Docker");
        return e;
    }

    @Test
    void muestraLaFichaYLasOfertasActivasComoPases() throws Exception {
        Empresa emp = new Empresa();
        emp.setId("em1");
        emp.setNombreEmpresa("Nordic Labs");
        emp.setSector("Fintech");
        emp.setTamano("11 – 50 personas");
        emp.setDescripcion("Construimos herramientas de pago.");
        emp.setSitioWeb("https://nordiclabs.io");
        when(empresaRepository.findById("em1")).thenReturn(Optional.of(emp));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "ACTIVO"))
                .thenReturn(List.of(oferta("e1", "Desarrollo Backend"), oferta("e2", "Plataforma de pagos")));

        mockMvc.perform(get("/empresa/em1").session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("empresa-perfil"))
                .andExpect(content().string(containsString("// perfil de empresa")))
                .andExpect(content().string(containsString("Nordic Labs")))
                .andExpect(content().string(containsString("Construimos herramientas de pago.")))
                .andExpect(content().string(containsString("2 ofertas activas")))
                .andExpect(content().string(containsString("Desarrollo Backend")))
                .andExpect(content().string(containsString("Plataforma de pagos")))
                .andExpect(content().string(containsString("href=\"/empleos/e1\"")))
                .andExpect(content().string(matchesPattern("(?s).*/CSS/nautico-empresa-publica(-[0-9a-f]{32})?\\.css.*")))
                .andExpect(content().string(not(containsString("ws-top"))));
    }

    @Test
    void sinOfertasMuestraElEstadoVacio() throws Exception {
        Empresa emp = new Empresa();
        emp.setId("em1");
        emp.setNombreEmpresa("Nordic Labs");
        when(empresaRepository.findById("em1")).thenReturn(Optional.of(emp));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("em1", "ACTIVO")).thenReturn(List.of());

        mockMvc.perform(get("/empresa/em1").session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no tiene posiciones abiertas")));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/empresa/em1"))
                .andExpect(status().is3xxRedirection());
    }
}
