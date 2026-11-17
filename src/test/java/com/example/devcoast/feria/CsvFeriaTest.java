package com.example.devcoast.feria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.feria.CsvFeria.Fila;

class CsvFeriaTest {

    @Test
    void leeEncabezadoYFilas() {
        List<Fila> f = CsvFeria.parsear("correo,nombre\na@x.co,Ana\nb@x.co,Beto\n");
        assertEquals(2, f.size());
        assertEquals("a@x.co", f.get(0).get("correo"));
        assertEquals("Beto", f.get(1).get("nombre"));
    }

    @Test
    void respetaComillasComasYSaltosDeLineaDentroDeUnCampo() {
        List<Fila> f = CsvFeria.parsear("a,b\n\"uno, dos\",\"línea 1\nlínea 2\"\n\"dijo \"\"hola\"\"\",x\n");
        assertEquals("uno, dos", f.get(0).get("a"));
        assertEquals("línea 1\nlínea 2", f.get(0).get("b"));
        assertEquals("dijo \"hola\"", f.get(1).get("a"));
    }

    @Test
    void ignoraBomComentariosYLineasEnBlanco() {
        List<Fila> f = CsvFeria.parsear("﻿# comentario\r\ncorreo,nombre\r\n\r\n# otra\r\na@x.co,Ana\r\n");
        assertEquals(1, f.size());
        assertEquals("Ana", f.get(0).get("nombre"));
    }

    @Test
    void columnasQueFaltanSeLeenVacias() {
        Fila f = CsvFeria.parsear("a,b,c\n1\n").get(0);
        assertEquals("1", f.get("a"));
        assertTrue(f.vacio("b"));
        assertEquals("", f.get("c"));
        assertEquals("", f.get("noExiste"));
    }

    @Test
    void ayudantesDeTipo() {
        Fila f = CsvFeria.parsear("si,no,n,lista\nSí,no,\"7.000.000\",\"Java; React ;;Go\"\n").get(0);
        assertTrue(f.si("si"));
        assertFalse(f.si("no"));
        assertEquals(7000000, f.entero("n"));
        assertNull(f.entero("no"));
        assertEquals(List.of("Java", "React", "Go"), f.lista("lista"));
    }
}
