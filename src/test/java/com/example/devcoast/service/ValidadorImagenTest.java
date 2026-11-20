package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ValidadorImagenTest {

    private static byte[] con(byte[] cabecera) {
        byte[] d = new byte[64];
        System.arraycopy(cabecera, 0, d, 0, cabecera.length);
        return d;
    }

    @Test
    void reconoceLosFormatosPermitidos() {
        assertEquals(ValidadorImagen.Tipo.JPEG, ValidadorImagen.detectar(con(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })));
        assertEquals(ValidadorImagen.Tipo.PNG,
                ValidadorImagen.detectar(con(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A })));
        assertEquals(ValidadorImagen.Tipo.GIF, ValidadorImagen.detectar(con("GIF89a".getBytes(StandardCharsets.US_ASCII))));
        byte[] webp = con("RIFF".getBytes(StandardCharsets.US_ASCII));
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 4);
        assertEquals(ValidadorImagen.Tipo.WEBP, ValidadorImagen.detectar(webp));
    }

    @Test
    void rechazaSvgYOtrosArchivos() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        assertNull(ValidadorImagen.detectar(svg));
        assertNotNull(ValidadorImagen.problema(svg));
        assertNotNull(ValidadorImagen.problema("<html></html>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rechazaVaciosYDemasiadoGrandes() {
        assertNotNull(ValidadorImagen.problema(new byte[0]));
        assertNotNull(ValidadorImagen.problema(null));
        byte[] grande = new byte[(int) ValidadorImagen.MAX_BYTES + 1];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;
        assertNotNull(ValidadorImagen.problema(grande));
    }

    @Test
    void aceptaUnaImagenValida() {
        assertNull(ValidadorImagen.problema(con(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })));
    }
}
