package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.UsuarioRepository;

@WebMvcTest(TalentoController.class)
@Import(SecurityConfig.class)
class TalentoControllerRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private ProyectoRepository proyectoRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;

    private Usuario talentoConDatos() {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780001");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setRolTitulo("Frontend Developer");
        u.setUbicacion("Santiago, Chile");
        u.setModalidad("Remoto");
        u.setDisponibilidad("Full-time");
        u.setEspecialidad("frontend");
        u.setPortfolioUrl("https://valentina.dev");
        u.setGithubUrl("javascript:alert(1)");
        u.setBio("Construyo interfaces que no necesitan manual de uso.");
        u.setStack(List.of("React", "TypeScript", "Tailwind"));
        u.setPerfilPublico(true);
        u.setDestacar(true);
        return u;
    }

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

    @Test
    void empresaVeTarjetasConInvitarYDatosDelPerfil() throws Exception {
        Usuario destacado = talentoConDatos();
        Usuario sinDatos = new Usuario();
        sinDatos.setId("64fabc12ab34cd56ef780002");
        sinDatos.setNombre("Nuevo Perfil");
        sinDatos.setTipo("talento");
        sinDatos.setPerfilPublico(true);
        Usuario yaInvitado = talentoConDatos();
        yaInvitado.setId("64fabc12ab34cd56ef780003");
        yaInvitado.setNombre("Camila Soto");
        yaInvitado.setDestacar(false);
        Usuario noAcepta = talentoConDatos();
        noAcepta.setId("64fabc12ab34cd56ef780004");
        noAcepta.setNombre("Mateo Vega");
        noAcepta.setDestacar(false);
        noAcepta.setPermitirMensajes(false);

        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(4L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(List.of(destacado, sinDatos, yaInvitado, noAcepta));
        Proyecto p = new Proyecto();
        p.setUsuarioId(destacado.getId());
        p.setColorIcono("#2DD4BF");
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any())).thenReturn(List.of(p));
        Invitacion inv = new Invitacion();
        inv.setTalentoId(yaInvitado.getId());
        when(invitacionRepository.findByEmpresaIdAndTalentoIdInAndEmpleoIdIsNull(anyString(), any())).thenReturn(List.of(inv));

        MvcResult r = mockMvc.perform(get("/talentos").session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("// directorio")))
                .andExpect(content().string(containsString("4 talentos con perfil público")))
                .andExpect(content().string(containsString("Valentina Ríos")))
                .andExpect(content().string(containsString("DESTACADO")))
                .andExpect(content().string(containsString("<span>Frontend</span>")))
                .andExpect(content().string(containsString("Tiempo completo")))
                .andExpect(content().string(containsString("1 proyecto en su portafolio")))
                .andExpect(content().string(containsString("href=\"https://valentina.dev\"")))
                .andExpect(content().string(not(containsString("javascript:alert"))))
                .andExpect(content().string(containsString("action=\"/talentos/64fabc12ab34cd56ef780001/invitar\"")))
                .andExpect(content().string(containsString("✓ Invitado")))
                .andExpect(content().string(containsString("No acepta invitaciones")))
                .andExpect(content().string(not(containsString("action=\"/talentos/64fabc12ab34cd56ef780003/invitar\""))))
                .andExpect(content().string(not(containsString("action=\"/talentos/64fabc12ab34cd56ef780004/invitar\""))))
                .andReturn();
        guardar("talentos-empresa.html", r);
    }

    @Test
    void talentoNoVeElBotonInvitar() throws Exception {
        Usuario yo = talentoConDatos();
        when(usuarioRepository.findById("t1")).thenReturn(java.util.Optional.of(yo));
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(List.of(yo));
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any())).thenReturn(List.of());

        MvcResult r = mockMvc.perform(get("/talentos").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("class=\"empresa\""))))
                .andExpect(content().string(containsString("1 talento con perfil público")))
                .andExpect(content().string(not(containsString("/invitar"))))
                .andReturn();
        guardar("talentos-talento.html", r);
    }

    @Test
    void vacioMuestraMarEnCalmaYConFiltrosOfreceLimpiar() throws Exception {
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(List.of());

        mockMvc.perform(get("/talentos").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mar en calma.")))
                .andExpect(content().string(containsString("Todavía no hay talentos con perfil público")))
                .andExpect(content().string(not(containsString("limpiar filtros"))));
        MvcResult r = mockMvc.perform(get("/talentos").param("modalidad", "hibrido").param("q", "java")
                        .session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(content().string(containsString("0 talentos con estos filtros")))
                .andExpect(content().string(containsString("No hay talentos con esos filtros")))
                .andExpect(content().string(containsString("limpiar filtros")))
                .andExpect(content().string(containsString("aria-current=\"true\"")))
                .andReturn();
        guardar("talentos-vacio.html", r);
    }

    @Test
    void paginacionConservaLosFiltros() throws Exception {
        Usuario yo = talentoConDatos();
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(30L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class))).thenReturn(List.of(yo));
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any())).thenReturn(List.of());

        mockMvc.perform(get("/talentos").param("page", "1").param("modalidad", "remoto").param("sort", "oldest")
                        .session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página 2 de 3")))
                .andExpect(content().string(containsString("modalidad=remoto&amp;disponibilidad=&amp;q=&amp;sort=oldest&amp;page=0")))
                .andExpect(content().string(containsString("modalidad=remoto&amp;disponibilidad=&amp;q=&amp;sort=oldest&amp;page=2")))
                .andExpect(content().string(not(containsString("DESTACADO"))));
    }

    @Test
    void sinSesionVaAlLogin() throws Exception {
        mockMvc.perform(get("/talentos")).andExpect(status().is3xxRedirection());
    }

    private Usuario talentoCompleto() {
        Usuario u = talentoConDatos();
        u.setStack(List.of("React", "TypeScript"));
        u.setHabilidadesBlandas(List.of("Comunicación"));
        u.setEmailContacto("valentina@correo.com");
        u.setMostrarCorreo(true);
        u.setTelefono("+56 9 1111 2222");
        u.setLinkedinUrl("https://linkedin.com/in/valentina");
        Usuario.CvExperiencia exp = new Usuario.CvExperiencia();
        exp.setCargo("Frontend Developer");
        exp.setEmpresa("Nordic Labs");
        exp.setPeriodo("2023 – 2025");
        u.setCvExperiencias(List.of(exp));
        return u;
    }

    @Test
    void laEmpresaPuedeInvitarDesdeLaFichaDelTalento() throws Exception {
        Usuario u = talentoCompleto();
        when(usuarioRepository.findById(u.getId())).thenReturn(java.util.Optional.of(u));
        com.example.devcoast.model.Empleo oferta = new com.example.devcoast.model.Empleo();
        oferta.setId("e9");
        oferta.setTitulo("Backend Java");
        oferta.setModalidad("Remoto");
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("e1", "ACTIVO")).thenReturn(List.of(oferta));
        when(invitacionRepository.existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull("e1", u.getId())).thenReturn(false);

        MvcResult r = mockMvc.perform(get("/talentos/" + u.getId()).session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("<h1>Valentina Ríos</h1>")))
                .andExpect(content().string(containsString("Tiempo completo")))
                .andExpect(content().string(containsString("href=\"https://valentina.dev\"")))
                .andExpect(content().string(containsString("Invitar a este talento")))
                .andExpect(content().string(containsString("action=\"/talentos/" + u.getId() + "/invitar\"")))
                .andExpect(content().string(containsString("Backend Java · Remoto")))
                .andExpect(content().string(containsString("Nordic Labs")))
                .andExpect(content().string(containsString("valentina@correo.com")))
                .andExpect(content().string(containsString("+56 9 1111 2222")))
                .andExpect(content().string(not(containsString("javascript:alert"))))
                .andReturn();
        guardar("talento-perfil-empresa.html", r);
    }

    @Test
    void yaInvitadoYSinContactoNoMuestranElFormulario() throws Exception {
        Usuario u = talentoCompleto();
        u.setMostrarCorreo(false);
        when(usuarioRepository.findById(u.getId())).thenReturn(java.util.Optional.of(u));
        when(empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc("e1", "ACTIVO")).thenReturn(List.of());
        when(invitacionRepository.existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull("e1", u.getId())).thenReturn(true);

        mockMvc.perform(get("/talentos/" + u.getId()).session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(content().string(containsString("Ya enviaste una invitación a este talento")))
                .andExpect(content().string(not(containsString("Invitar a este talento"))))
                .andExpect(content().string(not(containsString("valentina@correo.com"))));

        u.setPermitirMensajes(false);
        when(invitacionRepository.existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull("e1", u.getId())).thenReturn(false);
        mockMvc.perform(get("/talentos/" + u.getId()).session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(content().string(containsString("desactivó las invitaciones directas")))
                .andExpect(content().string(not(containsString("Invitar a este talento"))));
    }

    @Test
    void elTalentoVeLaFichaSinInvitar() throws Exception {
        Usuario otro = talentoCompleto();
        otro.setBio(null);
        otro.setCvExperiencias(null);
        otro.setStack(null);
        otro.setHabilidadesBlandas(null);
        otro.setEmailContacto(null);
        otro.setTelefono(null);
        otro.setUbicacion(null);
        otro.setSalarioEsperado(null);
        when(usuarioRepository.findById(otro.getId())).thenReturn(java.util.Optional.of(otro));
        when(usuarioRepository.findById("t1")).thenReturn(java.util.Optional.of(talentoConDatos()));

        MvcResult r = mockMvc.perform(get("/talentos/" + otro.getId()).session(sesion("t1", "talento", "Yo")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("class=\"empresa\""))))
                .andExpect(content().string(containsString("todavía no ha escrito su biografía")))
                .andExpect(content().string(containsString("no comparte datos de contacto")))
                .andExpect(content().string(not(containsString("Invitar a este talento"))))
                .andExpect(content().string(containsString("Volver al Directorio")))
                .andReturn();
        guardar("talento-perfil-talento.html", r);
    }

    @Test
    void unPerfilOcultoRedirigeAlDirectorio() throws Exception {
        Usuario u = talentoCompleto();
        u.setPerfilPublico(false);
        when(usuarioRepository.findById(u.getId())).thenReturn(java.util.Optional.of(u));
        mockMvc.perform(get("/talentos/" + u.getId()).session(sesion("e1", "empresa", "Nordic Labs")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
