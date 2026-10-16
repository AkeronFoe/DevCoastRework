package com.example.devcoast.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "empresas")
public class Empresa {

    @Id
    private String id;

    private String tipo;

    @Field("nombre_empresa")
    private String nombreEmpresa;

    @Field("nombre_representante")
    private String nombreRepresentante;

    @Field("cargo_representante")
    private String cargoRepresentante;

    private String sector;
    private String tamano;

    @Field("sitio_web")
    private String sitioWeb;

    private String correo;
    private String telefono;
    private String descripcion;
    private String ubicacion;
    private String password;
    private boolean activo = true;

    @Field("creado_en")
    private Date creadoEn;

    @Field("actualizado_en")
    private Date actualizadoEn;

    @Field("foto_url")
    private String fotoUrl;

    private String plan = "GRATIS";

    @Field("plan_vence_en")
    private Date planVenceEn;
}