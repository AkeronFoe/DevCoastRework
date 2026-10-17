package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.AreaEmpleo;
import com.example.devcoast.service.MatchEmpleo;

import lombok.Getter;

/** Lo que necesita un pase de la lista de solicitudes del talento: estado, etapa, siguiente paso y match. */
@Getter
public class SolicitudVista {

    /** Una etapa de la línea de progreso: {@code estado} = hecha | actual | fin | pendiente. */
    public record Etapa(String nombre, String estado, String texto) {
    }

    public static final String[] ETAPAS = { "Enviada", "En revisión", "Decisión" };
    private static final String[] MESES = { "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic" };

    private String id;
    private String empleoId;
    private String cargo;
    private String empresa;
    private String empresaInicial;
    private String empresaColor;
    private String areaClave;
    private String categoria;
    private String hace;
    private Integer match;
    private String nivel;
    private String estado;          // revision | aprobada | rechazada | descartado
    private String claseEstado;     // s-rev | s-ace | s-rec
    private String estadoTexto;     // En revisión | Aceptada | No avanzó
    private String detalle;
    private String siguientePaso;
    private String sello;           // ACEPTADA | NO AVANZÓ | null
    private String claseSello;      // ok | no
    private boolean retirable;
    private int orden;              // para "Por estado": en revisión, aceptadas, cerradas
    private Date fecha;
    private List<Etapa> etapas = new ArrayList<>();

    public static SolicitudVista de(Solicitud s, Usuario talento, Date ahora) {
        SolicitudVista v = new SolicitudVista();
        v.id = s.getId();
        v.empleoId = s.getEmpleoId();
        v.cargo = s.getEmpleoTitulo();
        v.empresa = s.getEmpleoEmpresaNombre();
        v.empresaInicial = v.empresa != null && !v.empresa.isBlank() ? v.empresa.trim().substring(0, 1).toUpperCase(Locale.ROOT) : null;
        v.empresaColor = s.getEmpleoColorIcono() != null && s.getEmpleoColorIcono().matches("#[0-9a-fA-F]{3,8}") ? s.getEmpleoColorIcono() : null;
        AreaEmpleo.Area area = AreaEmpleo.de(s.getEmpleoCategoria());
        v.areaClave = area == AreaEmpleo.Area.PROGRAMACION ? "p" : area == AreaEmpleo.Area.DISENO ? "d" : "o";
        v.categoria = AreaEmpleo.categoriaVisible(s.getEmpleoCategoria());
        v.fecha = s.getFechaSolicitud();
        v.hace = hace(s.getFechaSolicitud(), ahora);

        // match con el stack que tenía la oferta cuando se envió la solicitud
        Empleo oferta = new Empleo();
        oferta.setStack(s.getEmpleoStack());
        MatchEmpleo.Resultado m = MatchEmpleo.calcular(talento, oferta);
        if (m != null) {
            v.match = m.porcentaje();
            v.nivel = m.nivel();
        }

        String estado = s.getEstado() == null ? "revision" : s.getEstado();
        v.estado = estado;
        int actual;
        boolean termino = false;
        switch (estado) {
            case "aprobada":
                v.claseEstado = "s-ace";
                v.estadoTexto = "Aceptada";
                v.detalle = "Aceptada" + conFecha(fechaEvento(s, "aprobada"));
                v.siguientePaso = "La empresa te eligió. Revisa tus mensajes: puede escribirte con los siguientes pasos.";
                v.sello = "ACEPTADA";
                v.claseSello = "ok";
                actual = 2;
                v.orden = 2;
                break;
            case "rechazada":
                v.claseEstado = "s-rec";
                v.estadoTexto = "No avanzó";
                v.detalle = "Respuesta" + conFecha(fechaEvento(s, "rechazada"));
                v.siguientePaso = "La empresa decidió no continuar esta vez. Puedes seguir postulándote desde Empleos.";
                v.sello = "NO AVANZÓ";
                v.claseSello = "no";
                actual = 2;
                termino = true;
                v.orden = 3;
                break;
            case "descartado":
                v.claseEstado = "s-rec";
                v.estadoTexto = "No avanzó";
                v.detalle = "Descartada automáticamente";
                v.siguientePaso = "Tu stack no cumplió los requisitos mínimos que definió la empresa. Mejora tu perfil y postúlate a otras ofertas.";
                v.sello = "NO AVANZÓ";
                v.claseSello = "no";
                actual = 1;
                termino = true;
                v.orden = 3;
                break;
            default:
                v.claseEstado = "s-rev";
                v.estadoTexto = "En revisión";
                v.detalle = "Enviada" + conFecha(s.getFechaSolicitud());
                v.siguientePaso = "La empresa está revisando tu postulación. Te avisaremos cuando responda.";
                v.retirable = true;
                actual = 1;
                v.orden = 1;
        }

        for (int i = 0; i < ETAPAS.length; i++) {
            String est = i < actual ? "hecha" : i == actual ? (termino ? "fin" : "actual") : "pendiente";
            String texto = "hecha".equals(est) ? " (completado)" : "actual".equals(est) ? " (etapa actual)"
                    : "fin".equals(est) ? " (aquí terminó)" : " (pendiente)";
            v.etapas.add(new Etapa(ETAPAS[i], est, texto));
        }
        return v;
    }

    private static Date fechaEvento(Solicitud s, String estado) {
        if (s.getHistorial() == null)
            return null;
        Date f = null;
        for (EventoEstado e : s.getHistorial())
            if (estado.equals(e.getEstado()) && e.getFecha() != null)
                f = e.getFecha();
        return f;
    }

    private static String conFecha(Date d) {
        return d == null ? "" : " el " + corta(d);
    }

    /** "14 oct" */
    static String corta(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MESES[c.get(Calendar.MONTH)];
    }

    static String hace(Date fecha, Date ahora) {
        if (fecha == null || ahora == null)
            return null;
        long dias = Math.max(0, (ahora.getTime() - fecha.getTime()) / 86_400_000L);
        return dias == 0 ? "hoy" : dias == 1 ? "hace 1 día" : dias < 30 ? "hace " + dias + " días"
                : dias < 365 ? "hace " + (dias / 30) + (dias / 30 == 1 ? " mes" : " meses") : "hace más de un año";
    }
}
