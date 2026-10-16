package com.example.devcoast.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// el índice es para: contar invitaciones pendientes/aceptadas/rechazadas por talento rápido
@Getter
@Setter
@NoArgsConstructor
@CompoundIndex(name = "talento_estado_idx", def = "{'talento_id': 1, 'estado': 1}")
@Document(collection = "invitaciones")
public class Invitacion {

    @Id
    private String id;

    @Field("empresa_id")
    private String empresaId;

    @Field("empresa_nombre")
    private String empresaNombre;

    @Field("empresa_sector")
    private String empresaSector;

    @Field("empresa_web")
    private String empresaWeb;

    @Field("empresa_descripcion")
    private String empresaDescripcion;

    @Field("empresa_tamano")
    private String empresaTamano;

    @Field("talento_id")
    private String talentoId;

    @Field("talento_nombre")
    private String talentoNombre;

    @Field("talento_correo")
    private String talentoCorreo;

    @Field("empleo_id")
    private String empleoId;

    @Field("empleo_titulo")
    private String empleoTitulo;

    @Field("empleo_modalidad")
    private String empleoModalidad;

    @Field("empleo_contrato")
    private String empleoContrato;

    @Field("empleo_categoria")
    private String empleoCategoria;

    @Field("empleo_salario_min")
    private Integer empleoSalarioMin;

    @Field("empleo_salario_max")
    private Integer empleoSalarioMax;

    @Field("empleo_moneda")
    private String empleoMoneda;

    @Field("empleo_mostrar_salario")
    private boolean empleoMostrarSalario;

    private String mensaje;

    private String estado = "pendiente";

    @Field("fecha_envio")
    private Date fechaEnvio;
}
