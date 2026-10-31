package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

final class DashboardCalculos {

    private DashboardCalculos() {
    }

    record Pendiente(String texto, String href) {
    }

    record Evento(String texto, String tono, String cuando, Date fecha) {
    }

    static String nombrePila(String nombre) {
        if (nombre == null || nombre.isBlank())
            return "";
        return nombre.trim().split("\\s+")[0];
    }

    static List<Pendiente> pendientesPerfil(Usuario u) {
        List<Pendiente> p = new ArrayList<>();
        if (u == null)
            return p;
        if (vacio(u.getRolTitulo()))
            p.add(new Pendiente("Escribir tu cargo", "/perfil"));
        if (vacio(u.getBio()))
            p.add(new Pendiente("Contar quién eres en tu biografía", "/perfil"));
        if (u.getStack() == null || u.getStack().isEmpty())
            p.add(new Pendiente("Agregar tu stack (lo usamos para calcular tu match)", "/perfil"));
        if (vacio(u.getUbicacion()))
            p.add(new Pendiente("Indicar tu ubicación", "/perfil"));
        if (vacio(u.getModalidad()))
            p.add(new Pendiente("Elegir tu modalidad de trabajo", "/perfil"));
        if (vacio(u.getEmailContacto()))
            p.add(new Pendiente("Agregar un correo de contacto", "/perfil"));
        if (vacio(u.getPortfolioUrl()))
            p.add(new Pendiente("Agregar el enlace a tu portafolio", "/perfil"));
        if (vacio(u.getLinkedinUrl()))
            p.add(new Pendiente("Agregar tu LinkedIn", "/perfil"));
        if (vacio(u.getGithubUrl()))
            p.add(new Pendiente("Agregar tu GitHub", "/perfil"));
        if (u.getCvExperiencias() == null || u.getCvExperiencias().isEmpty())
            p.add(new Pendiente("Añadir tu experiencia al currículum", "/curriculum"));
        if (u.getCvEducaciones() == null || u.getCvEducaciones().isEmpty())
            p.add(new Pendiente("Añadir tu educación al currículum", "/curriculum"));
        return p;
    }

    static List<Evento> eventosTalento(List<Solicitud> solicitudes, List<Invitacion> invitaciones, Date ahora,
            int max) {
        List<Evento> out = new ArrayList<>();
        for (Solicitud s : solicitudes) {
            String que = (s.getEmpleoEmpresaNombre() != null ? s.getEmpleoEmpresaNombre() : "la empresa")
                    + (s.getEmpleoTitulo() != null ? " (" + s.getEmpleoTitulo() + ")" : "");
            List<EventoEstado> h = s.getHistorial();
            if (h == null || h.isEmpty()) {
                out.add(evento("Enviaste tu solicitud a " + que + ".", "g", s.getFechaSolicitud(), ahora));
                continue;
            }
            boolean primero = true;
            for (EventoEstado e : h) {
                if (e.getFecha() == null)
                    continue;
                String estado = e.getEstado() == null ? "" : e.getEstado();
                switch (estado) {
                    case "aprobada" -> out.add(evento(que + " aceptó tu solicitud.", "t", e.getFecha(), ahora));
                    case "rechazada" ->
                        out.add(evento(que + " decidió no continuar con tu solicitud.", "g", e.getFecha(), ahora));
                    case "descartado" ->
                        out.add(evento("Tu solicitud a " + que + " se descartó por los requisitos mínimos.", "g",
                                e.getFecha(), ahora));
                    default -> out.add(evento(
                            primero ? "Enviaste tu solicitud a " + que + "."
                                    : "Tu solicitud a " + que + " volvió a revisión.",
                            "g", e.getFecha(), ahora));
                }
                primero = false;
            }
        }
        for (Invitacion i : invitaciones)
            if (i.getFechaEnvio() != null)
                out.add(evento((i.getEmpresaNombre() != null ? i.getEmpresaNombre() : "Una empresa") + " te invitó"
                        + (i.getEmpleoTitulo() != null && !i.getEmpleoTitulo().isBlank() ? " a " + i.getEmpleoTitulo()
                                : "")
                        + ".",
                        "t", i.getFechaEnvio(), ahora));
        return recientes(out, max);
    }

    static List<Evento> eventosEmpresa(List<Solicitud> solicitudes, List<Invitacion> invitaciones, Date ahora,
            int max) {
        List<Evento> out = new ArrayList<>();
        for (Solicitud s : solicitudes) {
            String quien = s.getUsuarioNombre() != null ? s.getUsuarioNombre() : "Alguien";
            String cargo = s.getEmpleoTitulo() != null ? s.getEmpleoTitulo() : "tu oferta";
            List<EventoEstado> h = s.getHistorial();
            if (h == null || h.isEmpty()) {
                out.add(evento(quien + " se postuló a " + cargo + ".", "t", s.getFechaSolicitud(), ahora));
                continue;
            }
            boolean primero = true;
            for (EventoEstado e : h) {
                if (e.getFecha() == null)
                    continue;
                String estado = e.getEstado() == null ? "" : e.getEstado();
                switch (estado) {
                    case "aprobada" ->
                        out.add(evento("Aceptaste a " + quien + " en " + cargo + ".", "g", e.getFecha(), ahora));
                    case "rechazada" ->
                        out.add(evento("Descartaste a " + quien + " en " + cargo + ".", "g", e.getFecha(), ahora));
                    case "descartado" ->
                        out.add(evento(quien + " se descartó solo por los requisitos de " + cargo + ".", "g",
                                e.getFecha(), ahora));
                    default -> {
                        if (primero)
                            out.add(evento(quien + " se postuló a " + cargo + ".", "t", e.getFecha(), ahora));
                    }
                }
                primero = false;
            }
        }
        for (Invitacion i : invitaciones)
            if (i.getFechaEnvio() != null)
                out.add(evento("Invitaste a " + (i.getTalentoNombre() != null ? i.getTalentoNombre() : "un talento")
                        + (i.getEmpleoTitulo() != null && !i.getEmpleoTitulo().isBlank() ? " a " + i.getEmpleoTitulo()
                                : "")
                        + ".",
                        "g", i.getFechaEnvio(), ahora));
        return recientes(out, max);
    }

    static Integer promedio(List<Integer> valores) {
        if (valores == null || valores.isEmpty())
            return null;
        return (int) Math.round(valores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private static List<Evento> recientes(List<Evento> todos, int max) {
        List<Evento> conFecha = new ArrayList<>(todos.stream().filter(e -> e.fecha() != null).toList());
        conFecha.sort(Comparator.comparing(Evento::fecha).reversed());
        return new ArrayList<>(conFecha.subList(0, Math.min(max, conFecha.size())));
    }

    private static Evento evento(String texto, String tono, Date fecha, Date ahora) {
        return new Evento(texto, tono, cuando(fecha, ahora), fecha);
    }

    static String cuando(Date fecha, Date ahora) {
        if (fecha == null)
            return "";
        long dias = Math.max(0, (ahora.getTime() - fecha.getTime()) / 86_400_000L);
        return dias == 0 ? "hoy"
                : dias == 1 ? "ayer" : dias < 30 ? "hace " + dias + " días" : SolicitudVista.corta(fecha);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
