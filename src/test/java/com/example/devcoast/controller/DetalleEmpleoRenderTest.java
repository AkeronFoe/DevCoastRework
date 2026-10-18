package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.example.devcoast.model.Proyecto;
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

// Ficha de una oferta con el shell náutico: pase con match, detalle, diálogo de postulación y vista de la empresa dueña
@WebMvcTest(EmpleoController.class)
@Import(SecurityConfig.class)
class DetalleEmpleoRenderTest {

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

    private Empleo oferta() {
        Empleo e = oferta("o1", "Backend Java", "Java, Spring Boot, Kafka", "ACTIVO");
        e.setEmpresaId("e1");
        e.setDescripcionRol("Construyes servicios de pagos.");
        e.setSoftSkills("Comunicación, Proactividad");
        e.setRequisitosDeseables("Experiencia con Kafka.");
        e.setVacantes(2);
        return e;
    }

    @Test
    void talentoVeElPaseConMatchLasHabilidadesYElDialogoDePostulacion() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of(), "Java", "Spring Boot")));
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta()));
        when(solicitudRepository.findByUsuarioIdAndEmpleoId("t1", "o1")).thenReturn(Optional.empty());
        Proyecto p = new Proyecto();
        p.setId("p1");
        p.setNombre("API de pagos");
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of(p));

        mockMvc.perform(get("/empleos/o1").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("detalles-empleo"))
                .andExpect(content().string(containsString("<h1 class=\"jt-t\"")))
                .andExpect(content().string(containsString("Backend Java")))
                .andExpect(content().string(containsString("MATCH MEDIO")))   // 2 de 3 habilidades = 67 %
                .andExpect(content().string(containsString("TU PERFIL VS. LA OFERTA")))
                .andExpect(content().string(containsString("(lo tienes)")))
                .andExpect(content().string(containsString("(te falta)")))
                .andExpect(content().string(containsString("id=\"btnAplicar\"")))
                .andExpect(content().string(containsString("id=\"btnGuardar\"")))
                .andExpect(content().string(containsString("Guardar oferta")))
                .andExpect(content().string(containsString("id=\"dlgAplicar\"")))
                .andExpect(content().string(containsString("name=\"proyectosIds\"")))
                .andExpect(content().string(containsString("API de pagos")))
                .andExpect(content().string(containsString("action=\"/empleos/o1/aplicar\"")))
                .andExpect(content().string(containsString("Construyes servicios de pagos.")))
                .andExpect(content().string(not(containsString("Ver candidatos"))));
    }

    @Test
    void quienYaPostuloNoVeElBotonDePostular() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of("o1"), "Java")));
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta()));
        when(solicitudRepository.findByUsuarioIdAndEmpleoId("t1", "o1")).thenReturn(Optional.of(new Solicitud()));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of());

        mockMvc.perform(get("/empleos/o1").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya aplicaste")))
                .andExpect(content().string(containsString("Ver mi solicitud")))
                .andExpect(content().string(containsString("Guardada ✓")))   // estaba entre sus favoritos
                .andExpect(content().string(not(containsString("id=\"btnAplicar\""))))
                .andExpect(content().string(not(containsString("id=\"dlgAplicar\""))));
    }

    @Test
    void laEmpresaDuenaVeCandidatosEditarYEliminar() throws Exception {
        MockHttpSession s = sesion("empresa");
        s.setAttribute("usuarioActualId", "e1");
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta()));
        when(solicitudRepository.countByEmpleoId("o1")).thenReturn(7L);
        when(solicitudRepository.findByEmpleoIdOrderByFechaSolicitudDesc("o1")).thenReturn(List.of());

        mockMvc.perform(get("/empleos/o1").session(s).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("Ver candidatos")))
                .andExpect(content().string(containsString("Editar oferta")))
                .andExpect(content().string(containsString("/crearEmpleo?editar=o1")))
                .andExpect(content().string(containsString("POSTULANTES")))
                .andExpect(content().string(containsString("action=\"/empleos/eliminar/o1\"")))
                .andExpect(content().string(not(containsString("id=\"btnAplicar\""))));
    }

    @Test
    void unaEmpresaAjenaNoPuedePostularse() throws Exception {
        MockHttpSession s = sesion("empresa");
        s.setAttribute("usuarioActualId", "otra");
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(oferta()));

        mockMvc.perform(get("/empleos/o1").session(s).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las empresas no pueden postularse")))
                .andExpect(content().string(containsString("Es una oferta de otra empresa.")))
                .andExpect(content().string(not(containsString("Editar oferta"))))
                .andExpect(content().string(not(containsString("id=\"btnAplicar\""))));
    }

    @Test
    void unaOfertaCerradaMuestraLaEtiquetaYNoDejaPostular() throws Exception {
        Empleo e = oferta();
        e.setEstado("CERRADO");
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento(List.of(), "Java")));
        when(empleoRepository.findById("o1")).thenReturn(Optional.of(e));
        when(solicitudRepository.findByUsuarioIdAndEmpleoId("t1", "o1")).thenReturn(Optional.empty());
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of());

        mockMvc.perform(get("/empleos/o1").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CERRADA")))
                .andExpect(content().string(containsString("Oferta cerrada")))
                .andExpect(content().string(not(containsString("id=\"btnAplicar\""))));
    }

    @Test
    void unaOfertaQueNoExisteVuelveAEmpleos() throws Exception {
        when(empleoRepository.findById("nada")).thenReturn(Optional.empty());
        mockMvc.perform(get("/empleos/nada").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
