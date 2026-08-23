package com.example.devcoast.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "proyectos")
public class Proyecto {

    @Id
    private String id;

    @Field("usuario_id")
    private String usuarioId;

    @Field("usuario_correo")
    private String usuarioCorreo;

    private String nombre;
    private String categoria;
    private String tipo;

    @Field("descripcion_corta")
    private String descripcionCorta;

    @Field("descripcion_extendida")
    private String descripcionExtendida;

    private String anio;

    @Field("link_proyecto")
    private String linkProyecto;

    @Field("banner_clase")
    private String bannerClase;

    @Field("banner_data_url")
    private String bannerDataUrl;

    @Field("icono_clave")
    private String iconoClave;

    @Field("color_icono")
    private String colorIcono;

    @Field("logo_data_url")
    private String logoDataUrl;

    private List<String> stack = new ArrayList<>();
    private List<String> etiquetas = new ArrayList<>();

    private String rol;
    private String duracion;

    private boolean publicado;

    @Field("incluir_cv")
    private boolean incluirCv;

    private boolean destacar;

    @Field("creado_en")
    private Date creadoEn;

    @Field("actualizado_en")
    private Date actualizadoEn;

    @Field("imagen_url")
    private String imagenUrl;
}
