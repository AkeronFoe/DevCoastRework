package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.devcoast.model.Proyecto;
import com.example.devcoast.service.AreaEmpleo;

import lombok.Getter;

/** Una tarjeta del portafolio: datos ya preparados y validados (enlaces http/https, imagen http/https o data:image). */
@Getter
public class ProyectoVista {

    private String id;
    private String nombre;
    private String areaClave;     // p | d | o
    private String categoria;
    private String tipo;
    private String anio;
    private String descripcion;
    private List<String> herramientas = new ArrayList<>();
    private String enlace;        // null si no hay o no es http(s)
    private boolean publicado;
    private String portada;       // imagen subida por el talento, o null
    private int variante;         // 0-2: composición de la portada de diseño cuando no hay imagen
    private String nombreCorto;   // para la portada tipo editor (programación)

    public static ProyectoVista de(Proyecto p) {
        ProyectoVista v = new ProyectoVista();
        v.id = p.getId();
        v.nombre = p.getNombre();
        AreaEmpleo.Area area = AreaEmpleo.deProyecto(p.getCategoria());
        v.areaClave = area == AreaEmpleo.Area.PROGRAMACION ? "p" : area == AreaEmpleo.Area.DISENO ? "d" : "o";
        v.categoria = vacioANull(p.getCategoria());
        v.tipo = vacioANull(p.getTipo());
        v.anio = vacioANull(p.getAnio());
        v.descripcion = vacioANull(p.getDescripcionCorta());
        if (p.getStack() != null)
            for (String t : p.getStack())
                if (t != null && !t.isBlank())
                    v.herramientas.add(t.trim());
        v.enlace = urlSegura(p.getLinkProyecto());
        v.publicado = p.isPublicado();
        v.portada = imagenSegura(p.getBannerDataUrl());
        if (v.portada == null)
            v.portada = imagenSegura(p.getImagenUrl());
        v.variante = p.getId() == null ? 0 : Math.floorMod(p.getId().hashCode(), 3);
        // portada de código: el nombre y las herramientas reales del proyecto, no un código inventado
        v.nombreCorto = v.nombre == null ? "" : v.nombre.length() > 26 ? v.nombre.substring(0, 26) + "…" : v.nombre;
        return v;
    }

    /** Solo http(s). Cualquier otro esquema (javascript:, data:, etc.) se descarta. */
    public static String urlSegura(String url) {
        if (url == null)
            return null;
        String u = url.trim();
        return u.matches("(?i)^https?://[^\\s]+$") ? u : null;
    }

    private static String imagenSegura(String src) {
        if (src == null)
            return null;
        String s = src.trim();
        return s.matches("(?i)^https?://[^\\s]+$") || s.matches("(?i)^data:image/(png|jpe?g|webp|gif);base64,[A-Za-z0-9+/=]+$") ? s : null;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
