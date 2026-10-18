package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.PlanService;

// ronda 6: Postulaciones de la cuenta de Empresa, agrupadas por oferta, con el match de cada candidato
@WebMvcTest(CandidatosController.class)
@Import(SecurityConfig.class)
class PostulacionesPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private PlanService planService;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;

    private MockHttpSession sesion(String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", "em1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Nordic Labs");
        s.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");
        return s;
    }

    private Empleo oferta(String id, String titulo, String estado) {
        Empleo e = new Empleo();
        e.setId(id);
        e.setEmpresaId("em1");
        e.setTitulo(titulo);
        e.setCategoria("Programacion · Backend");
        e.setEstado(estado);
        return e;
    }

    private Solicitud sol(String id, String empleoId, String usuarioId, String nombre, String estado, int hace) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoId(empleoId);
        s.setUsuarioId(usuarioId);
        s.setUsuarioNombre(nombre);
        s.setUsuarioRolTitulo("Backend Developer");
        s.setEmpleoTitulo("Backend Java");
        s.setEmpleoStack("Java, Docker, Kafka, AWS");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date(System.currentTimeMillis() - hace * 86_400_000L));
        List<EventoEstado> h = new ArrayList<>();
        h.add(new EventoEstado("revision", s.getFechaSolicitud(), "Enviada"));
        s.setHistorial(h);
        return s;
    }

    private void datos(boolean pro) {
        when(empleoRepository.findByEmpresaIdOrderByIdDesc("em1"))
                .thenReturn(List.of(oferta("e1", "Backend Java", "ACTIVO"), oferta("e2", "DevOps", "CERRADO"), oferta("e3", "Sin gente", "ACTIVO")));
        when(solicitudRepository.findByEmpleoIdIn(any())).thenReturn(new ArrayList<>(List.of(
                sol("s1", "e1", "t1", "Camila Soto", "revision", 1),
                sol("s2", "e1", "t2", "Mateo Vega", "aprobada", 5),
                sol("s3", "e1", "t3", "Ana Ríos", "descartado", 7),
                sol("s4", "e2", "t1", "Camila Soto", "rechazada", 20))));
        Usuario t1 = new Usuario();
        t1.setId("t1");
        t1.setStack(List.of("Java", "Docker"));
        Usuario t2 = new Usuario();
        t2.setId("t2");
        t2.setStack(List.of("Java", "Docker", "Kafka", "AWS"));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(t1, t2));
        when(empresaRepository.findById("em1")).thenReturn(Optional.of(new Empresa()));
        when(planService.esPro(any())).thenReturn(pro);
    }

    private void guardar(String nombre, MvcResult r) throws Exception {
        Files.createDirectories(Path.of("target", "render"));
        Files.writeString(Path.of("target", "render", nombre), r.getResponse().getContentAsString());
    }

    @Test
    void agrupaPorOfertaConMatchYEstadoDeCadaCandidato() throws Exception {
        datos(false);
        MvcResult r = mockMvc.perform(get("/candidatos").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("candidatos"))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("// postulaciones")))
                .andExpect(content().string(containsString("<span id=\"cTotal\">4</span>")))
                .andExpect(content().string(containsString("Backend Java")))
                .andExpect(content().string(containsString("DevOps")))
                .andExpect(content().string(containsString("CERRADA")))
                // la oferta sin postulaciones no aparece
                .andExpect(content().string(not(containsString("Sin gente"))))
                .andExpect(content().string(containsString("1 en revisión · 1 aceptada · 1 no avanzó")))
                .andExpect(content().string(containsString("Match 50%")))     // Java, Docker de 4
                .andExpect(content().string(containsString("Match 100%")))
                .andExpect(content().string(containsString("Sin match")))      // Ana Ríos no tiene perfil cargado
                .andExpect(content().string(containsString("href=\"/empleos/e1/postulaciones/s2\"")))
                .andExpect(content().string(containsString("action=\"/empleos/e1/postulaciones/s2/aprobar\"")))
                .andExpect(content().string(containsString("action=\"/empleos/e1/postulaciones/s3/reconsiderar\"")))
                .andExpect(content().string(containsString("data-url=\"/empleos/e1/postulaciones/s1/estado\"")))
                .andExpect(content().string(containsString("data-estado=\"rechazada\"")))   // el descartado se agrupa con "no avanzó"
                .andExpect(content().string(containsString("Con el plan Gratis se registra la tarifa de colocación")))
                .andReturn();
        guardar("postulaciones.html", r);
    }

    @Test
    void conPlanProNoMencionaLaTarifa() throws Exception {
        datos(true);
        mockMvc.perform(get("/candidatos").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(not(containsString("tarifa de colocación"))))
                .andExpect(content().string(containsString("La persona recibirá un aviso de que fue aceptada.")));
    }

    @Test
    void filtraPorEstadoYPorNombreSinTildes() throws Exception {
        datos(false);
        mockMvc.perform(get("/candidatos").param("estado", "aceptadas").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(containsString("Mateo Vega")))
                .andExpect(content().string(not(containsString("Camila Soto"))))
                .andExpect(content().string(not(containsString("DevOps"))))
                .andExpect(content().string(containsString("data-estado=\"aceptadas\"")))
                // el total y los contadores no cambian con el filtro
                .andExpect(content().string(containsString("<span id=\"cTotal\">4</span>")));
        mockMvc.perform(get("/candidatos").param("q", "rios").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(containsString("Ana Ríos")))
                .andExpect(content().string(not(containsString("Mateo Vega"))));
        MvcResult r = mockMvc.perform(get("/candidatos").param("estado", "revision").param("q", "zzz").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(containsString("No hay postulaciones con esos filtros.")))
                .andExpect(content().string(containsString("limpiar filtros")))
                .andReturn();
        guardar("postulaciones-vacio-filtro.html", r);
    }

    @Test
    void sinPostulacionesInvitaAVerMisOfertas() throws Exception {
        when(empleoRepository.findByEmpresaIdOrderByIdDesc("em1")).thenReturn(List.of(oferta("e1", "Backend Java", "ACTIVO")));
        when(solicitudRepository.findByEmpleoIdIn(any())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/candidatos").session(sesion("empresa")).with(csrf()))
                .andExpect(content().string(containsString("Todavía nadie se ha postulado.")));
    }

    @Test
    void soloLaEmpresaEntra() throws Exception {
        mockMvc.perform(get("/candidatos").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(get("/candidatos").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
