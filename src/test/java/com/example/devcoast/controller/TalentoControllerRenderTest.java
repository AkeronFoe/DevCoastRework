package com.example.devcoast.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.UsuarioRepository;

// smoke test temporal de la migracion al layout ws-: si una expresion de
// Thymeleaf (o un getter mal escrito) rompe talentos.html, esto lo agarra
// en el build en vez de en produccion
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
        u.setEspecialidad("Frontend");
        u.setBio("Construyo interfaces que no necesitan manual de uso.");
        u.setStack(List.of("React", "TypeScript", "Tailwind"));
        u.setPerfilPublico(true);
        u.setDestacar(true);
        return u;
    }

    @Test
    void listadoDeTalentosRendizaComoEmpresaSinReventar() throws Exception {
        Usuario destacado = talentoConDatos();
        Usuario sinDatos = new Usuario();
        sinDatos.setId("64fabc12ab34cd56ef780002");
        sinDatos.setNombre("Nuevo Perfil");
        sinDatos.setTipo("talento");
        sinDatos.setPerfilPublico(true);

        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(2L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(List.of(destacado, sinDatos));
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any()))
                .thenReturn(List.of());
        when(invitacionRepository.findByEmpresaIdAndTalentoIdInAndEmpleoIdIsNull(anyString(), any()))
                .thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "e1");
        session.setAttribute("usuarioActualTipo", "empresa");
        session.setAttribute("usuarioActualNombre", "Nordic Labs");
        session.setAttribute("usuarioActualCorreo", "contacto@nordiclabs.io");

        mockMvc.perform(get("/talentos").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void listadoDeTalentosRendizaComoTalentoSinReventar() throws Exception {
        Usuario yo = talentoConDatos();
        when(usuarioRepository.findById("t1")).thenReturn(java.util.Optional.of(yo));
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(List.of(yo));
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any()))
                .thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t1");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");

        mockMvc.perform(get("/talentos").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void listadoDeTalentosVacioMuestraEmptyStateSinReventar() throws Exception {
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t1");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");

        mockMvc.perform(get("/talentos").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    // la paginación de /talentos pide el fragmento por fetch (ver talentos.js)
    // — este test pega justo lo que el navegador hace: header
    // X-Requested-With, y verifica que Thymeleaf resuelva el fragmento
    // "talentos :: resultadosTalentos" en vez de la página completa
    @Test
    void paginaSiguienteConHeaderAjaxDevuelveSoloElFragmento() throws Exception {
        Usuario yo = talentoConDatos();
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(20L);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(Usuario.class)))
                .thenReturn(List.of(yo));
        when(proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(any()))
                .thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", "t1");
        session.setAttribute("usuarioActualTipo", "talento");
        session.setAttribute("usuarioActualNombre", "Valentina Ríos");
        session.setAttribute("usuarioActualCorreo", "valentina@correo.com");

        mockMvc.perform(get("/talentos")
                        .param("page", "1")
                        .session(session)
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"talentos-resultados\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"wsBoot\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<html"))));
    }
}
