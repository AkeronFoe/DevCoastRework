package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.AreaEmpleo.Area;
import com.example.devcoast.service.EtiquetasPerfil;

import lombok.Getter;

/** Lo que muestra la tarjeta del Directorio (y la vista previa de Mi perfil) de un talento. Solo datos del perfil. */
@Getter
public class TalentoVista {

    static final int MAX_STACK = 5;
    static final int MAX_BIO = 150;
    // el avatar de cada talento toma siempre el mismo color de la paleta del sistema
    private static final String[] PALETA = { "#FF7B4F", "#2DD4BF", "#CDD1DB", "#CC4B27", "#1F8F82" };

    private String id;
    private String nombre;
    private String inicial;
    private String color;
    private String fotoUrl;
    private String rol;
    private String ubicacion;
    private String bio;
    private String modalidad;
    private String disponibilidad;
    private String especialidad;
    private String areaClave;           // p | d | o
    private String categoria;           // etiqueta del área para el chip
    private List<String> stack = new ArrayList<>();
    private int stackExtra;
    private int proyectos;
    private List<String> coloresProyectos = new ArrayList<>();
    private String portfolioUrl;
    private String linkedinUrl;
    private String githubUrl;
    private boolean destacado;
    private boolean invitado;
    private boolean aceptaInvitaciones;

    public static TalentoVista de(Usuario u, int proyectos, List<String> colores, boolean destacado, boolean invitado) {
        TalentoVista v = new TalentoVista();
        v.id = u.getId();
        String nombre = u.getNombre() == null || u.getNombre().isBlank() ? "Talento" : u.getNombre().trim();
        v.nombre = nombre;
        v.inicial = nombre.substring(0, 1).toUpperCase();
        v.color = PALETA[Math.floorMod(u.getId() == null ? 0 : u.getId().hashCode(), PALETA.length)];
        v.fotoUrl = enlace(u.getFotoUrl());
        v.rol = vacioANulo(u.getRolTitulo());
        v.ubicacion = vacioANulo(u.getUbicacion());
        v.bio = corto(u.getBio());
        v.modalidad = EtiquetasPerfil.modalidad(u.getModalidad());
        v.disponibilidad = EtiquetasPerfil.disponibilidad(u.getDisponibilidad());
        v.especialidad = EtiquetasPerfil.especialidad(u.getEspecialidad());
        Area area = EtiquetasPerfil.area(u.getEspecialidad());
        v.areaClave = area == Area.PROGRAMACION ? "p" : area == Area.DISENO ? "d" : "o";
        v.categoria = v.especialidad != null ? v.especialidad : null;
        if (u.getStack() != null) {
            List<String> limpio = u.getStack().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
            v.stack = new ArrayList<>(limpio.subList(0, Math.min(MAX_STACK, limpio.size())));
            v.stackExtra = Math.max(0, limpio.size() - MAX_STACK);
        }
        v.proyectos = proyectos;
        v.coloresProyectos = colores == null ? new ArrayList<>() : new ArrayList<>(colores);
        v.portfolioUrl = enlace(u.getPortfolioUrl());
        v.linkedinUrl = enlace(u.getLinkedinUrl());
        v.githubUrl = enlace(u.getGithubUrl());
        v.destacado = destacado;
        v.invitado = invitado;
        v.aceptaInvitaciones = u.getPermitirMensajes() == null || u.getPermitirMensajes();
        return v;
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Solo http(s): un enlace guardado con otro esquema no se muestra. */
    private static String enlace(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.startsWith("http://") || t.startsWith("https://") ? t : null;
    }

    private static String corto(String bio) {
        if (bio == null || bio.isBlank())
            return null;
        String t = bio.trim().replaceAll("\\s+", " ");
        if (t.length() <= MAX_BIO)
            return t;
        int corte = t.lastIndexOf(' ', MAX_BIO);
        return t.substring(0, corte > MAX_BIO / 2 ? corte : MAX_BIO).trim() + "…";
    }
}
