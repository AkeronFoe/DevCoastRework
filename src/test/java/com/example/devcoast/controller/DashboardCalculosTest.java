package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

class DashboardCalculosTest {

    private static final long DIA = 86_400_000L;
    private final Date ahora = new Date();

    private Date hace(int dias) {
        return new Date(ahora.getTime() - dias * DIA);
    }

    @Test
    void nombrePilaEsLaPrimeraPalabra() {
        assertEquals("Valentina", DashboardCalculos.nombrePila("  Valentina Ríos "));
        assertEquals("", DashboardCalculos.nombrePila(null));
        assertEquals("", DashboardCalculos.nombrePila("  "));
    }

    @Test
    void pendientesDelPerfilCoincidenConLaCompletitud() {
        Usuario vacio = new Usuario();
        assertEquals(11, DashboardCalculos.pendientesPerfil(vacio).size());

        Usuario u = new Usuario();
        u.setRolTitulo("Backend");
        u.setBio("Hola");
        u.setStack(List.of("Java"));
        u.setUbicacion("Bogotá");
        u.setModalidad("remoto");
        u.setEmailContacto("a@b.co");
        u.setGithubUrl("https://github.com/a");
        var p = DashboardCalculos.pendientesPerfil(u);
        assertEquals(4, p.size());
        assertTrue(p.stream().anyMatch(x -> x.texto().contains("portafolio")));
        assertTrue(p.stream().anyMatch(x -> x.texto().contains("LinkedIn")));
        assertTrue(DashboardCalculos.pendientesPerfil(null).isEmpty());
    }

    @Test
    void promedioRedondeadoONuloSinDatos() {
        assertEquals(73, DashboardCalculos.promedio(List.of(70, 75, 74)));
        assertNull(DashboardCalculos.promedio(List.of()));
        assertNull(DashboardCalculos.promedio(null));
    }

    @Test
    void cuandoUsaHoyAyerYDias() {
        assertEquals("hoy", DashboardCalculos.cuando(ahora, ahora));
        assertEquals("ayer", DashboardCalculos.cuando(hace(1), ahora));
        assertEquals("hace 5 días", DashboardCalculos.cuando(hace(5), ahora));
        assertEquals("", DashboardCalculos.cuando(null, ahora));
    }

    @Test
    void eventosDelTalentoSalenDelHistorialYLasInvitacionesMasRecientesPrimero() {
        Solicitud s = new Solicitud();
        s.setEmpleoTitulo("Backend");
        s.setEmpleoEmpresaNombre("Nordic");
        s.setFechaSolicitud(hace(8));
        s.setHistorial(new ArrayList<>(List.of(new EventoEstado("revision", hace(8), "Enviada"),
                new EventoEstado("aprobada", hace(2), "Aceptada"))));
        Invitacion i = new Invitacion();
        i.setEmpresaNombre("Faro");
        i.setEmpleoTitulo("Mobile");
        i.setFechaEnvio(hace(1));

        var ev = DashboardCalculos.eventosTalento(List.of(s), List.of(i), ahora, 6);
        assertEquals(3, ev.size());
        assertEquals("Faro te invitó a Mobile.", ev.get(0).texto());
        assertEquals("ayer", ev.get(0).cuando());
        assertEquals("Nordic (Backend) aceptó tu solicitud.", ev.get(1).texto());
        assertEquals("t", ev.get(1).tono());
        assertEquals("Enviaste tu solicitud a Nordic (Backend).", ev.get(2).texto());
        assertEquals(1, DashboardCalculos.eventosTalento(List.of(s), List.of(i), ahora, 1).size());
    }

    @Test
    void eventosDeLaEmpresaIncluyenPostulacionesDecisionesEInvitaciones() {
        Solicitud s = new Solicitud();
        s.setUsuarioNombre("Valentina Ríos");
        s.setEmpleoTitulo("Backend");
        s.setFechaSolicitud(hace(6));
        s.setHistorial(new ArrayList<>(List.of(new EventoEstado("revision", hace(6), "Enviada"),
                new EventoEstado("aprobada", hace(3), "Aceptada"))));
        Solicitud sinHistorial = new Solicitud();
        sinHistorial.setUsuarioNombre("Mateo");
        sinHistorial.setEmpleoTitulo("Diseño");
        sinHistorial.setFechaSolicitud(hace(1));
        Invitacion i = new Invitacion();
        i.setTalentoNombre("Camila");
        i.setFechaEnvio(hace(4));

        var ev = DashboardCalculos.eventosEmpresa(List.of(s, sinHistorial), List.of(i), ahora, 6);
        assertEquals(4, ev.size());
        assertEquals("Mateo se postuló a Diseño.", ev.get(0).texto());
        assertEquals("Aceptaste a Valentina Ríos en Backend.", ev.get(1).texto());
        assertEquals("Invitaste a Camila.", ev.get(2).texto());
        assertEquals("Valentina Ríos se postuló a Backend.", ev.get(3).texto());
    }
}
