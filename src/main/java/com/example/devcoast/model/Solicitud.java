package com.example.devcoast.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

// el índice unique usuario+empleo evita postulaciones duplicadas a nivel de BD,
// la validación del controller sola no alcanza si llegan dos requests al mismo tiempo
@Getter
@Setter
@CompoundIndexes({
    @CompoundIndex(name = "usuario_fecha_idx", def = "{'usuarioId': 1, 'fechaSolicitud': -1}"),
    @CompoundIndex(name = "empleo_fecha_idx",  def = "{'empleoId': 1,  'fechaSolicitud': -1}"),
    @CompoundIndex(name = "usuario_empleo_unique_idx", def = "{'usuarioId': 1, 'empleoId': 1}", unique = true)
})
@Document(collection = "solicitudes")
public class Solicitud {

    @Id
    private String id;

    private String usuarioId;
    private String usuarioCorreo;
    private String usuarioNombre;
    private String usuarioRolTitulo;

    private String empleoId;
    private String empleoTitulo;
    private String empleoCategoria;
    private String empleoSeniority;
    private String empleoModalidad;
    private String empleoContrato;
    private String empleoEmpresaNombre;
    private String empleoEmpresaSector;
    private String empleoEmpresaTamano;
    private String empleoEmpresaWeb;
    private String empleoEmpresaDescripcion;
    private String empleoIcono;
    private String empleoColorIcono;
    private String empleoStack;
    private String empleoSoftSkills;
    private String empleoDescripcionRol;
    private String empleoRequisitosDeseables;
    private String empleoMoneda;
    private Integer empleoSalarioMin;
    private Integer empleoSalarioMax;
    private String empleoSalarioPeriodo;
    private String empleoTimezone;
    private String empleoIngles;
    private Integer empleoVacantes;
    private boolean empleoMostrarSalario;

    private String estado;
    private Date   fechaSolicitud;

    private List<String>      proyectosIds      = new ArrayList<>();
    private String            mensajeMotivacion;
    private List<EventoEstado> historial         = new ArrayList<>();
}
