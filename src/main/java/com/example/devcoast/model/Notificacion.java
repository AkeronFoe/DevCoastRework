package com.example.devcoast.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// los índices son para: listar recientes por destinatario+fecha, y contar no leídas por destinatario rápido
@Getter
@Setter
@NoArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "dest_fecha_idx", def = "{'destinatario_id': 1, 'fecha_creacion': -1}"),
    @CompoundIndex(name = "dest_leida_idx", def = "{'destinatario_id': 1, 'leida': 1}")
})
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
