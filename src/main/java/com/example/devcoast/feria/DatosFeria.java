package com.example.devcoast.feria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.devcoast.feria.CsvFeria.Fila;
import com.example.devcoast.service.EtiquetasPerfil;
import com.example.devcoast.service.PlanService;

// lee la carpeta feria/datos, la revisa y deja listo lo que se puede importar. Si algo está mal, dice en qué archivo y en qué fila
public final class DatosFeria {

    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public final List<Fila> empresas = new ArrayList<>();
    public final List<Fila> ofertas = new ArrayList<>();
    public final List<Fila> talentos = new ArrayList<>();
    public final List<Fila> proyectos = new ArrayList<>();
    public final List<String> errores = new ArrayList<>();
    public final List<String> avisos = new ArrayList<>();
    public final Path carpeta;

    private DatosFeria(Path carpeta) {
        this.carpeta = carpeta;
    }

    public boolean valido() {
        return errores.isEmpty();
    }

    public String resumen() {
        return empresas.size() + " empresas, " + ofertas.size() + " ofertas, " + talentos.size() + " talentos, " + proyectos.size()
                + " proyectos";
    }

    public static DatosFeria cargar(Path carpeta) {
        DatosFeria d = new DatosFeria(carpeta);
        if (!Files.isDirectory(carpeta)) {
            d.errores.add("No existe la carpeta de datos de la feria: " + carpeta.toAbsolutePath());
            return d;
        }
        List<Fila> empresas = d.leer("empresas.csv", true);
        List<Fila> ofertas = d.leer("ofertas.csv", false);
        List<Fila> talentos = d.leer("talentos.csv", false);
        List<Fila> proyectos = d.leer("proyectos.csv", false);

        Set<String> correosEmpresa = d.validarEmpresas(empresas);
        d.validarOfertas(ofertas, correosEmpresa);
        d.validarLimitesDePlan();
        Set<String> correosTalento = d.validarTalentos(talentos, correosEmpresa);
        d.validarProyectos(proyectos, correosTalento);
        return d;
    }

    private List<Fila> leer(String nombre, boolean obligatorio) {
        Path p = carpeta.resolve(nombre);
        if (!Files.isRegularFile(p)) {
            if (obligatorio)
                errores.add(nombre + ": no se encontró el archivo.");
            else
                avisos.add(nombre + ": no se encontró; no se importará nada de ese tipo.");
            return List.of();
        }
        try {
            return CsvFeria.leer(p);
        } catch (IOException e) {
            errores.add(nombre + ": no se pudo leer (" + e.getMessage() + ").");
            return List.of();
        }
    }

    private void err(String archivo, Fila f, String msg) {
        errores.add(archivo + " · registro " + f.numero() + ": " + msg);
    }

    private void aviso(String archivo, Fila f, String msg) {
        avisos.add(archivo + " · registro " + f.numero() + ": " + msg);
    }

    private boolean faltan(String archivo, Fila f, String... columnas) {
        boolean falta = false;
        for (String c : columnas)
            if (f.vacio(c)) {
                err(archivo, f, "falta la columna obligatoria «" + c + "».");
                falta = true;
            }
        return falta;
    }

    private boolean imagenValida(String archivo, Fila f, String columna) {
        if (f.vacio(columna))
            return true;
        String ruta = f.get(columna);
        Path base = carpeta.resolve("imagenes").normalize();
        Path img = base.resolve(ruta).normalize();
        if (!img.startsWith(base)) {
            err(archivo, f, "«" + columna + "» apunta fuera de la carpeta imagenes: " + ruta);
            return false;
        }
        if (!Files.isRegularFile(img)) {
            err(archivo, f, "no existe la imagen «" + ruta + "» (se busca en imagenes/).");
            return false;
        }
        return true;
    }

    private Set<String> validarEmpresas(List<Fila> filas) {
        Set<String> correos = new HashSet<>();
        for (Fila f : filas) {
            if (faltan("empresas.csv", f, "correo", "nombre", "sector"))
                continue;
            String correo = f.get("correo").toLowerCase(Locale.ROOT);
            if (!CORREO.matcher(correo).matches()) {
                err("empresas.csv", f, "el correo «" + correo + "» no es válido.");
                continue;
            }
            if (!correos.add(correo)) {
                err("empresas.csv", f, "el correo «" + correo + "» está repetido.");
                continue;
            }
            String plan = f.get("plan").toUpperCase(Locale.ROOT);
            if (!plan.isEmpty() && !plan.equals("GRATIS") && !plan.equals("PRO")) {
                err("empresas.csv", f, "«plan» debe ser GRATIS o PRO, no «" + f.get("plan") + "».");
                continue;
            }
            if (!imagenValida("empresas.csv", f, "logo"))
                continue;
            empresas.add(f);
        }
        return correos;
    }

    private void validarOfertas(List<Fila> filas, Set<String> correosEmpresa) {
        Set<String> claves = new HashSet<>();
        for (Fila f : filas) {
            if (faltan("ofertas.csv", f, "empresa_correo", "titulo", "categoria", "modalidad", "stack", "descripcion"))
                continue;
            String empresa = f.get("empresa_correo").toLowerCase(Locale.ROOT);
            if (!correosEmpresa.contains(empresa)) {
                err("ofertas.csv", f, "la empresa «" + empresa + "» no está en empresas.csv.");
                continue;
            }
            if (!claves.add(empresa + "|" + f.get("titulo").toLowerCase(Locale.ROOT))) {
                err("ofertas.csv", f, "la oferta «" + f.get("titulo") + "» está repetida para esa empresa.");
                continue;
            }
            String modalidad = EtiquetasPerfil.modalidadClave(f.get("modalidad"));
            if (modalidad.isEmpty()) {
                err("ofertas.csv", f, "«modalidad» debe ser Remoto, Híbrido o Presencial, no «" + f.get("modalidad") + "».");
                continue;
            }
            String estado = f.get("estado").toUpperCase(Locale.ROOT);
            if (!estado.isEmpty() && !estado.equals("ACTIVO") && !estado.equals("CERRADO")) {
                err("ofertas.csv", f, "«estado» debe ser ACTIVO o CERRADO, no «" + f.get("estado") + "».");
                continue;
            }
            ofertas.add(f);
        }
    }

    // el plan gratis admite 2 ofertas activas y el PRO 15: si los archivos se pasan, la app no las dejaría editar como se espera
    private void validarLimitesDePlan() {
        Map<String, Integer> activas = new LinkedHashMap<>();
        for (Fila f : ofertas)
            if (!"CERRADO".equalsIgnoreCase(f.get("estado")))
                activas.merge(f.get("empresa_correo").toLowerCase(Locale.ROOT), 1, Integer::sum);
        for (Fila f : empresas) {
            int n = activas.getOrDefault(f.get("correo").toLowerCase(Locale.ROOT), 0);
            boolean pro = "PRO".equalsIgnoreCase(f.get("plan"));
            int limite = pro ? PlanService.LIMITE_ACTIVAS_PRO : PlanService.LIMITE_ACTIVAS_GRATIS;
            if (n > limite)
                errores.add("ofertas.csv: " + f.get("nombre") + " tiene " + n + " ofertas activas y su plan " + (pro ? "PRO" : "GRATIS")
                        + " admite " + limite + ".");
        }
    }

    private Set<String> validarTalentos(List<Fila> filas, Set<String> correosEmpresa) {
        Set<String> correos = new HashSet<>();
        for (Fila f : filas) {
            if (faltan("talentos.csv", f, "correo", "nombre", "rol", "especialidad", "stack"))
                continue;
            String correo = f.get("correo").toLowerCase(Locale.ROOT);
            if (!CORREO.matcher(correo).matches()) {
                err("talentos.csv", f, "el correo «" + correo + "» no es válido.");
                continue;
            }
            if (!correos.add(correo) || correosEmpresa.contains(correo)) {
                err("talentos.csv", f, "el correo «" + correo + "» está repetido.");
                continue;
            }
            if (!f.si("permiso")) {
                aviso("talentos.csv", f, f.get("nombre") + " no tiene permiso = si; no se importa.");
                correos.remove(correo);
                continue;
            }
            if (!EtiquetasPerfil.CLAVES_ESPECIALIDAD.contains(EtiquetasPerfil.especialidadClave(f.get("especialidad")))) {
                err("talentos.csv", f, "«especialidad» debe ser una de " + EtiquetasPerfil.CLAVES_ESPECIALIDAD + ", no «"
                        + f.get("especialidad") + "».");
                continue;
            }
            if (!imagenValida("talentos.csv", f, "foto"))
                continue;
            talentos.add(f);
        }
        return correos;
    }

    private void validarProyectos(List<Fila> filas, Set<String> correosTalento) {
        Map<String, Fila> vistos = new LinkedHashMap<>();
        for (Fila f : filas) {
            if (faltan("proyectos.csv", f, "talento_correo", "nombre", "categoria", "descripcion_corta"))
                continue;
            String talento = f.get("talento_correo").toLowerCase(Locale.ROOT);
            if (!correosTalento.contains(talento)) {
                aviso("proyectos.csv", f, "«" + f.get("nombre") + "»: el talento «" + talento
                        + "» no está en talentos.csv (o no dio permiso); se omite.");
                continue;
            }
            if (vistos.putIfAbsent(talento + "|" + f.get("nombre").toLowerCase(Locale.ROOT), f) != null) {
                err("proyectos.csv", f, "el proyecto «" + f.get("nombre") + "» está repetido para ese talento.");
                continue;
            }
            if (!imagenValida("proyectos.csv", f, "imagen"))
                continue;
            proyectos.add(f);
        }
    }
}
