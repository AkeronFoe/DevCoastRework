package com.example.devcoast.controller;

import java.util.Date;
import java.util.Locale;

import com.example.devcoast.model.Invitacion;
import com.example.devcoast.service.AreaEmpleo;

import lombok.Getter;

@Getter
public class InvitacionVista {

    private String id;
    private String empleoId;
    private String cargo;
    private String empresa;
    private String empresaInicial;
    private String areaClave;
    private String categoria;
    private String modalidad;
    private String salarioTexto;
    private String hace;
    private String mensaje;
    private String estado;
    private String claseEstado;
    private String estadoTexto;
    private String sello;
    private String claseSello;
    private Date fecha;

    public static InvitacionVista de(Invitacion i, Date ahora) {
        InvitacionVista v = new InvitacionVista();
        v.id = i.getId();
        v.empleoId = i.getEmpleoId();
        v.cargo = i.getEmpleoTitulo() != null && !i.getEmpleoTitulo().isBlank() ? i.getEmpleoTitulo() : null;
        v.empresa = i.getEmpresaNombre();
        v.empresaInicial = v.empresa != null && !v.empresa.isBlank()
                ? v.empresa.trim().substring(0, 1).toUpperCase(Locale.ROOT)
                : null;
        AreaEmpleo.Area area = AreaEmpleo.de(i.getEmpleoCategoria());
        v.areaClave = area == AreaEmpleo.Area.PROGRAMACION ? "p" : area == AreaEmpleo.Area.DISENO ? "d" : "o";
        v.categoria = AreaEmpleo.categoriaVisible(i.getEmpleoCategoria());
        v.modalidad = i.getEmpleoModalidad() != null && !i.getEmpleoModalidad().isBlank() ? i.getEmpleoModalidad()
                : null;
        v.mensaje = i.getMensaje() != null && !i.getMensaje().isBlank() ? i.getMensaje().trim() : null;
        v.fecha = i.getFechaEnvio();
        v.hace = SolicitudVista.hace(i.getFechaEnvio(), ahora);
        v.estado = i.getEstado() == null ? "pendiente" : i.getEstado();
        switch (v.estado) {
            case "aceptada":
                v.claseEstado = "s-ace";
                v.estadoTexto = "Aceptada";
                v.sello = "ACEPTADA";
                v.claseSello = "ok";
                break;
            case "rechazada":
                v.claseEstado = "s-ret";
                v.estadoTexto = "Rechazada";
                v.sello = "RECHAZADA";
                v.claseSello = "";
                break;
            default:
                v.claseEstado = "s-inv";
                v.estadoTexto = "Por responder";
        }
        if (i.isEmpleoMostrarSalario() && (i.getEmpleoSalarioMin() != null || i.getEmpleoSalarioMax() != null)) {
            String mon = i.getEmpleoMoneda() == null ? "" : i.getEmpleoMoneda() + " ";
            String a = i.getEmpleoSalarioMin() != null
                    ? String.format(Locale.forLanguageTag("es"), "%,d", i.getEmpleoSalarioMin())
                    : null;
            String b = i.getEmpleoSalarioMax() != null
                    ? String.format(Locale.forLanguageTag("es"), "%,d", i.getEmpleoSalarioMax())
                    : null;
            v.salarioTexto = mon + (a != null && b != null ? a + " – " + b : a != null ? "Desde " + a : "Hasta " + b);
        }
        return v;
    }
}
