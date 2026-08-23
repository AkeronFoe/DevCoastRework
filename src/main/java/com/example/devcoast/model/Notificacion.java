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
@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;

    @Field("destinatario_id")
    private String destinatarioId;

    // tipo: NUEVA_SOLICITUD | ESTADO_SOLICITUD | NUEVA_INVITACION
    private String tipo;

    private String titulo;
    private String mensaje;

    private boolean leida = false;

    @Field("fecha_creacion")
    private Date fechaCreacion;

    private String url;
}
