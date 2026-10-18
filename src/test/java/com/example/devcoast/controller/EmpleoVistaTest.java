package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Empleo;

class EmpleoVistaTest {

    private static final Date AHORA = new Date();

    private Empleo oferta() {
        Empleo e = new Empleo();
        e.setId(new ObjectId(new Date(AHORA.getTime() - 5 * 86_400_000L)).toHexString());
        e.setTitulo("Desarrollo Backend");
        e.setEmpresaNombre("Nordic Labs");
        e.setCategoria("Programacion · Backend");
        e.setStack("Java, Spring Boot");
        return e;
    }

    @Test
    void salarioEnMillonesYEnMiles() {
        Empleo e = oferta();
        e.setMostrarSalario(true);
        e.setMoneda("COP");
        e.setSalarioPeriodo("Mensual");
        e.setSalarioMin(5_000_000);
        e.setSalarioMax(7_000_000);
        EmpleoVista v = EmpleoVista.de(e, null, false, false, null, AHORA);
        assertEquals("$5–7 M", v.getSalarioTexto());
        assertEquals("SALARIO · COP/MES", v.getSalarioEtiqueta());

        e.setMoneda("USD");
        e.setSalarioMin(1800);
        e.setSalarioMax(2500);
        v = EmpleoVista.de(e, null, false, false, null, AHORA);
        assertEquals("$1,8–2,5 K", v.getSalarioTexto());
        assertEquals("SALARIO · USD/MES", v.getSalarioEtiqueta());
    }

    @Test
    void salarioOcultoNoSeMuestra() {
        Empleo e = oferta();
        e.setMostrarSalario(false);
        e.setSalarioMin(5_000_000);
        e.setSalarioMax(7_000_000);
        assertNull(EmpleoVista.de(e, null, false, false, null, AHORA).getSalarioTexto());
    }

    @Test
    void fechaSalDelObjectIdYNuevaSoloSiTieneDosDiasOMenos() {
        Empleo e = oferta();
        EmpleoVista v = EmpleoVista.de(e, null, false, false, null, AHORA);
        assertEquals("hace 5 días", v.getHace());
        assertFalse(v.isNueva());

        e.setFechaPublicacion(new Date(AHORA.getTime() - 86_400_000L));
        v = EmpleoVista.de(e, null, false, false, null, AHORA);
        assertEquals("hace 1 día", v.getHace());
        assertTrue(v.isNueva());
    }

    @Test
    void ofertaConIdQueNoEsObjectIdNoMuestraFecha() {
        Empleo e = oferta();
        e.setId("abc");
        EmpleoVista v = EmpleoVista.de(e, null, false, false, null, AHORA);
        assertNull(v.getHace());
        assertFalse(v.isNueva());
    }

    @Test
    void descripcionCortaSoloEnElDestacadoYCortaEnPalabra() {
        Empleo e = oferta();
        e.setDescripcionRol("Construirás servicios ".repeat(20));
        assertNull(EmpleoVista.de(e, null, false, false, null, AHORA).getDescripcionCorta());
        String d = EmpleoVista.de(e, null, false, true, 3L, AHORA).getDescripcionCorta();
        assertTrue(d.endsWith("…"));
        assertTrue(d.length() <= 142);
    }

    @Test
    void sinMatchLasHabilidadesSonNeutras() {
        EmpleoVista v = EmpleoVista.de(oferta(), null, false, false, null, AHORA);
        assertNull(v.getMatch());
        assertEquals("neutra", v.getHabilidades().get(0).estado());
    }
}
