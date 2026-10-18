package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.controller.CartaVista.Item;

class CartaVistaTest {

    private static final long DIA = 86_400_000L;
    private final Date ahora = new Date();

    private Date hace(int dias) {
        return new Date(ahora.getTime() - dias * DIA);
    }

    private Item item(String id, String estado, String area, Date envio, Date decision) {
        return new Item(id, "Cargo " + id, "Empresa " + id, 50, area, estado, envio, decision, "/solicitudes/" + id);
    }

    @Test
    void tresZonasSegunEtapaYFechasDelHistorial() {
        CartaVista c = CartaVista.de(List.of(
                item("1", "revision", "p", hace(1), null),                 // zarpando
                item("2", "revision", "d", hace(3), null),                 // zarpando (3 días justos)
                item("3", "revision", "p", hace(4), null),                 // alta mar
                item("4", "aprobada", "p", hace(40), hace(10)),            // puerto
                item("5", "rechazada", "p", hace(2), hace(1)),             // no se dibuja
                item("6", "descartado", "p", hace(2), null)), ahora);      // no se dibuja
        assertEquals(3, c.getZonas().size());
        assertEquals("ZARPANDO", c.getZonas().get(0).titulo());
        assertEquals(2, c.getZonas().get(0).total());
        assertEquals("EN ALTA MAR", c.getZonas().get(1).titulo());
        assertEquals(1, c.getZonas().get(1).total());
        assertEquals("PUERTO", c.getZonas().get(2).titulo());
        assertEquals(1, c.getZonas().get(2).total());
        assertTrue(c.getZonas().get(2).barcos().get(0).aceptada());
        assertEquals(4, c.getTotalEnRuta());
        assertFalse(c.isVacia());
    }

    @Test
    void elPuertoSoloTrae30DiasUsandoLaFechaDeLaDecision() {
        CartaVista c = CartaVista.de(List.of(
                item("1", "aprobada", "p", hace(60), hace(30)),     // límite: entra
                item("2", "aprobada", "p", hace(60), hace(31)),     // fuera
                item("3", "aprobada", "p", hace(5), null),          // sin decisión en el historial: cuenta desde el envío
                item("4", "aprobada", "p", hace(45), null)), ahora);   // sin decisión y vieja: fuera
        assertEquals(2, c.getZonas().get(2).total());
    }

    @Test
    void sinFechaUnaEnRevisionCuentaComoAltaMar() {
        CartaVista c = CartaVista.de(List.of(item("1", "revision", "p", null, null)), ahora);
        assertEquals(0, c.getZonas().get(0).total());
        assertEquals(1, c.getZonas().get(1).total());
    }

    @Test
    void colorDeLaVelaSegunElAreaYTextoAccesibleCompleto() {
        CartaVista c = CartaVista.de(List.of(
                item("1", "revision", "p", hace(5), null),
                item("2", "revision", "d", hace(5), null),
                item("3", "revision", "o", hace(5), null)), ahora);
        var b = c.getZonas().get(1).barcos();
        assertEquals("#FF7B4F", b.get(0).s1());
        assertEquals("#2DD4BF", b.get(1).s1());
        assertEquals("#CDD1DB", b.get(2).s1());
        assertEquals("Cargo 1, Empresa 1, en revisión, match 50 por ciento, área Programación", b.get(0).aria());
        assertEquals("En revisión: 3", c.getZonas().get(1).aria());
        assertEquals("Enviada: 0", c.getZonas().get(0).aria());
    }

    @Test
    void maximoCuatroBarcosPorZonaYElRestoSeResume() {
        List<Item> todas = new ArrayList<>();
        for (int i = 0; i < 7; i++)
            todas.add(item("r" + i, "revision", "p", hace(10), null));
        CartaVista c = CartaVista.de(todas, ahora);
        assertEquals(4, c.getZonas().get(1).barcos().size());
        assertEquals(7, c.getZonas().get(1).total());
        assertEquals(3, c.getZonas().get(1).extra());
    }

    @Test
    void sinSolicitudesActivasLaCartaEstaVacia() {
        CartaVista c = CartaVista.de(List.of(item("3", "rechazada", "p", hace(1), hace(1))), ahora);
        assertTrue(c.isVacia());
        assertEquals(0, c.getTotalEnRuta());
    }
}
