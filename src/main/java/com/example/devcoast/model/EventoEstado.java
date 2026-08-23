package com.example.devcoast.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoEstado {
    private String estado;
    private Date   fecha;
    private String nota;
}
