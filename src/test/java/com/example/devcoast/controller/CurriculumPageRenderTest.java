package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

// smoke test temporal de la migracion de curriculum.html al layout ws-: cubre
// el propio CV en modo edicion (con datos y recien creado) y el modo lectura
// que usa una empresa para revisar la postulacion de un candidato
@WebMvcTest(controllers = { PageController.class, SolicitudController.class })
@Import(SecurityConfig.class)
class CurriculumPageRenderTest {

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

    private MockHttpSession sesionTalento(String id) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return session;
    }

    private MockHttpSession sesionEmpresa() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "e1");
        session.setAttribute("usuarioActualTipo", "empresa");
        session.setAttribute("usuarioActualNombre", "Nordic Labs");
        session.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");
        return session;
    }

    @Test
    void propioCvConDatosCompletosRendizaSinReventar() throws Exception {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setRolTitulo("Frontend Developer");
        u.setBio("Construyo interfaces claras.");
        u.setStack(List.of("React", "TypeScript"));
        u.setHabilidadesBlandas(List.of("Comunicación", "Liderazgo"));
        u.setCvBullets(List.of("Reduje el tiempo de deploy un 40%."));
        u.setUbicacion("Santiago, Chile");
        u.setModalidad("Remoto");
        u.setDisponibilidad("Full-time");
        u.setEspecialidad("Frontend");
        u.setSalarioEsperado("USD 1800 - 2500");
        u.setLinkedinUrl("https://linkedin.com/in/valentina");
        u.setGithubUrl("https://github.com/valentina");
        u.setNivelEspanol(5);
        u.setNivelIngles(4);

        Usuario.CvExperiencia exp = new Usuario.CvExperiencia();
        exp.setCargo("Frontend Developer");
        exp.setEmpresa("Nordic Labs");
        exp.setPeriodo("2023 - hoy");
        exp.setModalidad("Remoto");
        exp.setDescripcion("Construí el design system.");
        u.setCvExperiencias(List.of(exp));

        Usuario.CvEducacion edu = new Usuario.CvEducacion();
        edu.setInstitucion("Universidad de Chile");
        edu.setTitulo("Ingeniería Civil Informática");
        edu.setPeriodo("2018 - 2023");
        u.setCvEducaciones(List.of(edu));

        Proyecto p = new Proyecto();
        p.setId("p1");
        p.setNombre("Vitrina de proyectos");
        p.setStack(List.of("React", "Spring"));
        p.setColorIcono("#4fa0e8");

        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of(p));

        mockMvc.perform(get("/curriculum").session(sesionTalento("t1")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void propioCvReciénCreadoSinDatosRendizaSinReventar() throws Exception {
        Usuario u = new Usuario();
        u.setId("t2");
        u.setNombre("Nuevo Talento");
        u.setTipo("talento");

        when(usuarioRepository.findById("t2")).thenReturn(Optional.of(u));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t2")).thenReturn(List.of());

        mockMvc.perform(get("/curriculum").session(sesionTalento("t2")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void revisionDeEmpresaConProyectosYMensajeRendizaSinReventar() throws Exception {
        Empleo empleo = new Empleo();
        empleo.setId("emp1");
        empleo.setEmpresaId("e1");

        Solicitud s = new Solicitud();
        s.setId("s1");
        s.setEmpleoId("emp1");
        s.setUsuarioId("t1");
        s.setUsuarioCorreo("valentina@correo.com");
        s.setEstado("revision");
        s.setFechaSolicitud(new Date());
        s.setMensajeMotivacion("Me encantaría formar parte del equipo.");
        s.setProyectosIds(List.of("p1"));

        Usuario candidato = new Usuario();
        candidato.setId("t1");
        candidato.setNombre("Valentina Ríos");
        candidato.setTipo("talento");

        Proyecto p = new Proyecto();
        p.setId("p1");
        p.setNombre("Vitrina de proyectos");
        p.setStack(List.of("React", "Spring", "Docker", "AWS", "Redis"));
        p.setLinkProyecto("https://vitrina.dev");

        when(empleoRepository.findById("emp1")).thenReturn(Optional.of(empleo));
        when(solicitudRepository.findByIdAndEmpleoId("s1", "emp1")).thenReturn(Optional.of(s));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(candidato));
        when(proyectoRepository.findById("p1")).thenReturn(Optional.of(p));

        mockMvc.perform(get("/empleos/emp1/postulaciones/s1").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void revisionDeEmpresaAprobadaSinProyectosNiMensajeRendizaSinReventar() throws Exception {
        Empleo empleo = new Empleo();
        empleo.setId("emp1");
        empleo.setEmpresaId("e1");

        Solicitud s = new Solicitud();
        s.setId("s2");
        s.setEmpleoId("emp1");
        s.setUsuarioId("t3");
        s.setUsuarioCorreo("otro@correo.com");
        s.setEstado("aprobada");
        s.setFechaSolicitud(new Date());

        Usuario candidato = new Usuario();
        candidato.setId("t3");
        candidato.setNombre("Otro Candidato");
        candidato.setTipo("talento");

        when(empleoRepository.findById("emp1")).thenReturn(Optional.of(empleo));
        when(solicitudRepository.findByIdAndEmpleoId("s2", "emp1")).thenReturn(Optional.of(s));
        when(usuarioRepository.findById("t3")).thenReturn(Optional.of(candidato));

        mockMvc.perform(get("/empleos/emp1/postulaciones/s2").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk());
    }
    // ===== rediseño náutico: la hoja de CV, el diálogo de edición y la revisión de la empresa =====

    @Test
    void elDuenoVeLaHojaConSusDatosElDialogoYLaCompletitud() throws Exception {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setRolTitulo("Frontend Developer");
        u.setBio("Construyo interfaces claras.");
        u.setStack(List.of("React", "TypeScript"));
        u.setHabilidadesBlandas(List.of("Comunicación"));
        u.setCvBullets(List.of("Reduje el tiempo de deploy un 40%.", "Lideré el rediseño."));
        u.setNivelIngles(4);
        Usuario.CvExperiencia exp = new Usuario.CvExperiencia();
        exp.setCargo("Frontend Developer");
        exp.setEmpresa("Nordic Labs");
        exp.setPeriodo("2023 - hoy");
        u.setCvExperiencias(List.of(exp));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of());

        mockMvc.perform(get("/curriculum").session(sesionTalento("t1")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("// tu currículum")))
                .andExpect(content().string(containsString("class=\"cv-name\"")))
                .andExpect(content().string(containsString("Valentina Ríos")))
                .andExpect(content().string(containsString("Nordic Labs")))
                .andExpect(content().string(containsString("Reduje el tiempo de deploy un 40%.")))
                .andExpect(content().string(containsString("B2 Conversacional")))
                // el diálogo trae sus datos: la experiencia existente, el molde de una nueva y los logros para el textarea
                .andExpect(content().string(containsString("id=\"dlgCv\"")))
                .andExpect(content().string(containsString("value=\"Nordic Labs\"")))
                .andExpect(content().string(containsString("id=\"tplExp\"")))
                .andExpect(content().string(containsString("data-lineas=\"Reduje el tiempo de deploy un 40%.||Lideré el rediseño.\"")))
                .andExpect(content().string(containsString("Completitud")))
                .andExpect(content().string(matchesPattern("(?s).*/JS/curriculum-editor(-[0-9a-f]{32})?\\.js.*")))
                // el currículum del dueño no muestra acciones de empresa
                .andExpect(content().string(not(containsString("Aceptar"))));
    }

    @Test
    void laRevisionDeEmpresaMuestraLaPostulacionSinEditorYConAccionesConConfirmacion() throws Exception {
        Empleo empleo = new Empleo();
        empleo.setId("emp1");
        empleo.setEmpresaId("e1");
        Solicitud s = new Solicitud();
        s.setId("s1");
        s.setEmpleoId("emp1");
        s.setUsuarioId("t1");
        s.setUsuarioCorreo("valentina@correo.com");
        s.setEstado("revision");
        s.setFechaSolicitud(new Date());
        s.setMensajeMotivacion("Me encantaría formar parte del equipo.");
        Usuario candidato = new Usuario();
        candidato.setId("t1");
        candidato.setNombre("Valentina Ríos");
        candidato.setTipo("talento");
        when(empleoRepository.findById("emp1")).thenReturn(Optional.of(empleo));
        when(solicitudRepository.findByIdAndEmpleoId("s1", "emp1")).thenReturn(Optional.of(s));
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(candidato));

        mockMvc.perform(get("/empleos/emp1/postulaciones/s1").session(sesionEmpresa()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("// revisión de candidatura")))
                .andExpect(content().string(containsString("EN REVISIÓN")))
                .andExpect(content().string(containsString("Me encantaría formar parte del equipo.")))
                .andExpect(content().string(containsString("/empleos/emp1/postulaciones/s1/aprobar")))
                .andExpect(content().string(containsString("/empleos/emp1/postulaciones/s1/rechazar")))
                .andExpect(content().string(containsString("Sí, aceptar")))
                .andExpect(content().string(containsString("Volver al empleo")))
                // la empresa solo lee: sin diálogo de edición ni completitud
                .andExpect(content().string(not(containsString("id=\"dlgCv\""))))
                .andExpect(content().string(not(containsString("Editar secciones"))));
    }
}
