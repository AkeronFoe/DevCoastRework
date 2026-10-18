package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

class SolicitudVistaTest {

    private static final Date AHORA = new Date();

    private Solicitud solicitud(String estado) {
        Solicitud s = new Solicitud();
        s.setId("s1");
        s.setEmpleoId("e1");
        s.setEmpleoTitulo("Desarrollo Backend");
        s.setEmpleoEmpresaNombre("Nordic Labs");
        s.setEmpleoCategoria("Programacion · Backend");
        s.setEmpleoStack("Java, Docker");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date(AHORA.getTime() - 3 * 86_400_000L));
        s.setHistorial(new ArrayList<>());
        return s;
    }

    private Usuario talento() {
        Usuario u = new Usuario();
        u.setStack(List.of("Java"));
        return u;
    }

    private String estados(SolicitudVista v) {
        StringBuilder sb = new StringBuilder();
        v.getEtapas().forEach(e -> sb.append(e.estado().charAt(0)));
        return sb.toString();
    }

    @Test
    void enRevisionEsLaEtapa2ConRetirar() {
        SolicitudVista v = SolicitudVista.de(solicitud("revision"), talento(), AHORA);
        assertEquals("hap", estados(v));
        assertEquals("s-rev", v.getClaseEstado());
        assertEquals("En revisión", v.getEstadoTexto());
        assertTrue(v.isRetirable());
        assertNull(v.getSello());
        assertTrue(v.getDetalle().startsWith("Enviada el "));
        assertEquals("hace 3 días", v.getHace());
        assertEquals(50, v.getMatch());
    }

    @Test
    void aceptadaTerminaEnDecisionConSelloYFechaDelHistorial() {
        Solicitud s = solicitud("aprobada");
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 14, 10, 0);
        s.getHistorial().add(new EventoEstado("aprobada", c.getTime(), "Aceptada por la empresa"));
        SolicitudVista v = SolicitudVista.de(s, talento(), AHORA);
        assertEquals("hha", estados(v));
        assertEquals("s-ace", v.getClaseEstado());
        assertEquals("ACEPTADA", v.getSello());
        assertEquals("Aceptada el 14 oct", v.getDetalle());
        assertFalse(v.isRetirable());
    }

    @Test
    void rechazadaMarcaLaDecisionEnRojoYDescartadaTerminaEnRevision() {
        SolicitudVista r = SolicitudVista.de(solicitud("rechazada"), talento(), AHORA);
        assertEquals("hhf", estados(r));
        assertEquals("NO AVANZÓ", r.getSello());
        assertEquals("s-rec", r.getClaseEstado());

        SolicitudVista d = SolicitudVista.de(solicitud("descartado"), talento(), AHORA);
        assertEquals("hfp", estados(d));
        assertEquals("Descartada automáticamente", d.getDetalle());
        assertEquals("(aquí terminó)", d.getEtapas().get(1).texto().trim());
    }

    @Test
    void sinStackNoHayMatchYOrdenPorEstado() {
        SolicitudVista v = SolicitudVista.de(solicitud("revision"), new Usuario(), AHORA);
        assertNull(v.getMatch());
        assertEquals(1, v.getOrden());
        assertEquals(2, SolicitudVista.de(solicitud("aprobada"), talento(), AHORA).getOrden());
        assertEquals(3, SolicitudVista.de(solicitud("rechazada"), talento(), AHORA).getOrden());
    }
}
