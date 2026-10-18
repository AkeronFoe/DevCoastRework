package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/** Una oferta de la cuenta de Empresa con las postulaciones que le llegaron (página Postulaciones). */
@Getter
public class PostulacionesOfertaVista {

    private String id;
    private String titulo;
    private String categoria;
    private String areaClave;
    private boolean activa;
    private int total;          // todas las postulaciones de la oferta, sin filtrar
    private int revision;
    private int aceptadas;
    private int noAvanzaron;    // rechazadas por la empresa o descartadas por el screening
    private List<SolicitudVista> items = new ArrayList<>();   // las que pasan el filtro de estado y búsqueda

    public static PostulacionesOfertaVista de(String id, String titulo, String categoria, String areaClave, boolean activa,
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
}
