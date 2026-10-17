package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Usuario;

class TalentoVistaTest {

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId("64fabc12ab34cd56ef780001");
        u.setNombre("  Valentina Ríos ");
        u.setRolTitulo("Frontend Developer");
        u.setEspecialidad("ui-ux");
        u.setModalidad("hibrido");
        u.setDisponibilidad("part-time");
        return u;
    }

    @Test
    void usaEtiquetasInicialYAreaDelPerfil() {
        TalentoVista v = TalentoVista.de(usuario(), 2, List.of("#FF7B4F"), true, false);
        assertEquals("Valentina Ríos", v.getNombre());
        assertEquals("V", v.getInicial());
        assertEquals("UI / UX", v.getCategoria());
        assertEquals("d", v.getAreaClave());
        assertEquals("Híbrido", v.getModalidad());
        assertEquals("Medio tiempo", v.getDisponibilidad());
        assertEquals(2, v.getProyectos());
        assertTrue(v.isDestacado());
        assertTrue(v.isAceptaInvitaciones());
    }

    @Test
    void limitaElStackYCuentaElResto() {
        Usuario u = usuario();
        u.setStack(List.of("Java", "Spring", "Docker", "AWS", "Kafka", "Redis", "Go"));
        TalentoVista v = TalentoVista.de(u, 0, null, false, false);
        assertEquals(5, v.getStack().size());
        assertEquals(2, v.getStackExtra());
    }

    @Test
    void soloMuestraEnlacesHttpYAcortaLaBio() {
        Usuario u = usuario();
        u.setPortfolioUrl("https://valentina.dev");
        u.setLinkedinUrl("javascript:alert(1)");
        u.setGithubUrl("github.com/valentina");
        u.setBio("palabra ".repeat(60));
        TalentoVista v = TalentoVista.de(u, 0, null, false, false);
        assertEquals("https://valentina.dev", v.getPortfolioUrl());
        assertNull(v.getLinkedinUrl());
        assertNull(v.getGithubUrl());
        assertTrue(v.getBio().endsWith("…"));
        assertTrue(v.getBio().length() <= TalentoVista.MAX_BIO + 1);
    }

    @Test
    void sinNombreNiDatosNoReventaYElColorEsEstable() {
        Usuario u = new Usuario();
        u.setId("abc");
        TalentoVista a = TalentoVista.de(u, 0, null, false, false);
        TalentoVista b = TalentoVista.de(u, 0, null, false, false);
        assertEquals("Talento", a.getNombre());
        assertEquals("T", a.getInicial());
        assertEquals(a.getColor(), b.getColor());
        assertNull(a.getCategoria());
        assertEquals("o", a.getAreaClave());
        assertFalse(a.isDestacado());
    }

    @Test
    void permitirMensajesEnFalsoNoAceptaInvitaciones() {
        Usuario u = usuario();
        u.setPermitirMensajes(false);
        assertFalse(TalentoVista.de(u, 0, null, false, false).isAceptaInvitaciones());
    }
}
