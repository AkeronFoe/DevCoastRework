package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
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
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;

// Subir / editar proyecto con el shell náutico: el formulario por pasos conserva los ids que usa s-proyecto.js
@WebMvcTest(ProyectoController.class)
@Import(SecurityConfig.class)
class SubirProyectoRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProyectoRepository proyectoRepository;
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
        s.setAttribute("usuarioActualId", "t1");
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    @Test
    void talentoVeElFormularioPorPasosConLaVistaPrevia() throws Exception {
        mockMvc.perform(get("/subirProyecto").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("subirProyecto"))
                .andExpect(content().string(containsString("// nuevo proyecto")))
                .andExpect(content().string(containsString("Subir proyecto")))
                .andExpect(content().string(containsString("/CSS/nautico-subir.css")))
                .andExpect(content().string(containsString("action=\"/proyectos/guardar\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                // los campos ocultos que llena s-proyecto.js al enviar
                .andExpect(content().string(containsString("name=\"nombre\" id=\"h-nombre\"")))
                .andExpect(content().string(containsString("name=\"stack\" id=\"h-stack\"")))
                .andExpect(content().string(containsString("id=\"btn-publicar\"")))
                .andExpect(content().string(containsString("id=\"lp-card\"")))
                .andExpect(content().string(containsString("/JS/proyectos-render.js")))
                .andExpect(content().string(not(containsString("ws-top"))))
                .andExpect(content().string(not(containsString("wsjobs"))));
    }

    @Test
    void editarCargaElProyecto() throws Exception {
        Proyecto p = new Proyecto();
        p.setId("p1");
        p.setUsuarioId("t1");
        p.setNombre("Atlas Pay");
        p.setCategoria("Backend");
        when(proyectoRepository.findByIdAndUsuarioId("p1", "t1")).thenReturn(Optional.of(p));

        mockMvc.perform(get("/subirProyecto?editar=p1").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("// editando proyecto")))
                .andExpect(content().string(containsString("Editar proyecto")))
                .andExpect(content().string(containsString("value=\"Atlas Pay\"")))
                .andExpect(content().string(containsString("name=\"id\"")));
    }

    @Test
    void laEmpresaNoPuedeSubirProyectos() throws Exception {
        mockMvc.perform(get("/subirProyecto").session(sesion("empresa")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
