package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import com.example.devcoast.model.Solicitud;
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
import com.example.devcoast.service.PlanService;

@WebMvcTest(EmpleoController.class)
@Import(SecurityConfig.class)
class GuardadosPageRenderTest {

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
        s.setAttribute("usuarioActualId", "t1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    private Empleo oferta(String id, String titulo, String stack, String estado) {
        Empleo e = new Empleo();
        e.setId(id);
        e.setTitulo(titulo);
        e.setEmpresaNombre("Astillero Digital");
        e.setCategoria("Programación");
        e.setModalidad("Remoto");
        e.setSeniority("Senior");
        e.setStack(stack);
        e.setEstado(estado);
        return e;
    }

    private Usuario talento(List<String> favoritos, String... stack) {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setStack(List.of(stack));
        u.setEmpleosFavoritos(new ArrayList<>(favoritos));
        return u;
    }

    @Test
    void conGuardadosMuestraLosPasesConMatchYElBotonQuitar() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of("o1", "o2"), "Java", "Spring Boot")));
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta("o1", "Backend Java", "Java, Spring Boot", "ACTIVO")));
        when(empleoRepository.findById("o2")).thenReturn(Optional.of(oferta("o2", "Plataforma Cloud", "Java, Kubernetes", "ACTIVO")));
        Solicitud ya = new Solicitud();
        ya.setEmpleoId("o1");
        when(solicitudRepository.findByUsuarioIdAndEmpleoIdIn(eq("t1"), anyList())).thenReturn(List.of(ya));

        mockMvc.perform(get("/empleos/guardados").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("// guardados")))
                .andExpect(content().string(containsString("2 ofertas guardadas")))
                .andExpect(content().string(containsString("1 con match alto")))
                .andExpect(content().string(containsString("1 ya solicitadas")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern("(?s).*Plataforma Cloud.*Backend Java.*")))
                .andExpect(content().string(containsString("data-empleo-id=\"o1\"")))
                .andExpect(content().string(containsString("Quitar Backend Java de guardados")))
                .andExpect(content().string(containsString("MATCH ALTO")))
                .andExpect(content().string(containsString("Solicitada")))
                .andExpect(content().string(matchesPattern("(?s).*/JS/guardados(-[0-9a-f]{32})?\\.js.*")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void sinGuardadosMuestraElEstadoVacioConEnlaceAEmpleos() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of(), "Java")));

        mockMvc.perform(get("/empleos/guardados").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nada guardado todavía.")))
                .andExpect(content().string(containsString("Explorar empleos")))
                .andExpect(content().string(not(containsString("id=\"guardadosLista\""))));
    }

    @Test
    void unaOfertaCerradaSeMarcaYNoSePuedePostular() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of("o3"), "Java")));
        when(empleoRepository.findById("o3")).thenReturn(Optional.of(oferta("o3", "Oferta vieja", "Java", "CERRADO")));
        when(solicitudRepository.findByUsuarioIdAndEmpleoIdIn(eq("t1"), anyList())).thenReturn(List.of());

        mockMvc.perform(get("/empleos/guardados").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CERRADA")))
                .andExpect(content().string(containsString(">Cerrada</button>")));
    }

    @Test
    void elFiltroDePendientesOcultaLasYaSolicitadas() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of("o1", "o2"), "Java")));
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta("o1", "Backend Java", "Java", "ACTIVO")));
        when(empleoRepository.findById("o2")).thenReturn(Optional.of(oferta("o2", "Plataforma Cloud", "Java", "ACTIVO")));
        Solicitud ya = new Solicitud();
        ya.setEmpleoId("o1");
        when(solicitudRepository.findByUsuarioIdAndEmpleoIdIn(eq("t1"), anyList())).thenReturn(List.of(ya));

        mockMvc.perform(get("/empleos/guardados").param("filtro", "pendientes").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plataforma Cloud")))
                .andExpect(content().string(not(containsString("data-empleo-id=\"o1\""))))
                .andExpect(content().string(containsString("limpiar filtros")));
    }

    @Test
    void unaCuentaDeEmpresaVaAlDashboard() throws Exception {
        mockMvc.perform(get("/empleos/guardados").session(sesion("empresa")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
