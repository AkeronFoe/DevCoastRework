package com.example.devcoast.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class LimiteIntentosLoginTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);
    private final LimiteIntentosLogin limite = new LimiteIntentosLogin(ahora::get);

    private void fallar(String ip, String correo, int veces) {
        for (int i = 0; i < veces; i++)
            limite.fallo(ip, correo);
    }

    @Test
    void alQuintoFalloSeguidoDeLaMismaCuentaDesdeElMismoEquipoSeBloquea() {
        fallar("1.1.1.1", "ana@x.com", 4);
        assertFalse(limite.bloqueado("1.1.1.1", "ana@x.com"));
        limite.fallo("1.1.1.1", "ana@x.com");
        assertTrue(limite.bloqueado("1.1.1.1", "ana@x.com"));
    }

    @Test
    void elBloqueoDeUnaCuentaYEquipoNoAfectaAOtraPersona() {
        fallar("1.1.1.1", "ana@x.com", 5);
        assertFalse(limite.bloqueado("2.2.2.2", "luis@x.com"));
        assertFalse(limite.bloqueado("1.1.1.1", "luis@x.com"));
    }

    @Test
    void pasadoLaVentanaVuelveAPoderIntentar() {
        fallar("1.1.1.1", "ana@x.com", 5);
        assertTrue(limite.bloqueado("1.1.1.1", "ana@x.com"));
        ahora.addAndGet(LimiteIntentosLogin.VENTANA_MS + 1);
        assertFalse(limite.bloqueado("1.1.1.1", "ana@x.com"));
    }

    @Test
    void unLoginCorrectoLimpiaLosFallosDeEsaCuentaYEquipo() {
        fallar("1.1.1.1", "ana@x.com", 4);
        limite.exito("1.1.1.1", "ana@x.com");
        fallar("1.1.1.1", "ana@x.com", 4);
        assertFalse(limite.bloqueado("1.1.1.1", "ana@x.com"));
    }

    @Test
    void unMismoEquipoProbandoMuchasCuentasTambienSeBloquea() {
        for (int i = 0; i < LimiteIntentosLogin.MAX_POR_EQUIPO; i++)
            limite.fallo("9.9.9.9", "persona" + i + "@x.com");
        assertTrue(limite.bloqueado("9.9.9.9", "otra@x.com"));
    }

    @Test
    void variasPersonasProbandoLaMismaCuentaDesdeEquiposDistintosTambienSeBloquea() {
        for (int i = 0; i < LimiteIntentosLogin.MAX_POR_CUENTA; i++)
            limite.fallo("10.0.0." + i, "victima@x.com");
        assertTrue(limite.bloqueado("10.0.1.1", "victima@x.com"));
    }

    @Test
    void mayusculasYEspaciosNoSirvenParaEvadirElLimite() {
        fallar("1.1.1.1", "Ana@X.com ", 5);
        assertTrue(limite.bloqueado("1.1.1.1", "ana@x.com"));
    }

    @Test
    void limpiarQuitaLoQueYaExpiro() {
        fallar("1.1.1.1", "ana@x.com", 3);
        ahora.addAndGet(LimiteIntentosLogin.VENTANA_MS + 1);
        limite.limpiar();
        fallar("1.1.1.1", "ana@x.com", 4);
        assertFalse(limite.bloqueado("1.1.1.1", "ana@x.com"));
    }
}
