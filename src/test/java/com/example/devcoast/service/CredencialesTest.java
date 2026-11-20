package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CredencialesTest {

    @Test
    void aceptaContrasenaNormal() {
        assertTrue(Credenciales.cumpleReglas("demo1234"));
        assertNull(Credenciales.problema("demo1234"));
    }

    @Test
    void rechazaCortasYNulas() {
        assertFalse(Credenciales.cumpleReglas("corta"));
        assertFalse(Credenciales.cumpleReglas(null));
        assertNotNull(Credenciales.problema("       "));
    }

    @Test
    void rechazaLasQueNoCabenEnBcrypt() {
        assertTrue(Credenciales.cabeEnBcrypt("a".repeat(72)));
        assertFalse(Credenciales.cabeEnBcrypt("a".repeat(73)));
        assertFalse(Credenciales.cabeEnBcrypt("ñ".repeat(40)));
        assertNotNull(Credenciales.problema("a".repeat(100)));
    }
}
