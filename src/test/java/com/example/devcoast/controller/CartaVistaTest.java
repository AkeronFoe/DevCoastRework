package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

class CartaVistaTest {

    private SolicitudVista vista(String id, String estado, String categoria) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setEmpleoTitulo("Cargo " + id);
        s.setEmpleoEmpresaNombre("Empresa " + id);
        s.setEmpleoCategoria(categoria);
        s.setEmpleoStack("Java, Docker");
        s.setEstado(estado);
        s.setFechaSolicitud(new Date());
        Usuario u = new Usuario();
        u.setStack(List.of("Java"));
        return SolicitudVista.de(s, u, new Date());
    }

    @Test
    void agrupaPorEtapaYNoDibujaLasQueNoAvanzaron() {
        CartaVista c = CartaVista.de(List.of(
                vista("1", "revision", "Programacion · Backend"),
                vista("2", "aprobada", "Diseño · UI/UX"),
                vista("3", "rechazada", "Programacion · Backend"),
                vista("4", "descartado", "Programacion · Backend"),
                vista("5", "revision", "Marketing digital")));
        assertEquals(2, c.getZonas().size());
        assertEquals("EN ALTA MAR", c.getZonas().get(0).titulo());
        assertEquals(2, c.getZonas().get(0).total());
        assertEquals("PUERTO", c.getZonas().get(1).titulo());
        assertEquals(1, c.getZonas().get(1).total());
        assertEquals(3, c.getTotalEnRuta());
        assertFalse(c.isVacia());
        assertTrue(c.getZonas().get(1).barcos().get(0).aceptada());
    }

    @Test
    void colorDeLaVelaSegunElAreaYTextoAccesibleCompleto() {
        CartaVista c = CartaVista.de(List.of(
                vista("1", "revision", "Programacion · Backend"),
                vista("2", "revision", "Diseño · UI/UX"),
                vista("3", "revision", "Marketing digital")));
        var b = c.getZonas().get(0).barcos();
        assertEquals("#FF7B4F", b.get(0).s1());
        assertEquals("#2DD4BF", b.get(1).s1());
        assertEquals("#CDD1DB", b.get(2).s1());
        assertEquals("Cargo 1, Empresa 1, en revisión, match 50 por ciento, área Programación", b.get(0).aria());
        assertEquals("En revisión: 3", c.getZonas().get(0).aria());
    }

    @Test
    void maximoCuatroBarcosPorZonaYElRestoSeResume() {
        List<SolicitudVista> todas = new ArrayList<>();
        for (int i = 0; i < 7; i++)
            todas.add(vista("r" + i, "revision", "Programacion · Backend"));
        CartaVista c = CartaVista.de(todas);
        assertEquals(4, c.getZonas().get(0).barcos().size());
        assertEquals(7, c.getZonas().get(0).total());
        assertEquals(3, c.getZonas().get(0).extra());
    }

    @Test
    void sinSolicitudesLaCartaEstaVacia() {
        CartaVista c = CartaVista.de(List.of(vista("3", "rechazada", "Otro")));
        assertTrue(c.isVacia());
        assertEquals(0, c.getZonas().get(0).barcos().size());
    }
}
