package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MvcResult;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;

// ronda 4 del rediseño náutico: /proyectos como portafolio (tarjetas, interruptor, diálogo) y sus endpoints
@WebMvcTest(ProyectoController.class)
@Import(SecurityConfig.class)
class ProyectosPageRenderTest {

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

    private Proyecto proyecto(String id, String nombre, String categoria, boolean publicado) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setUsuarioId("t1");
        p.setNombre(nombre);
        p.setCategoria(categoria);
        p.setTipo("Personal");
        p.setAnio("2026");
        p.setDescripcionCorta("Descripción de " + nombre);
        p.setStack(List.of("React", "Java"));
        p.setPublicado(publicado);
        p.setCreadoEn(new Date());
        return p;
    }

    private List<Proyecto> lista;

    @BeforeEach
    void datos() {
        Proyecto codigo = proyecto("p1", "App de tareas", "Full Stack", true);
        codigo.setLinkProyecto("https://github.com/vale/tareas");
        Proyecto diseno = proyecto("p2", "Identidad para marca local", "Branding", true);
        diseno.setLinkProyecto("javascript:alert(1)");
        Proyecto oculto = proyecto("p3", "Póster de festival", "Ilustraciones 3D", false);
        oculto.setBannerDataUrl("data:image/png;base64,iVBORw0KGgo=");
        lista = List.of(codigo, diseno, oculto);
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(lista);
        when(proyectoRepository.findByIdAndUsuarioId("p1", "t1")).thenReturn(Optional.of(codigo));
        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void volcar(MvcResult r, String nombre) throws Exception {
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), r.getResponse().getContentAsString());
    }

    // ───────── la página ─────────
    @Test
    void portafolioMuestraTarjetasPortadasEnlacesYElInterruptor() throws Exception {
        MvcResult r = mockMvc.perform(get("/proyectos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3 proyectos · 2 públicos")))
                .andExpect(content().string(containsString("App de tareas")))
                // portada de código con datos reales (nombre y herramientas)
                .andExpect(content().string(containsString("cv code")))
                // portada subida por el talento
                .andExpect(content().string(containsString("data:image/png;base64,iVBORw0KGgo=")))
                // enlace seguro con rel/target; el javascript: se descarta
                .andExpect(content().string(containsString("href=\"https://github.com/vale/tareas\"")))
                .andExpect(content().string(containsString("rel=\"noopener noreferrer\"")))
                .andExpect(content().string(not(containsString("javascript:alert"))))
                // ya no hay enlace "Código" (el modelo no tiene repositorio)
                .andExpect(content().string(not(containsString(">Código<"))))
                // interruptor con estado, nombre accesible y texto
                .andExpect(content().string(containsString("role=\"switch\"")))
                .andExpect(content().string(containsString("aria-checked=\"true\"")))
                .andExpect(content().string(containsString("aria-checked=\"false\"")))
                .andExpect(content().string(containsString("Proyecto público: App de tareas")))
                .andExpect(content().string(containsString("OCULTO")))
                .andExpect(content().string(containsString("pj in off")))
                // acciones: editar (editor completo como respaldo) y eliminar con confirmación visible
                .andExpect(content().string(containsString("/subirProyecto?editar=p1")))
                .andExpect(content().string(containsString("¿Eliminar este proyecto?")))
                .andExpect(content().string(containsString("/proyectos/eliminar/p1")))
                // diálogo
                .andExpect(content().string(containsString("<dialog")))
                .andExpect(content().string(containsString("value=\"Motion / Animacion\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                // shell
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("Tus proyectos públicos aparecen en tu perfil")))
                .andReturn();
        volcar(r, "proyectos.html");
    }

    @Test
    void filtrosPorAreaYVisibilidad() throws Exception {
        mockMvc.perform(get("/proyectos").param("area", "diseno").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Identidad para marca local")))
                .andExpect(content().string(containsString("Póster de festival")))
                .andExpect(content().string(not(containsString("App de tareas"))))
                .andExpect(content().string(containsString("limpiar filtros")));
        mockMvc.perform(get("/proyectos").param("vis", "0").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Póster de festival")))
                .andExpect(content().string(not(containsString("Identidad para marca local"))));
        mockMvc.perform(get("/proyectos").param("q", "poster").session(sesion("talento")).with(csrf()))
                .andExpect(content().string(containsString("Póster de festival")))
                .andExpect(content().string(not(containsString("App de tareas"))));
    }

    @Test
    void sinResultadosMuestraMarEnCalmaSinElMosaicoDeAnadir() throws Exception {
        MvcResult r = mockMvc.perform(get("/proyectos").param("q", "cobol").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mar en calma.")))
                .andExpect(content().string(containsString("No hay proyectos con esos filtros.")))
                .andExpect(content().string(not(containsString("id=\"add\""))))
                .andReturn();
        volcar(r, "proyectos-sin-resultados.html");
    }

    @Test
    void sinProyectosSoloQuedaElMosaicoDeAnadir() throws Exception {
        when(proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc("t1")).thenReturn(List.of());
        MvcResult r = mockMvc.perform(get("/proyectos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 proyectos · 0 públicos")))
                .andExpect(content().string(containsString("id=\"add\"")))
                .andReturn();
        volcar(r, "proyectos-vacio.html");
    }

    @Test
    void empresaYVisitanteNoTienenProyectos() throws Exception {
        mockMvc.perform(get("/proyectos").session(sesion("empresa")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/misEmpleos"));
        mockMvc.perform(get("/proyectos").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    // ───────── visibilidad ─────────
    @Test
    void visibilidadResponderJsonYGuarda() throws Exception {
        mockMvc.perform(post("/proyectos/p1/visibilidad").param("publicado", "false").accept(MediaType.APPLICATION_JSON)
                .session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.publicado").value(false));
        ArgumentCaptor<Proyecto> cap = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).save(cap.capture());
        org.junit.jupiter.api.Assertions.assertFalse(cap.getValue().isPublicado());
    }

    @Test
    void visibilidadSinJavascriptRedirige() throws Exception {
        mockMvc.perform(post("/proyectos/p1/visibilidad").param("publicado", "false").session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashMessage", "“App de tareas” quedó oculto."));
    }

    @Test
    void visibilidadSoloDeProyectosPropiosYDeTalentos() throws Exception {
        mockMvc.perform(post("/proyectos/ajeno/visibilidad").param("publicado", "false").accept(MediaType.APPLICATION_JSON)
                .session(sesion("talento")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/proyectos/p1/visibilidad").param("publicado", "false").accept(MediaType.APPLICATION_JSON)
                .session(sesion("empresa")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/proyectos/p1/visibilidad").param("publicado", "false").accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isUnauthorized());
        verify(proyectoRepository, never()).save(any());
    }

    // ───────── crear, editar, eliminar desde el portafolio ─────────
    @Test
    void crearDesdeElDialogoVuelveAlPortafolio() throws Exception {
        mockMvc.perform(post("/proyectos/guardar").param("nombre", "Landing").param("categoria", "Frontend")
                .param("tipo", "Personal").param("descripcion", "Sitio estático").param("stack", "HTML, CSS")
                .param("linkProyecto", "https://ejemplo.com").param("volver", "proyectos")
                .session(sesion("talento")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashMessage", "Proyecto publicado correctamente"));
        ArgumentCaptor<Proyecto> cap = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).save(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("HTML", "CSS"), cap.getValue().getStack());
        org.junit.jupiter.api.Assertions.assertEquals("t1", cap.getValue().getUsuarioId());
    }

    @Test
    void sinVolverTodoSigueComoAntes() throws Exception {
        mockMvc.perform(post("/proyectos/guardar").param("nombre", "Landing").param("categoria", "Frontend")
                .param("tipo", "Personal").param("descripcion", "x").session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/misProyectos"));
    }

    @Test
    void unEnlaceQueNoEsHttpSeRechaza() throws Exception {
        mockMvc.perform(post("/proyectos/guardar").param("nombre", "Landing").param("categoria", "Frontend")
                .param("tipo", "Personal").param("descripcion", "x").param("linkProyecto", "javascript:alert(1)")
                .param("volver", "proyectos").session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashType", "error"));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void editarDesdeElDialogoSoloCambiaLosCamposDelDialogo() throws Exception {
        Proyecto p = lista.get(0);
        p.setRol("Desarrollador principal");
        p.setDuracion("1 – 3 meses");
        p.setDescripcionExtendida("Texto largo");
        p.setBannerClase("cover-x");

        mockMvc.perform(post("/proyectos/p1/actualizar").param("nombre", "App de tareas v2").param("categoria", "Backend")
                .param("tipo", "Cliente").param("descripcion", "Nueva").param("anio", "2025").param("stack", "Java, Spring")
                .param("linkProyecto", "").param("volver", "proyectos").session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashMessage", "Proyecto actualizado correctamente"));

        ArgumentCaptor<Proyecto> cap = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectoRepository).save(cap.capture());
        Proyecto g = cap.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("App de tareas v2", g.getNombre());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("Java", "Spring"), g.getStack());
        org.junit.jupiter.api.Assertions.assertNull(g.getLinkProyecto());
        // lo que el diálogo no toca se conserva
        org.junit.jupiter.api.Assertions.assertEquals("Desarrollador principal", g.getRol());
        org.junit.jupiter.api.Assertions.assertEquals("1 – 3 meses", g.getDuracion());
        org.junit.jupiter.api.Assertions.assertEquals("Texto largo", g.getDescripcionExtendida());
        org.junit.jupiter.api.Assertions.assertEquals("cover-x", g.getBannerClase());
    }

    @Test
    void noSePuedeEditarElProyectoDeOtraPersona() throws Exception {
        mockMvc.perform(post("/proyectos/ajeno/actualizar").param("nombre", "x").param("categoria", "Frontend")
                .param("tipo", "Personal").param("descripcion", "x").param("volver", "proyectos")
                .session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashType", "error"));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void eliminarVuelveAlPortafolio() throws Exception {
        when(proyectoRepository.findById("p1")).thenReturn(Optional.of(lista.get(0)));
        mockMvc.perform(post("/proyectos/eliminar/p1").param("volver", "proyectos").session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/proyectos"))
                .andExpect(flash().attribute("flashMessage", "Proyecto eliminado correctamente"));
        verify(proyectoRepository).deleteById("p1");
        mockMvc.perform(post("/proyectos/eliminar/p1").session(sesion("talento")).with(csrf()))
                .andExpect(redirectedUrl("/misProyectos"));
    }
}
