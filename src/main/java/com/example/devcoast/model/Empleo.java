package com.example.devcoast.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "empleos")
public class Empleo {

    @Id
    private String id;

    private String empresaNombre;
    private String empresaSector;
    private String empresaWeb;
    private String empresaTamano;
    private String empresaDescripcion;
    private String empresaId;
    private String empresaCorreo;

    private String icono;
    private String colorIcono;

    private String titulo;
    private String categoria;
    private String seniority;
    private String modalidad;
    private String contrato;

    private Integer vacantes;

    private String moneda;
    private Integer salarioMin;
    private Integer salarioMax;
    private String salarioPeriodo;

    private String timezone;
    private String ingles;

    private String descripcionRol;
    private String requisitosDeseables;

    private String stack;
    private String softSkills;

    private boolean mostrarSalario;
    private boolean matchAutomatico;
    private boolean destacar;
    private boolean notificacionesEmail;

    private String estado = "ACTIVO";

    private String faseReclutamiento = "ABIERTO";

    private boolean screeningActivo = false;
    private String screeningKeywords;

    private String logoUrl;

    private Date fechaPublicacion;

    public Date getFechaPublicacionEfectiva() {
        if (fechaPublicacion != null)
            return fechaPublicacion;
        if (id != null && ObjectId.isValid(id))
            return new ObjectId(id).getDate();
        return null;
    }

    public String getCategoriaMostrar() {
        return com.example.devcoast.service.AreaEmpleo.categoriaVisible(categoria);
    }

    public String getFormattedSalarioMin() {
        return formatSalary(this.salarioMin);
    }

    public String getFormattedSalarioMax() {
        return formatSalary(this.salarioMax);
    }

    private String formatSalary(Integer salary) {
        if (salary == null)
            return "";
        if (salary >= 1_000_000) {
            double val = salary / 1_000_000.0;
            return String.format("%.1fM", val).replaceAll("\\.0M$", "M");
        } else if (salary >= 1_000) {
            double val = salary / 1_000.0;
            return String.format("%.1fK", val).replaceAll("\\.0K$", "K");
        }
        return salary.toString();
    }
}