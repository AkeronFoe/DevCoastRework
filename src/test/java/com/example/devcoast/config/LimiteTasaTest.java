package com.example.devcoast.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class LimiteTasaTest {

    private final AtomicLong ahora = new AtomicLong(5_000_000L);
    private final LimiteTasa limite = new LimiteTasa(ahora::get);
    private static final long VENTANA = 60_000L;

    @Test
    void dejaPasarHastaElMaximoYDespuesAvisa() {
        for (int i = 0; i < 3; i++)
            assertFalse(limite.excedido("a", 3, VENTANA));
        assertTrue(limite.excedido("a", 3, VENTANA));
    }

    @Test
    void cadaClaveTieneSuPropioConteo() {
        for (int i = 0; i < 3; i++)
            limite.excedido("a", 3, VENTANA);
        assertFalse(limite.excedido("b", 3, VENTANA));
    }

    @Test
    void conElTiempoVuelveAPermitir() {
        for (int i = 0; i < 3; i++)
            limite.excedido("a", 3, VENTANA);
        assertTrue(limite.excedido("a", 3, VENTANA));
        ahora.addAndGet(VENTANA + 1);
        assertFalse(limite.excedido("a", 3, VENTANA));
    }

    @Test
    void losIntentosRechazadosNoAlargaElBloqueo() {
        for (int i = 0; i < 3; i++)
            limite.excedido("a", 3, VENTANA);
        for (int i = 0; i < 50; i++)
            limite.excedido("a", 3, VENTANA);
        ahora.addAndGet(VENTANA + 1);
        assertFalse(limite.excedido("a", 3, VENTANA));
    }
}
