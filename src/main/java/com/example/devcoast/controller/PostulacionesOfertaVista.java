package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class PostulacionesOfertaVista {

    private String id;
    private String titulo;
    private String categoria;
    private String areaClave;
    private boolean activa;
    private int total;
    private int revision;
    private int aceptadas;
    private int noAvanzaron;
    private List<SolicitudVista> items = new ArrayList<>();

    public static PostulacionesOfertaVista de(String id, String titulo, String categoria, String areaClave,
            boolean activa,
            List<SolicitudVista> todas, List<SolicitudVista> filtradas) {
        PostulacionesOfertaVista v = new PostulacionesOfertaVista();
        v.id = id;
        v.titulo = titulo;
        v.categoria = categoria;
        v.areaClave = areaClave;
        v.activa = activa;
        v.total = todas.size();
        for (SolicitudVista s : todas) {
            switch (s.getEstado()) {
                case "aprobada" -> v.aceptadas++;
                case "rechazada", "descartado" -> v.noAvanzaron++;
                default -> v.revision++;
            }
        }
        v.items = filtradas;
        return v;
    }

    // la misma oferta con los mismos contadores, pero mostrando solo una parte de
    // sus postulaciones (para paginar)
    public PostulacionesOfertaVista conItems(List<SolicitudVista> parte) {
        PostulacionesOfertaVista v = new PostulacionesOfertaVista();
        v.id = id;
        v.titulo = titulo;
        v.categoria = categoria;
        v.areaClave = areaClave;
        v.activa = activa;
        v.total = total;
        v.revision = revision;
        v.aceptadas = aceptadas;
        v.noAvanzaron = noAvanzaron;
        v.items = parte;
        return v;
    }
}
