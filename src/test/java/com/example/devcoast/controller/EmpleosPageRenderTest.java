package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
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
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Notificacion;
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
class EmpleosPageRenderTest {

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

    private Usuario talento(String... stack) {
        Usuario u = new Usuario();
        u.setId("t1");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setStack(List.of(stack));
        return u;
    }

    private Empleo oferta(int dias, String titulo, String empresa, String categoria, String modalidad, String stack) {
        Empleo e = new Empleo();
        e.setId(new ObjectId(new Date(System.currentTimeMillis() - dias * 86_400_000L - (long) (Math.random() * 1000))).toHexString());
        e.setTitulo(titulo);
        e.setEmpresaNombre(empresa);
        e.setCategoria(categoria);
        e.setModalidad(modalidad);
        e.setSeniority("Senior");
        e.setStack(stack);
        e.setMostrarSalario(true);
        e.setMoneda("COP");
        e.setSalarioPeriodo("Mensual");
        e.setSalarioMin(5_000_000);
        e.setSalarioMax(7_000_000);
        e.setDescripcionRol("Construirás y mantendrás servicios para millones de usuarios.");
        return e;
    }

    private List<Empleo> lista;

    @BeforeEach
    void datos() {
        lista = List.of(
                oferta(1, "Backend Java", "Nordic Labs", "Programacion · Backend", "Remoto", "Java, Spring Boot, Docker"),
                oferta(3, "Frontend React", "Estudio Lumen", "Programacion · Frontend", "Híbrido", "React, TypeScript, Figma, Jest"),
                oferta(8, "Diseño de producto", "Capsula", "Diseño · UI/UX", "Presencial", "Figma, Illustrator"),
                oferta(20, "Marketing digital", "Faro", "Marketing digital", "Remoto", ""));
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(lista);
        when(mongoTemplate.count(any(Query.class), eq(Empleo.class))).thenReturn(4L);
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento("Java", "Spring Boot", "Docker", "Figma")));
        when(solicitudRepository.findByUsuarioIdAndEmpleoIdIn(any(), anyList())).thenReturn(List.of());
        when(solicitudRepository.countByEmpleoId(any())).thenReturn(7L);
    }

    private String volcar(MvcResult r, String nombre) throws Exception {
        String html = r.getResponse().getContentAsString();
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), html);
        return html;
    }

    @Test
    void sinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/empleos").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void talentoConStackVeElMatchElDestacadoYElShell() throws Exception {
        Notificacion n = new Notificacion();
        n.setId("n1");
        n.setTitulo("Tu postulación fue aceptada");
        n.setMensaje("Nordic Labs aceptó tu postulación");
        when(notificacionRepository.countByDestinatarioIdAndLeidaFalse("t1")).thenReturn(1L);
        when(notificacionRepository.findTop5ByDestinatarioIdOrderByFechaCreacionDesc("t1")).thenReturn(List.of(n));
        when(invitacionRepository.countByTalentoIdAndEstado("t1", "pendiente")).thenReturn(2L);

        MvcResult r = mockMvc.perform(get("/empleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DESTACADO · MEJOR MATCH")))
                .andExpect(content().string(containsString("jt in feat")))
                .andExpect(content().string(containsString("MATCH ALTO")))
                .andExpect(content().string(containsString("TU PERFIL VS. LA OFERTA")))
                .andExpect(content().string(containsString("(lo tienes)")))
                .andExpect(content().string(containsString("(te falta)")))
                .andExpect(content().string(containsString("POSTULANTES")))
                .andExpect(content().string(containsString("Postularme a Backend Java en Nordic Labs")))
                .andExpect(content().string(containsString("$5–7 M")))
                .andExpect(content().string(containsString("SALARIO · COP/MES")))
                .andExpect(content().string(containsString("Programación · Frontend")))
                .andExpect(content().string(containsString("Mostrando 4 de 4 ofertas · 1 con match alto")))
                .andExpect(content().string(containsString("cuánto de lo que pide la oferta ya está en tu perfil")))
                .andExpect(content().string(containsString("Alto (85%+)")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("/empleos/" + lista.get(0).getId() + "/aplicar")))
                .andExpect(content().string(containsString("data-postular=\"/empleos/" + lista.get(0).getId() + "/postular\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("4 ofertas activas")))
                .andExpect(content().string(containsString("2 invitaciones pendientes")))
                .andExpect(content().string(containsString("Tu postulación fue aceptada")))
                .andExpect(content().string(not(containsString("Mis ofertas"))))
                .andReturn();
        volcar(r, "empleos-talento.html");
    }

    @Test
    void filtroDeAreaDejaSoloEsaAreaYElDestacadoEsElMejorDeLoQueSeVe() throws Exception {
        mockMvc.perform(get("/empleos").param("area", "diseno").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mostrando 1 de 4 ofertas")))
                .andExpect(content().string(containsString("Diseño de producto")))
                .andExpect(content().string(not(containsString("Backend Java"))))
                .andExpect(content().string(containsString("jt in feat")))
                .andExpect(content().string(containsString("limpiar filtros")));
    }

    @Test
    void matchAltoSoloMuestraLasOfertasConMatchDeAlto() throws Exception {
        mockMvc.perform(get("/empleos").param("hi", "1").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mostrando 1 de 4 ofertas · 1 con match alto")))
                .andExpect(content().string(containsString("Backend Java")))
                .andExpect(content().string(not(containsString("Frontend React"))));
    }

    @Test
    void ordenPorSalarioNoRompeYConservaElOrdenElegidoEnElSelector() throws Exception {
        mockMvc.perform(get("/empleos").param("orden", "salario").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"salario\" selected=\"selected\">")));
    }

    @Test
    void ofertaYaSolicitadaSeMarcaConSelloYBotonDeshabilitado() throws Exception {
        Solicitud s = new Solicitud();
        s.setEmpleoId(lista.get(1).getId());
        when(solicitudRepository.findByUsuarioIdAndEmpleoIdIn(any(), anyList())).thenReturn(List.of(s));

        MvcResult r = mockMvc.perform(get("/empleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jt in sent")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(containsString(">Solicitada<")))
                .andReturn();
        volcar(r, "empleos-solicitada.html");
    }

    @Test
    void talentoSinStackNoVeMatchPeroPuedePostularse() throws Exception {
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(talento()));

        MvcResult r = mockMvc.perform(get("/empleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agrega tu stack en")))
                .andExpect(content().string(containsString("Agrega tu stack para ver tu match.")))
                .andExpect(content().string(not(containsString("MATCH ALTO"))))
                .andExpect(content().string(not(containsString("TU PERFIL VS. LA OFERTA"))))
                .andExpect(content().string(containsString("Postularme a Backend Java")))
                .andReturn();
        volcar(r, "empleos-sin-stack.html");
    }

    @Test
    void cuentaDeEmpresaVeLasOfertasSinPostularseNiMatch() throws Exception {
        MvcResult r = mockMvc.perform(get("/empleos").session(sesion("empresa")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver oferta")))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("Mis ofertas")))
                .andExpect(content().string(not(containsString("Postularme"))))
                .andExpect(content().string(not(containsString("MATCH ALTO"))))
                .andExpect(content().string(not(containsString(">MATCH<</span>MATCH"))))
                .andReturn();
        volcar(r, "empleos-empresa.html");
    }

    @Test
    void sinOfertasMuestraMarEnCalma() throws Exception {
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(Empleo.class))).thenReturn(0L);

        MvcResult r = mockMvc.perform(get("/empleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mar en calma.")))
                .andExpect(content().string(containsString("Todavía no hay ofertas activas.")))
                .andReturn();
        volcar(r, "empleos-vacio.html");
    }

    @Test
    void filtrosSinResultadosOfrecenLimpiar() throws Exception {
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(List.of());

        mockMvc.perform(get("/empleos").param("q", "cobol").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay ofertas con esos filtros.")))
                .andExpect(content().string(containsString("Limpiar filtros")));
    }

    @Test
    void masDeUnaPaginaPaginaLasOtrasOfertasYElDestacadoSoloSaleEnLaPrimera() throws Exception {
        List<Empleo> muchas = new ArrayList<>();
        for (int i = 0; i < 25; i++)
            muchas.add(oferta(i + 1, "Oferta " + i, "Empresa " + i, "Programacion · Backend", "Remoto", "Java, Kafka"));
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(muchas);
        when(mongoTemplate.count(any(Query.class), eq(Empleo.class))).thenReturn(25L);

        mockMvc.perform(get("/empleos").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página 1 de 3")))
                .andExpect(content().string(containsString("Otras ofertas")))
                .andExpect(content().string(containsString("Siguiente")));
        mockMvc.perform(get("/empleos").param("page", "2").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página 3 de 3")))
                .andExpect(content().string(not(containsString("DESTACADO · MEJOR MATCH"))))
                .andExpect(content().string(containsString("Anterior")));
    }

    @Test
    void errorDeOtroFlujoLlegaComoAviso() throws Exception {
        mockMvc.perform(get("/empleos").flashAttr("flashMessage", "El empleo ya no está disponible")
                .flashAttr("flashType", "error").session(sesion("talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El empleo ya no está disponible")));
    }
}
