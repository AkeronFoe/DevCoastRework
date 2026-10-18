package com.example.devcoast.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.devcoast.config.LimiteTasa;
import com.example.devcoast.config.SecurityConfig;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

@WebMvcTest(MensajeController.class)
@Import(SecurityConfig.class)
class MensajesRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudRepository solicitudRepository;
    @MockitoBean
    private EmpleoRepository empleoRepository;
    @MockitoBean
    private MensajeRepository mensajeRepository;
    @MockitoBean
    private NotificacionRepository notificacionRepository;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EmpresaRepository empresaRepository;
    @MockitoBean
    private InvitacionRepository invitacionRepository;
    @MockitoBean
    private MongoTemplate mongoTemplate;
    @MockitoBean
    private LimiteTasa limiteTasa;

    private MockHttpSession sesion(String id, String tipo) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("usuarioActualId", id);
        s.setAttribute("usuarioActualTipo", tipo);
        s.setAttribute("usuarioActualNombre", "Valentina Ríos");
        s.setAttribute("usuarioActualCorreo", "valentina@correo.com");
        return s;
    }

    private Solicitud solicitud() {
        Solicitud s = new Solicitud();
        s.setId("s1");
        s.setUsuarioId("t1");
        s.setUsuarioNombre("Valentina Ríos");
        s.setEmpleoId("e1");
        s.setEmpleoTitulo("Frontend Developer");
        s.setEmpleoEmpresaNombre("Nordic Labs");
        s.setEstado("aprobada");
        return s;
    }

    private Empleo empleo() {
        Empleo e = new Empleo();
        e.setId("e1");
        e.setEmpresaId("c1");
        e.setTitulo("Frontend Developer");
        return e;
    }

    private Mensaje mensaje(String id, String de, String para, String texto) {
        Mensaje m = new Mensaje();
        m.setId(id);
        m.setSolicitudId("s1");
        m.setRemitenteId(de);
        m.setRemitenteNombre("Alguien");
        m.setDestinatarioId(para);
        m.setTexto(texto);
        m.setFechaEnvio(new Date());
        return m;
    }

    private void bandejaCon(Document ultimo, Document noLeidos) {
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("mensajes"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(ultimo == null ? List.of() : List.of(ultimo), new Document()))
                .thenReturn(new AggregationResults<>(noLeidos == null ? List.of() : List.of(noLeidos), new Document()));
    }

    @Test
    void bandejaSinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/mensajes")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void bandejaVaciaMuestraElEstadoVacio() throws Exception {
        bandejaCon(null, null);
        mockMvc.perform(get("/mensajes").session(sesion("t1", "talento")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aún no hay mensajes.")))
                .andExpect(content().string(containsString("Elige una conversación.")));
    }

    @Test
    void bandejaListaLasConversacionesConSinLeer() throws Exception {
        bandejaCon(new Document("_id", "s1").append("texto", "¿Cuándo hablamos?").append("fecha", new Date())
                .append("remitente", "c1"), new Document("_id", "s1").append("n", 2));
        when(solicitudRepository.findAllById(any())).thenReturn(List.of(solicitud()));

        mockMvc.perform(get("/mensajes").session(sesion("t1", "talento")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nordic Labs")))
                .andExpect(content().string(containsString("¿Cuándo hablamos?")))
                .andExpect(content().string(containsString("2 sin leer")))
                .andExpect(content().string(containsString("/chat/s1")));
    }

    @Test
    void bandejaFiltraPorBusqueda() throws Exception {
        bandejaCon(new Document("_id", "s1").append("texto", "hola").append("fecha", new Date()).append("remitente", "c1"), null);
        when(solicitudRepository.findAllById(any())).thenReturn(List.of(solicitud()));

        mockMvc.perform(get("/mensajes").param("q", "nordic").session(sesion("t1", "talento")))
                .andExpect(content().string(containsString("Nordic Labs")));
        bandejaCon(new Document("_id", "s1").append("texto", "hola").append("fecha", new Date()).append("remitente", "c1"), null);
        mockMvc.perform(get("/mensajes").param("q", "zzz").session(sesion("t1", "talento")))
                .andExpect(content().string(containsString("Sin resultados.")))
                .andExpect(content().string(not(containsString("/chat/s1"))));
    }

    @Test
    void chatMuestraMensajesEscapadosYMarcaLeidos() throws Exception {
        bandejaCon(null, null);
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));
        when(mensajeRepository.findBySolicitudIdOrderByFechaEnvioDesc(eq("s1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mensaje("m2", "t1", "c1", "<script>alert(1)</script>"),
                        mensaje("m1", "c1", "t1", "Hola Valentina"))));

        mockMvc.perform(get("/chat/s1").session(sesion("t1", "talento")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hola Valentina")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)"))))
                .andExpect(content().string(containsString("Aceptada")))
                .andExpect(content().string(containsString("Ver postulación")));
        verify(mongoTemplate).updateMulti(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.UpdateDefinition.class), eq(Mensaje.class));
    }

    @Test
    void chatVacioOfreceSugerencias() throws Exception {
        bandejaCon(null, null);
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));
        when(mensajeRepository.findBySolicitudIdOrderByFechaEnvioDesc(eq("s1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/chat/s1").session(sesion("t1", "talento")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rompe el hielo.")))
                .andExpect(content().string(containsString("data-sug=")));
    }

    @Test
    void chatDeOtroUsuarioRedirigeAlDashboard() throws Exception {
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));

        mockMvc.perform(get("/chat/s1").session(sesion("intruso", "talento")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void apiNuevosYMasMensajesPidenSerParticipante() throws Exception {
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));

        mockMvc.perform(get("/chat/s1/nuevos").param("despues", "0").session(sesion("intruso", "talento")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/chat/s1/mas-mensajes").param("antes", "0").session(sesion("intruso", "talento")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/chat/s1/nuevos").param("despues", "0")).andExpect(status().isUnauthorized());
    }

    @Test
    void nuevosDevuelveLosPosterioresYLosPropiosSinLeer() throws Exception {
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));
        when(mensajeRepository.findBySolicitudIdAndFechaEnvioAfterOrderByFechaEnvioAsc(eq("s1"), any(Date.class)))
                .thenReturn(List.of(mensaje("m9", "c1", "t1", "Nuevo")));
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(Mensaje.class)))
                .thenReturn(List.of(mensaje("m5", "t1", "c1", "mío sin leer")));

        mockMvc.perform(get("/chat/s1/nuevos").param("despues", "1000").session(sesion("t1", "talento")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes[0].texto").value("Nuevo"))
                .andExpect(jsonPath("$.mensajes[0].t").isNumber())
                .andExpect(jsonPath("$.sinLeer[0]").value("m5"));
    }

    @Test
    void enviarGuardaRecortaYAvisaAlOtro() throws Exception {
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));
        when(limiteTasa.excedido(anyString(), anyInt(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);

        mockMvc.perform(post("/chat/s1/enviar").param("texto", "  " + "x".repeat(1500) + "  ")
                .session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        ArgumentCaptor<Mensaje> guardado = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(guardado.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1000, guardado.getValue().getTexto().length());
        org.junit.jupiter.api.Assertions.assertEquals("c1", guardado.getValue().getDestinatarioId());
        verify(notificacionRepository).save(any());
    }

    @Test
    void enviarVacioOPasadoDeLimiteNoGuarda() throws Exception {
        when(solicitudRepository.findById("s1")).thenReturn(Optional.of(solicitud()));
        when(empleoRepository.findById("e1")).thenReturn(Optional.of(empleo()));
        when(limiteTasa.excedido(anyString(), anyInt(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);

        mockMvc.perform(post("/chat/s1/enviar").param("texto", "   ").session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().isBadRequest());

        when(limiteTasa.excedido(anyString(), anyInt(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        mockMvc.perform(post("/chat/s1/enviar").param("texto", "hola").session(sesion("t1", "talento")).with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("limite"));
        org.mockito.Mockito.verify(mensajeRepository, org.mockito.Mockito.never()).save(any());
    }
}
