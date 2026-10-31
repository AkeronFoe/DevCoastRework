package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.devcoast.model.Proyecto;
import com.example.devcoast.service.AreaEmpleo;

import lombok.Getter;

@Getter
public class ProyectoVista {

    private String id;
    private String nombre;
    private String areaClave;
    private String categoria;
    private String tipo;
    private String anio;
    private String descripcion;
    private List<String> herramientas = new ArrayList<>();
    private String enlace;
    private boolean publicado;
    private String portada;
    private int variante;
    private String nombreCorto;

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
        v.nombreCorto = v.nombre == null ? "" : v.nombre.length() > 26 ? v.nombre.substring(0, 26) + "…" : v.nombre;
        return v;
    }

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
        return s.matches("(?i)^https?://[^\\s]+$")
                || s.matches("(?i)^data:image/(png|jpe?g|webp|gif);base64,[A-Za-z0-9+/=]+$") ? s : null;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
