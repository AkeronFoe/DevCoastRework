package com.example.devcoast.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

// los índices son para: paginar por solicitudId+fecha, y contar no leídos por destinatario rápido
@Getter
@Setter
@CompoundIndexes({
    @CompoundIndex(name = "sol_fecha_idx",  def = "{'solicitudId': 1, 'fechaEnvio': -1}"),
    @CompoundIndex(name = "dest_leido_idx", def = "{'destinatarioId': 1, 'leido': 1}")
})
@Document(collection = "mensajes")
public class Mensaje {

    @Id
    private String id;

    private String solicitudId;

    private String remitenteId;
    private String remitenteNombre;

    private String destinatarioId;

    private String  texto;
    private Date    fechaEnvio;
    private boolean leido = false;
}
