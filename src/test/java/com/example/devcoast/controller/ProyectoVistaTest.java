package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Proyecto;

class ProyectoVistaTest {

    private Proyecto proyecto() {
        Proyecto p = new Proyecto();
        p.setId("64fabc12ab34cd56ef780001");
        p.setNombre("App de tareas");
        p.setCategoria("Full Stack");
        p.setStack(List.of("React", " Java ", ""));
        return p;
    }

    @Test
    void soloSeAceptanEnlacesHttpYHttps() {
        assertEquals("https://github.com/x/y", ProyectoVista.urlSegura(" https://github.com/x/y "));
        assertEquals("http://ejemplo.com", ProyectoVista.urlSegura("HTTP://ejemplo.com".toLowerCase()));
        assertNull(ProyectoVista.urlSegura("javascript:alert(1)"));
        assertNull(ProyectoVista.urlSegura("data:text/html;base64,AAAA"));
        assertNull(ProyectoVista.urlSegura("ftp://x.com"));
        assertNull(ProyectoVista.urlSegura("https://con espacio.com"));
        assertNull(ProyectoVista.urlSegura(" "));
        assertNull(ProyectoVista.urlSegura(null));
    }

    @Test
    void laPortadaSoloAcepta_httpOImagenesData() {
        Proyecto p = proyecto();
        p.setBannerDataUrl("javascript:alert(1)");
        p.setImagenUrl("https://res.cloudinary.com/x/imagen.png");
        assertEquals("https://res.cloudinary.com/x/imagen.png", ProyectoVista.de(p).getPortada());

        p.setBannerDataUrl("data:image/png;base64,iVBORw0KGgo=");
        assertEquals("data:image/png;base64,iVBORw0KGgo=", ProyectoVista.de(p).getPortada());

        p.setBannerDataUrl("data:text/html;base64,PHNjcmlwdD4=");
        p.setImagenUrl(null);
        assertNull(ProyectoVista.de(p).getPortada());
    }

    @Test
    void areaYHerramientasSalenDeLosDatos() {
        ProyectoVista v = ProyectoVista.de(proyecto());
        assertEquals("p", v.getAreaClave());
        assertEquals(List.of("React", "Java"), v.getHerramientas());
        assertEquals("App de tareas", v.getNombreCorto());

        Proyecto d = proyecto();
        d.setCategoria("Branding");
        assertEquals("d", ProyectoVista.de(d).getAreaClave());
        d.setCategoria("Otro");
        assertEquals("o", ProyectoVista.de(d).getAreaClave());
        d.setCategoria(null);
        assertEquals("o", ProyectoVista.de(d).getAreaClave());
    }

    @Test
    void lasVariantesDeLaPortadaEstanEntre0Y2() {
        for (String id : List.of("a", "b", "64fabc12ab34cd56ef780001", "zzz")) {
            Proyecto p = proyecto();
            p.setId(id);
            int v = ProyectoVista.de(p).getVariante();
            assertEquals(true, v >= 0 && v <= 2);
        }
    }
}
