package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.nio.file.Files;
import java.nio.file.Path;
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
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Proyecto;
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

// ronda 6: Mi perfil (talento) y Perfil de empresa con el shell náutico y el formulario en un pase vertical
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

    private MockHttpSession sesion(String id, String tipo, String nombre) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualTipo", tipo);
        session.setAttribute("usuarioActualNombre", nombre);
        session.setAttribute("usuarioActualCorreo", "cuenta@correo.com");
        return session;
    }

    private void guardar(String nombre, MvcResult r) throws Exception {
        Files.createDirectories(Path.of("target", "render"));
        Files.writeString(Path.of("target", "render", nombre), r.getResponse().getContentAsString());
    }

    private void sinActividad() {
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(anyString())).thenReturn(List.of());
        when(invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(anyString())).thenReturn(List.of());
        when(solicitudRepository.countByUsuarioIdAndEstado(anyString(), anyString())).thenReturn(0L);
        when(solicitudRepository.countByUsuarioId(anyString())).thenReturn(0L);
    }

    @Test
    void talentoConDatosVeElFormularioLosSelectoresYLaVistaPrevia() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780001");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setRolTitulo("Frontend Developer");
        u.setBio("Construyo interfaces claras.");
        u.setStack(List.of("React", "TypeScript"));
        u.setHabilidadesBlandas(List.of("Comunicación"));
        u.setUbicacion("Santiago, Chile");
        u.setModalidad("Remoto");               // guardado por la versión anterior de Mi perfil
        u.setDisponibilidad("Full-time");
        u.setEspecialidad("frontend");
        u.setLinkedinUrl("https://linkedin.com/in/valentina");
        u.setGithubUrl("https://github.com/valentina");
        u.setPerfilPublico(true);
        u.setDestacar(true);
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));
        sinActividad();
        Proyecto p = new Proyecto();
        p.setPublicado(true);
        p.setColorIcono("#FF7B4F");
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of(p));

        MvcResult r = mockMvc.perform(get("/perfil").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"))
                .andExpect(content().string(containsString("// mi perfil")))
                .andExpect(content().string(containsString("class=\"pass\"")))
                // los valores viejos ("Remoto", "Full-time") quedan seleccionados en los selectores nuevos
                .andExpect(content().string(containsString("selected=\"selected\">Remoto</option>")))
                .andExpect(content().string(containsString("selected=\"selected\">Tiempo completo</option>")))
                .andExpect(content().string(containsString("selected=\"selected\">Frontend</option>")))
                .andExpect(content().string(containsString("\"React\"")))
                .andExpect(content().string(containsString("Así te ven en el Directorio")))
                .andExpect(content().string(containsString("id=\"pfPreview\"")))
                .andExpect(content().string(containsString("DESTACADO")))
                .andExpect(content().string(containsString("1 proyecto en su portafolio")))
                .andExpect(content().string(containsString("Ver mi perfil público")))
                .andExpect(content().string(containsString("aria-checked=\"true\"")))
                .andExpect(content().string(containsString("Logros")))
                .andReturn();
        guardar("perfil-talento.html", r);
    }

    @Test
    void unTextoLibreDeEspecialidadSeConservaComoOpcion() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780003");
        u.setNombre("Mateo Vega");
        u.setTipo("talento");
        u.setEspecialidad("Product design, UI");
        u.setModalidad("freelance");
        u.setPerfilPublico(false);
        when(usuarioRepository.findById("t3")).thenReturn(Optional.of(u));
        sinActividad();

        mockMvc.perform(get("/perfil").session(sesion("t3", "talento", "Mateo Vega")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"Product design, UI\" selected>Product design, UI</option>")))
                .andExpect(content().string(containsString("<option value=\"freelance\" selected>Freelance</option>")))
                .andExpect(content().string(not(containsString("Ver mi perfil público"))))
                .andExpect(content().string(containsString("Tu perfil está oculto")));
    }

    @Test
    void talentoRecienCreadoSinDatosRendizaConPendientes() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780002");
        u.setNombre("Nuevo Talento");
        u.setTipo("talento");
        when(usuarioRepository.findById("t2")).thenReturn(Optional.of(u));
        sinActividad();

        MvcResult r = mockMvc.perform(get("/perfil").session(sesion("t2", "talento", "Nuevo Talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Para completar tu perfil")))
                .andExpect(content().string(containsString("Escribir tu cargo")))
                .andExpect(content().string(containsString("Aún no agregas experiencia.")))
                .andExpect(content().string(containsString("Escribe tu nombre.")))
                .andReturn();
        guardar("perfil-talento-nuevo.html", r);
    }

    @Test
    void empresaVeSuFormularioConTeal() throws Exception {
        Empresa e = new Empresa();
        e.setId("e1");
        e.setNombreEmpresa("Nordic Labs");
        e.setCorreo("contacto@nordiclabs.io");
        e.setSector("Fintech");
        e.setTamano("11-50");
        e.setTelefono("+57 300 111 2222");
        e.setNombreRepresentante("Ana Mora");
        e.setCargoRepresentante("Talent Lead");
        e.setSitioWeb("https://nordiclabs.io");
        e.setDescripcion("Construimos pagos.");
        e.setPlan("PRO");
        when(empresaRepository.findById("e1")).thenReturn(Optional.of(e));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(anyString(), anyString())).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/perfil").session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil-empresa"))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("Perfil de empresa")))
                .andExpect(content().string(containsString("value=\"Nordic Labs\"")))
                .andExpect(content().string(containsString("<option value=\"11-50\" selected=\"selected\">11 - 50</option>")))
                .andExpect(content().string(containsString("value=\"Ana Mora\"")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(containsString("Plan <b>PRO</b>")))
                .andExpect(content().string(containsString("href=\"/empresa/e1\"")))
                .andReturn();
        guardar("perfil-empresa.html", r);
    }

    @Test
    void sinSesionVaAlLogin() throws Exception {
        mockMvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
    }
}
