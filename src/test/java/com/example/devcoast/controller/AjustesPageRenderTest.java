package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
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

// Ajustes con el shell náutico: secciones como chips, selector de tema, privacidad solo para talento y plan solo para empresa
@WebMvcTest(PageController.class)
@Import(SecurityConfig.class)
class AjustesPageRenderTest {

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

    private MockHttpSession sesion(String id, String tipo, String nombre) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualTipo", tipo);
        session.setAttribute("usuarioActualNombre", nombre);
        session.setAttribute("usuarioActualCorreo", "cuenta@correo.com");
        return session;
    }

    @Test
    void talentoVeCuentaApariencia_PrivacidadYNoVePlan() throws Exception {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780001");
        u.setNombre("Valentina Ríos");
        u.setTipo("talento");
        u.setPerfilPublico(true);
        u.setMostrarCorreo(false);
        when(usuarioRepository.findById("t1")).thenReturn(Optional.of(u));

        mockMvc.perform(get("/ajustes").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("ajustes"))
                .andExpect(content().string(containsString("Tu camarote.")))
                .andExpect(content().string(containsString("Valentina Ríos")))
                .andExpect(content().string(containsString("cuenta@correo.com")))
                // selector de tema con las tres opciones
                .andExpect(content().string(containsString("data-theme-opt=\"dark\"")))
                .andExpect(content().string(containsString("data-theme-opt=\"light\"")))
                .andExpect(content().string(containsString("data-theme-opt=\"midnight\"")))
                .andExpect(content().string(containsString("data-dens=\"compact\"")))
                .andExpect(content().string(containsString("data-text=\"large\"")))
                // privacidad solo para talento, con el estado real de cada interruptor
                .andExpect(content().string(containsString("data-sec=\"privacidad\"")))
                .andExpect(content().string(containsString("data-priv=\"perfilPublico\"")))
                .andExpect(content().string(containsString("data-priv=\"mostrarCorreo\"")))
                .andExpect(content().string(containsString("aria-checked=\"true\"")))
                .andExpect(content().string(containsString("aria-checked=\"false\"")))
                .andExpect(content().string(not(containsString("data-sec=\"plan\""))))
                // diálogos y avisos
                .andExpect(content().string(containsString("id=\"dlgCorreo\"")))
                .andExpect(content().string(containsString("action=\"/cuenta/eliminar\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("id=\"toast\"")))
                .andExpect(content().string(matchesPattern("(?s).*/CSS/nautico-ajustes(-[0-9a-f]{32})?\\.css.*")))
                .andExpect(content().string(matchesPattern("(?s).*/JS/preferencias(-[0-9a-f]{32})?\\.js.*")));
    }

    @Test
    void empresaVePlanYNoVePrivacidad() throws Exception {
        Empresa e = new Empresa();
        e.setId("64fabc12ab34cd56ef7800a1");
        e.setNombreEmpresa("Astillero Digital");
        e.setPlan("PRO");
        e.setPlanVenceEn(new Date(1_800_000_000_000L));
        when(empresaRepository.findById("e1")).thenReturn(Optional.of(e));
        when(empleoRepository.countByEmpresaIdAndEstado("e1", "ACTIVO")).thenReturn(4L);
        when(empleoRepository.countByEmpresaIdAndDestacarTrueAndEstado("e1", "ACTIVO")).thenReturn(1L);
        when(planService.limiteVacantesActivas(e)).thenReturn(15);
        when(planService.limiteDestacados(e)).thenReturn(3);

        mockMvc.perform(get("/ajustes").session(sesion("e1", "empresa", "Astillero Digital")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("ajustes"))
                .andExpect(content().string(containsString("class=\"empresa\"")))
                .andExpect(content().string(containsString("data-sec=\"plan\"")))
                .andExpect(content().string(containsString("Pro · vence el")))
                .andExpect(content().string(containsString("4 de 15")))
                .andExpect(content().string(containsString("1 de 3")))
                .andExpect(content().string(containsString("href=\"/facturacion\"")))
                // el servidor rechaza estas opciones a las empresas: no se ofrecen
                .andExpect(content().string(not(containsString("data-sec=\"privacidad\""))))
                .andExpect(content().string(not(containsString("data-priv="))))
                .andExpect(content().string(not(containsString("id=\"btnPro\""))));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/ajustes"))
                .andExpect(status().is3xxRedirection());
    }
    // ===== Facturación (cuenta de empresa): los cobros como recibos =====

    @Test
    void facturacionMuestraLosCobrosComoRecibosConElTotal() throws Exception {
        Cobro c1 = new Cobro();
        c1.setEmpleoTitulo("Backend Java");
        c1.setMonto(5.0);
        c1.setMotivo("Tarifa de colocación (plan Gratis)");
        c1.setFecha(new Date(1_800_000_000_000L));
        Cobro c2 = new Cobro();
        c2.setEmpleoTitulo("DevOps");
        c2.setMonto(5.0);
        c2.setMotivo("Tarifa de colocación (plan Gratis)");
        c2.setFecha(new Date(1_800_100_000_000L));
        when(cobroRepository.findByEmpresaIdOrderByFechaDesc("e1")).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/facturacion").session(sesion("e1", "empresa", "Astillero Digital")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("facturacion"))
                .andExpect(content().string(containsString("// historial de cobros")))
                .andExpect(content().string(containsString("$10.00")))
                .andExpect(content().string(containsString("Backend Java")))
                .andExpect(content().string(containsString("DevOps")))
                .andExpect(content().string(containsString("MONTO")))
                .andExpect(content().string(not(containsString("Sin cobros."))));
    }

    @Test
    void facturacionSinCobrosMuestraElEstadoVacio() throws Exception {
        when(cobroRepository.findByEmpresaIdOrderByFechaDesc("e1")).thenReturn(List.of());

        mockMvc.perform(get("/facturacion").session(sesion("e1", "empresa", "Astillero Digital")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin cobros.")))
                .andExpect(content().string(containsString("$0.00")));
    }

    @Test
    void facturacionEsSoloParaEmpresas() throws Exception {
        mockMvc.perform(get("/facturacion").session(sesion("t1", "talento", "Valentina Ríos")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
