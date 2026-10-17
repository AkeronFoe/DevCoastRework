package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.repository.CobroRepository;
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

// ronda 2 del rediseño náutico: landing, login y registro renderizan con los mockups nuevos
@WebMvcTest({ PageController.class, AuthController.class, RegistroController.class })
@Import(SecurityConfig.class)
class PublicPagesRenderTest {

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
    private CobroRepository cobroRepository;
    @MockitoBean
    private PlanService planService;

    private void volcar(String nombre, String html) throws Exception {
        Path dir = Path.of("target", "render");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), html);
    }

    private Empleo oferta(String titulo, String empresa, String modalidad, String categoria) {
        Empleo e = new Empleo();
        e.setId("e-" + titulo);
        e.setTitulo(titulo);
        e.setEmpresaNombre(empresa);
        e.setModalidad(modalidad);
        e.setCategoria(categoria);
        return e;
    }

    // ───────── landing ─────────
    @Test
    void landingMuestraLasTresOfertasRealesComoPases() throws Exception {
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(List.of(
                oferta("Desarrollo Backend", "Nordic Labs", "Remoto", "Programacion · Backend"),
                oferta("Diseño de producto", "Estudio Lumen", "Híbrido", "Diseño · UI/UX"),
                oferta("DevOps", "Capsula", "Presencial", null)));

        var res = mockMvc.perform(get("/about-us").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Desarrollo Backend")))
                .andExpect(content().string(containsString("Estudio Lumen")))
                .andExpect(content().string(containsString("ÁREA · DISEÑO · UI/UX")))
                .andExpect(content().string(containsString("id=\"pases\"")))
                .andExpect(content().string(containsString("hullclip")))
                .andExpect(content().string(not(containsString("[Cargo]"))))
                .andExpect(content().string(not(containsString("[Empresa]"))))
                .andExpect(content().string(not(containsString("[año]"))))
                .andReturn();
        volcar("landing.html", res.getResponse().getContentAsString());
    }

    @Test
    void landingSinOfertasOmiteLaEscenaDePases() throws Exception {
        when(mongoTemplate.find(any(Query.class), eq(Empleo.class))).thenReturn(List.of());

        mockMvc.perform(get("/about-us").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"pases\""))))
                .andExpect(content().string(not(containsString("href=\"#pases\""))))
                .andExpect(content().string(containsString("id=\"crear\"")));
    }

    // ───────── login ─────────
    @Test
    void loginTieneLosCamposYElCsrfDelServidor() throws Exception {
        var res = mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"correo\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(not(containsString("volver a probar"))))
                .andExpect(content().string(not(containsString("Recordarme"))))
                .andReturn();
        volcar("login.html", res.getResponse().getContentAsString());
    }

    @Test
    void loginMuestraElErrorDelServidorEnUnAlerta() throws Exception {
        var res = mockMvc.perform(get("/login").flashAttr("flashMessage", "Correo o contraseña incorrectos")
                .flashAttr("flashType", "error").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Correo o contraseña incorrectos")))
                .andExpect(content().string(containsString("data-error=\"true\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andReturn();
        volcar("login-error.html", res.getResponse().getContentAsString());
    }

    @Test
    void loginAvisaCuandoVienesDeRegistrarte() throws Exception {
        mockMvc.perform(get("/login").param("registro", "1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cuenta creada. Ya puedes iniciar sesión.")));
    }

    // ───────── registro ─────────
    @Test
    void registroTraeLosDosFormulariosYLasEspecialidadesReales() throws Exception {
        var res = mockMvc.perform(get("/register-options").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/register/talento\"")))
                .andExpect(content().string(containsString("action=\"/register/empresa\"")))
                .andExpect(content().string(containsString("value=\"diseno-grafico\"")))
                .andExpect(content().string(containsString("value=\"ui-ux\"")))
                .andExpect(content().string(containsString("name=\"rolTitulo\"")))
                .andExpect(content().string(containsString("name=\"stack\"")))
                .andExpect(content().string(containsString("name=\"nombreRepresentante\"")))
                .andExpect(content().string(containsString("PASE · TALENTO")))
                .andExpect(content().string(not(containsString("volver a probar"))))
                .andReturn();
        volcar("register.html", res.getResponse().getContentAsString());
    }

    @Test
    void registroDeEmpresaArrancaConElPaseDeEmpresa() throws Exception {
        var res = mockMvc.perform(get("/register/empresa").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PASE · EMPRESA")))
                .andExpect(content().string(containsString("data-role=\"empresa\"")))
                .andReturn();
        volcar("register-empresa.html", res.getResponse().getContentAsString());
    }

    @Test
    void registroConErrorDelServidorMarcaElCampoYMuestraElMensaje() throws Exception {
        var res = mockMvc.perform(get("/register/talento").param("error", "correo")
                .flashAttr("fNombre", "Valentina").flashAttr("fCorreo", "vale@correo").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ese correo no es válido o ya tiene una cuenta.")))
                .andExpect(content().string(containsString("Revisa los campos marcados")))
                .andExpect(content().string(containsString("value=\"Valentina\"")))
                .andExpect(content().string(containsString("data-error=\"true\"")))
                .andReturn();
        volcar("register-error.html", res.getResponse().getContentAsString());
    }
}
