package com.example.devcoast.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoEstado {
    private String estado;
    private Date   fecha;
    private String nota;

    public EventoEstado() {
    }

    public EventoEstado(String estado, Date fecha, String nota) {
        this.estado = estado;
        this.fecha = fecha;
        this.nota = nota;
    }
}
