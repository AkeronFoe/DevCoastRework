package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Usuario;

class MatchEmpleoTest {

    private Usuario talento(String... stack) {
        Usuario u = new Usuario();
        u.setStack(List.of(stack));
        return u;
    }

    private Empleo oferta(String stack) {
        Empleo e = new Empleo();
        e.setStack(stack);
        return e;
    }

    @Test
    void porcentajeEsLoQueLaOfertaPideYElTalentoYaTiene() {
        var r = MatchEmpleo.calcular(talento("Java", "Spring Boot", "Figma"), oferta("Java, Spring Boot, Kafka, AWS"));
        assertEquals(50, r.porcentaje());
        assertEquals(List.of("Java", "Spring Boot"), r.tiene());
        assertEquals(List.of("Kafka", "AWS"), r.faltan());
    }

    @Test
    void tenerHabilidadesDeMasNoSubeElPorcentaje() {
        var r = MatchEmpleo.calcular(talento("Java", "Go", "Rust", "Python", "C"), oferta("Java, Docker"));
        assertEquals(50, r.porcentaje());
    }

    @Test
    void ignoraMayusculasTildesYPuntuacion() {
        var r = MatchEmpleo.calcular(talento("nodejs", "REACT", "Diseño UX"), oferta("Node.js; React, diseno ux"));
        assertEquals(100, r.porcentaje());
        assertTrue(r.faltan().isEmpty());
    }

    @Test
    void javaNoCoincideConJavascript() {
        var r = MatchEmpleo.calcular(talento("JavaScript"), oferta("Java"));
        assertEquals(0, r.porcentaje());
        assertEquals(List.of("Java"), r.faltan());
    }

    @Test
    void cSharpYCMasMasConservanSuSimbolo() {
        var r = MatchEmpleo.calcular(talento("C#"), oferta("C#, C++, C"));
        assertEquals(33, r.porcentaje());
        assertEquals(List.of("C#"), r.tiene());
    }

    @Test
    void habilidadesRepetidasEnLaOfertaCuentanUnaVez() {
        assertEquals(List.of("Java", "Docker"), MatchEmpleo.habilidadesRequeridas("Java, java ,Docker,, ;"));
    }

    @Test
    void sinPosibilidadDeCalcularDevuelveNullYCero() {
        assertNull(MatchEmpleo.calcular(talento(), oferta("Java")));
        assertNull(MatchEmpleo.calcular(null, oferta("Java")));
        assertNull(MatchEmpleo.calcular(talento("Java"), oferta("  ")));
        assertNull(MatchEmpleo.calcular(talento("Java"), oferta(null)));
        assertEquals(0, MatchEmpleo.porcentaje(talento("Java"), oferta(null)));
    }

    @Test
    void nivelesDelDiseno() {
        assertEquals("alto", MatchEmpleo.nivel(100));
        assertEquals("alto", MatchEmpleo.nivel(85));
        assertEquals("medio", MatchEmpleo.nivel(84));
        assertEquals("medio", MatchEmpleo.nivel(65));
        assertEquals("bajo", MatchEmpleo.nivel(64));
        assertEquals("bajo", MatchEmpleo.nivel(0));
    }
}
