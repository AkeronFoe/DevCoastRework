package com.example.devcoast.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "cobros")
public class Cobro {

    @Id
    private String id;

    private String empresaId;
    private String solicitudId;
    private String empleoTitulo;
    private double monto;
    private String motivo;
    private Date fecha;
}
