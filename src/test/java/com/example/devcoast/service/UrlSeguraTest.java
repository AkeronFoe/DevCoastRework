package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrlSeguraTest {

    @Test
    void rechazaEsquemasPeligrosos() {
        assertNull(UrlSegura.normalizar("javascript:alert(1)"));
        assertNull(UrlSegura.normalizar("JaVaScRiPt:alert(1)"));
        assertNull(UrlSegura.normalizar("data:text/html,<script>alert(1)</script>"));
        assertNull(UrlSegura.normalizar("vbscript:x"));
        assertNull(UrlSegura.normalizar("//evil.com"));
    }

    @Test
    void rechazaCaracteresQueRompenUnAtributo() {
        assertNull(UrlSegura.normalizar("http://a.com\" onmouseover=\"x"));
        assertNull(UrlSegura.normalizar("https://exa mple.com"));
        assertNull(UrlSegura.normalizar("https://a.com/<b>"));
    }

    @Test
    void agregaHttpsCuandoFalta() {
        assertEquals("https://miweb.com", UrlSegura.normalizar("miweb.com"));
        assertEquals("https://www.linkedin.com/in/yo", UrlSegura.normalizar("  www.linkedin.com/in/yo "));
        assertEquals("https://miweb.com:8080/ruta", UrlSegura.normalizar("miweb.com:8080/ruta"));
    }

    @Test
    void respetaEnlacesValidos() {
        assertEquals("https://a.co/x?y=1", UrlSegura.normalizar("https://a.co/x?y=1"));
        assertEquals("http://a.co", UrlSegura.normalizar("http://a.co"));
    }

    @Test
    void vacioONuloDaNulo() {
        assertNull(UrlSegura.normalizar(null));
        assertNull(UrlSegura.normalizar("   "));
        assertNull(UrlSegura.normalizar("https://"));
    }

    @Test
    void paraMostrarFiltraDatosViejos() {
        assertNull(UrlSegura.paraMostrar("javascript:alert(1)"));
        assertEquals("https://a.co", UrlSegura.paraMostrar(" https://a.co "));
        assertFalse(UrlSegura.esSegura("ftp://a.co"));
        assertTrue(UrlSegura.esSegura("https://a.co"));
    }
}
