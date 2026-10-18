package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.example.devcoast.service.AreaEmpleo.Area;

class AreaEmpleoTest {

    @Test
    void agrupaLasCategoriasDelFormulario() {
        assertEquals(Area.PROGRAMACION, AreaEmpleo.de("Programacion · Frontend"));
        assertEquals(Area.PROGRAMACION, AreaEmpleo.de("Programación · Backend"));
        assertEquals(Area.PROGRAMACION, AreaEmpleo.de("DevOps / Infraestructura"));
        assertEquals(Area.PROGRAMACION, AreaEmpleo.de("Data / Analytics"));
        assertEquals(Area.PROGRAMACION, AreaEmpleo.de("Backend"));
        assertEquals(Area.DISENO, AreaEmpleo.de("Diseño · UI/UX"));
        assertEquals(Area.DISENO, AreaEmpleo.de("Diseño · Grafico"));
        assertEquals(Area.OTRAS, AreaEmpleo.de("Marketing digital"));
        assertEquals(Area.OTRAS, AreaEmpleo.de("Producto / UX"));
        assertEquals(Area.OTRAS, AreaEmpleo.de("Otro"));
        assertEquals(Area.OTRAS, AreaEmpleo.de(null));
    }

    @Test
    void laCategoriaSeMuestraConTilde() {
        assertEquals("Programación · Backend", AreaEmpleo.categoriaVisible("Programacion · Backend"));
        assertEquals("Diseño · Gráfico", AreaEmpleo.categoriaVisible("Diseño · Grafico"));
        assertEquals("Marketing digital", AreaEmpleo.categoriaVisible("Marketing digital"));
        assertNull(AreaEmpleo.categoriaVisible(null));
    }

    @Test
    void laClaveDelParametroSeInterpreta() {
        assertEquals(Area.DISENO, Area.deClave("diseno"));
        assertEquals(Area.PROGRAMACION, Area.deClave("Programación"));
        assertNull(Area.deClave("zzz"));
        assertNull(Area.deClave(null));
    }
}
