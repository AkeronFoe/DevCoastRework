package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.devcoast.service.AreaEmpleo.Area;

class EtiquetasPerfilTest {

    @Test
    void modalidadUnificaLasVariantesGuardadas() {
        assertEquals("remoto", EtiquetasPerfil.modalidadClave("Remoto"));
        assertEquals("hibrido", EtiquetasPerfil.modalidadClave("Híbrido"));
        assertEquals("hibrido", EtiquetasPerfil.modalidadClave("hibrido"));
        assertEquals("", EtiquetasPerfil.modalidadClave("freelance"));
        assertEquals("Híbrido", EtiquetasPerfil.modalidad("hibrido"));
        assertEquals("Freelance", EtiquetasPerfil.modalidad("freelance"));
        assertNull(EtiquetasPerfil.modalidad("  "));
    }

    @Test
    void disponibilidadUnificaLasVariantesGuardadas() {
        assertEquals("full-time", EtiquetasPerfil.disponibilidadClave("Full-time"));
        assertEquals("full-time", EtiquetasPerfil.disponibilidadClave("Tiempo completo"));
        assertEquals("part-time", EtiquetasPerfil.disponibilidadClave("Part time"));
        assertEquals("practicas", EtiquetasPerfil.disponibilidadClave("Prácticas"));
        assertEquals("Tiempo completo", EtiquetasPerfil.disponibilidad("full-time"));
        assertEquals("Medio tiempo", EtiquetasPerfil.disponibilidad("Part-time"));
    }

    @Test
    void losFiltrosDelDirectorioEncuentranTodasLasVariantes() {
        assertTrue("Híbrido".matches("(?i)" + EtiquetasPerfil.modalidadRegex("hibrido")));
        assertTrue("hibrido".matches("(?i)" + EtiquetasPerfil.modalidadRegex("hibrido")));
        assertTrue("Full-time".matches("(?i)" + EtiquetasPerfil.disponibilidadRegex("full-time")));
        assertTrue("tiempo completo".matches("(?i)" + EtiquetasPerfil.disponibilidadRegex("full-time")));
        assertTrue("Part time".matches("(?i)" + EtiquetasPerfil.disponibilidadRegex("part-time")));
        assertNull(EtiquetasPerfil.modalidadRegex("cualquier cosa"));
        assertNull(EtiquetasPerfil.disponibilidadRegex(""));
    }

    @Test
    void especialidadDelRegistroSeMuestraConEtiquetaYUnTextoLibreQuedaIgual() {
        assertEquals("UI / UX", EtiquetasPerfil.especialidad("ui-ux"));
        assertEquals("Diseño gráfico", EtiquetasPerfil.especialidad("diseno-grafico"));
        assertEquals("Product design, UI", EtiquetasPerfil.especialidad("Product design, UI"));
        assertEquals("ui-ux", EtiquetasPerfil.especialidadClave("UI / UX"));
        assertEquals("", EtiquetasPerfil.especialidadClave("Product design, UI"));
    }

    @Test
    void areaDeLaEspecialidad() {
        assertEquals(Area.PROGRAMACION, EtiquetasPerfil.area("backend"));
        assertEquals(Area.PROGRAMACION, EtiquetasPerfil.area("Mobile"));
        assertEquals(Area.DISENO, EtiquetasPerfil.area("diseno-grafico"));
        assertEquals(Area.DISENO, EtiquetasPerfil.area("UI / UX"));
        assertEquals(Area.OTRAS, EtiquetasPerfil.area(null));
        assertEquals(Area.OTRAS, EtiquetasPerfil.area("Marketing"));
    }
}
