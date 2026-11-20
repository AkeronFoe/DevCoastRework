package com.example.devcoast.service;

import com.example.devcoast.model.Empleo;

public final class ValidacionEmpleo {

    public static final int MAX_TITULO = 120;
    public static final int MAX_CORTO = 120;
    public static final int MAX_DESCRIPCION_EMPRESA = 1500;
    public static final int MAX_DESCRIPCION_ROL = 5000;
    public static final int MAX_REQUISITOS = 3000;
    public static final int MAX_STACK = 600;
    public static final int MAX_SOFT = 400;
    public static final int MAX_KEYWORDS = 400;
    public static final int MAX_VACANTES = 500;
    public static final int MAX_SALARIO = 1_000_000_000;

    private ValidacionEmpleo() {
    }

    public static String validar(Empleo f) {
        if (vacio(f.getTitulo()) || vacio(f.getCategoria()) || vacio(f.getSeniority())
                || vacio(f.getModalidad()) || vacio(f.getContrato()) || vacio(f.getEmpresaNombre())
                || vacio(f.getEmpresaSector()) || vacio(f.getEmpresaTamano()) || vacio(f.getEmpresaDescripcion())
                || vacio(f.getStack()))
            return "Completa todos los campos obligatorios del empleo";

        if (largo(f.getTitulo(), MAX_TITULO))
            return "El título es demasiado largo (máximo " + MAX_TITULO + " caracteres)";
        for (String corto : new String[] { f.getCategoria(), f.getSeniority(), f.getModalidad(), f.getContrato(),
                f.getEmpresaNombre(), f.getEmpresaSector(), f.getEmpresaTamano(), f.getMoneda(), f.getSalarioPeriodo(),
                f.getTimezone(), f.getIngles(), f.getIcono(), f.getColorIcono() })
            if (largo(corto, MAX_CORTO))
                return "Hay un campo con demasiado texto (máximo " + MAX_CORTO + " caracteres)";
        if (largo(f.getEmpresaDescripcion(), MAX_DESCRIPCION_EMPRESA))
            return "La descripción de la empresa es demasiado larga (máximo " + MAX_DESCRIPCION_EMPRESA + " caracteres)";
        if (largo(f.getDescripcionRol(), MAX_DESCRIPCION_ROL))
            return "La descripción del rol es demasiado larga (máximo " + MAX_DESCRIPCION_ROL + " caracteres)";
        if (largo(f.getRequisitosDeseables(), MAX_REQUISITOS))
            return "Los requisitos deseables son demasiado largos (máximo " + MAX_REQUISITOS + " caracteres)";
        if (largo(f.getStack(), MAX_STACK))
            return "El stack es demasiado largo (máximo " + MAX_STACK + " caracteres)";
        if (largo(f.getSoftSkills(), MAX_SOFT))
            return "Las habilidades blandas son demasiado largas (máximo " + MAX_SOFT + " caracteres)";
        if (largo(f.getScreeningKeywords(), MAX_KEYWORDS))
            return "Las palabras del filtro automático son demasiado largas (máximo " + MAX_KEYWORDS + " caracteres)";

        if (f.getVacantes() == null || f.getVacantes() < 1)
            return "Las vacantes disponibles deben ser al menos 1";
        if (f.getVacantes() > MAX_VACANTES)
            return "Las vacantes no pueden pasar de " + MAX_VACANTES;
        if (fueraDeRango(f.getSalarioMin()) || fueraDeRango(f.getSalarioMax()))
            return "El salario debe ser un valor entre 0 y " + MAX_SALARIO;
        if (f.getSalarioMin() != null && f.getSalarioMax() != null && f.getSalarioMin() > f.getSalarioMax())
            return "El salario mínimo no puede ser mayor que el máximo";
        return null;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static boolean largo(String s, int max) {
        return s != null && s.length() > max;
    }

    private static boolean fueraDeRango(Integer v) {
        return v != null && (v < 0 || v > MAX_SALARIO);
    }
}
