package com.example.devcoast.controller;

import java.util.Date;
import java.util.List;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Solicitud;

import lombok.Getter;

/** Una oferta de la cuenta de Empresa (Mis ofertas): los datos del pase más el conteo de sus postulaciones por estado. */
@Getter
public class OfertaEmpresaVista {

    private EmpleoVista oferta;
    private boolean activa;
    private boolean destacada;
    private Integer vacantes;
    private String contrato;
    private int total;
    private int enRevision;
    private int aceptadas;
    private int rechazadas;

    public static OfertaEmpresaVista de(Empleo e, List<Solicitud> postulaciones, Date ahora) {
        OfertaEmpresaVista v = new OfertaEmpresaVista();
        v.oferta = EmpleoVista.de(e, null, false, false, null, ahora);
        v.activa = !"CERRADO".equalsIgnoreCase(e.getEstado());
        v.destacada = e.isDestacar();
        v.vacantes = e.getVacantes();
        v.contrato = e.getContrato() == null || e.getContrato().isBlank() ? null : e.getContrato().trim();
        for (Solicitud s : postulaciones) {
            v.total++;
            String estado = s.getEstado();
            if ("aprobada".equals(estado))
                v.aceptadas++;
            else if ("rechazada".equals(estado))
                v.rechazadas++;
            else
                v.enRevision++;   // "revision" y las que no tienen estado (documentos viejos)
        }
        return v;
    }
}
