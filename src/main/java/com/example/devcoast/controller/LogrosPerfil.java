package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.devcoast.model.Usuario;

final class LogrosPerfil {

    private LogrosPerfil() {
    }

    record Logro(String nombre, String descripcion, boolean desbloqueado, String progreso) {
    }

    static List<Logro> de(Usuario u, long proyectos, long invitacionesTotal, long solicitudesAceptadas) {
        boolean bio = lleno(u == null ? null : u.getBio());
        int completitud = u == null ? 0 : u.getCompletitudPerfil();
        boolean experiencia = u != null && u.getCvExperiencias() != null && !u.getCvExperiencias().isEmpty();
        boolean linkedin = lleno(u == null ? null : u.getLinkedinUrl());
        boolean github = lleno(u == null ? null : u.getGithubUrl());
        boolean disponibilidad = lleno(u == null ? null : u.getDisponibilidad());

        List<Logro> l = new ArrayList<>();
        l.add(new Logro("Miembro", "Bienvenido a DevCoast", true, ""));
        l.add(new Logro("Historia propia", "Biografía escrita", bio, bio ? "" : "Escribe tu biografía"));
        l.add(new Logro("Perfil sólido", "Perfil al 80 % o más", completitud >= 80, completitud >= 80 ? "" : completitud + " % de 80 %"));
        l.add(new Logro("Portafolio activo", "Al menos 1 proyecto", proyectos > 0, proyectos > 0 ? "" : "Sube tu primer proyecto"));
        l.add(new Logro("Portafolio rico", "3 o más proyectos", proyectos >= 3, proyectos >= 3 ? "" : proyectos + " de 3"));
        l.add(new Logro("Estrella", "5 o más proyectos", proyectos >= 5, proyectos >= 5 ? "" : proyectos + " de 5"));
        l.add(new Logro("Trayectoria", "Experiencia en tu currículum", experiencia, experiencia ? "" : "Agrégala en Curriculum"));
        l.add(new Logro("Conectado", "LinkedIn enlazado", linkedin, linkedin ? "" : "Agrega tu LinkedIn"));
        l.add(new Logro("Código público", "GitHub enlazado", github, github ? "" : "Agrega tu GitHub"));
        l.add(new Logro("Disponible", "Disponibilidad configurada", disponibilidad, disponibilidad ? "" : "Elige tu disponibilidad"));
        l.add(new Logro("Radar empresarial", "Primera invitación recibida", invitacionesTotal > 0,
                invitacionesTotal > 0 ? "" : "Activa tu perfil público"));
        l.add(new Logro("Preseleccionado", "Una solicitud aceptada", solicitudesAceptadas > 0,
                solicitudesAceptadas > 0 ? "" : "Postúlate a empleos"));
        return l;
    }

    private static boolean lleno(String s) {
        return s != null && !s.isBlank();
    }
}
