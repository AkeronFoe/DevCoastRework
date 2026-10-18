package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.PlanService;

// ronda 6: lo que guarda el formulario de Mi perfil y del Perfil de empresa en POST /perfil/guardar
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class PerfilGuardarTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
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

    private MockHttpSession sesion(String id, String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", id);
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Cuenta");
        return s;
    }

    @Test
    void laEmpresaGuardaTambienTamanoTelefonoYRepresentante() throws Exception {
        Empresa e = new Empresa();
        e.setId("e1");
        e.setNombreEmpresa("Nordic Labs");
        when(empresaRepository.findById("e1")).thenReturn(Optional.of(e));

        mockMvc.perform(post("/perfil/guardar").session(sesion("e1", "empresa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nordic Labs SAS\",\"rolTitulo\":\"Fintech\",\"bio\":\"Pagos.\",\"ubicacion\":\"Bogotá\","
                                + "\"portfolioUrl\":\"https://nordiclabs.io\",\"tamano\":\"11-50\",\"telefono\":\"+57 300\","
                                + "\"nombreRepresentante\":\"Ana Mora\",\"cargoRepresentante\":\"Talent Lead\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        ArgumentCaptor<Empresa> guardada = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(guardada.capture());
        Empresa g = guardada.getValue();
        assertEquals("Nordic Labs SAS", g.getNombreEmpresa());
        assertEquals("Fintech", g.getSector());
        assertEquals("Pagos.", g.getDescripcion());
        assertEquals("https://nordiclabs.io", g.getSitioWeb());
        assertEquals("11-50", g.getTamano());
        assertEquals("+57 300", g.getTelefono());
        assertEquals("Ana Mora", g.getNombreRepresentante());
        assertEquals("Talent Lead", g.getCargoRepresentante());
    }

    @Test
    void siElCampoNoLlegaLaEmpresaConservaElValorGuardado() throws Exception {
        Empresa e = new Empresa();
        e.setId("e1");
        e.setNombreEmpresa("Nordic Labs");
        e.setTamano("51-200");
        e.setTelefono("+57 111");
        when(empresaRepository.findById("e1")).thenReturn(Optional.of(e));

        mockMvc.perform(post("/perfil/guardar").session(sesion("e1", "empresa")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"Nueva descripción\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Empresa> guardada = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(guardada.capture());
        assertEquals("51-200", guardada.getValue().getTamano());
        assertEquals("+57 111", guardada.getValue().getTelefono());
        assertEquals("Nueva descripción", guardada.getValue().getDescripcion());
    }

    @Test
    void elTalentoGuardaLasClavesDeLosSelectoresYLasListas() throws Exception {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/perfil/guardar").session(sesion("t1", "talento")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Valentina Ríos\",\"modalidad\":\"hibrido\",\"disponibilidad\":\"full-time\","
                                + "\"especialidad\":\"ui-ux\",\"stack\":[\"Figma\",\"React\"],\"habilidadesBlandas\":[\"Comunicación\"]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        Usuario g = guardado.getValue();
        assertEquals("hibrido", g.getModalidad());
        assertEquals("full-time", g.getDisponibilidad());
        assertEquals("ui-ux", g.getEspecialidad());
        assertEquals(java.util.List.of("Figma", "React"), g.getStack());
        assertEquals(java.util.List.of("Comunicación"), g.getHabilidadesBlandas());
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(post("/perfil/guardar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
