package com.example.devcoast.feria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

// al arrancar con DEVCOAST_PERFIL=feria carga los datos curados. Si la historia de las cuentas demo no existe aún, la arma.
// FERIA_REINICIAR=true la rehace aunque ya exista; FERIA_SOLO_VALIDAR=true revisa los archivos, imprime el informe y apaga la app.
@Component
@ConditionalOnProperty(name = "devcoast.perfil", havingValue = "feria")
public class FeriaArranque implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FeriaArranque.class);

    private final FeriaService feria;
    private final MongoTemplate mongo;
    private final ApplicationContext contexto;
    private final boolean soloValidar;
    private final boolean reiniciar;

    public FeriaArranque(FeriaService feria, MongoTemplate mongo, ApplicationContext contexto,
            @Value("${devcoast.feria.solo-validar:false}") boolean soloValidar,
            @Value("${devcoast.feria.reiniciar:false}") boolean reiniciar) {
        this.feria = feria;
        this.mongo = mongo;
        this.contexto = contexto;
        this.soloValidar = soloValidar;
        this.reiniciar = reiniciar;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Feria] Modo feria activo. Base de datos: {}", mongo.getDb().getName());
        DatosFeria datos = feria.leerDatos();
        datos.avisos.forEach(a -> log.warn("[Feria] {}", a));
        datos.errores.forEach(e -> log.error("[Feria] {}", e));
        if (soloValidar) {
            log.info("[Feria] Validación: {} → {}", datos.resumen(), datos.valido() ? "TODO BIEN" : datos.errores.size() + " ERRORES");
            int codigo = datos.valido() ? 0 : 1;
            System.exit(SpringApplication.exit(contexto, () -> codigo));
            return;
        }
        if (!datos.valido()) {
            log.error("[Feria] Los datos tienen errores: NO se importó nada. Corrige los archivos en {} y reinicia la app.",
                    feria.carpeta().toAbsolutePath());
            return;
        }
        avisarSiLaBaseTraeOtrosDatos(datos);
        try {
            if (reiniciar || !feria.hayHistoriaSembrada())
                feria.reiniciar(datos, false);
            else
                feria.importar(datos);
        } catch (RuntimeException e) {
            log.error("[Feria] No se pudo cargar la feria: {}", e.getMessage(), e);
        }
    }

    // la feria ya usa su propia base, pero si alguien la apunta a una con datos de otras fuentes conviene saberlo: ahí todo va lento y se mezcla
    private void avisarSiLaBaseTraeOtrosDatos(DatosFeria datos) {
        try {
            long ofertas = mongo.estimatedCount(com.example.devcoast.model.Empleo.class);
            if (ofertas > datos.ofertas.size() + 200)
                log.warn("[Feria] La base «{}» tiene {} ofertas y la feria solo trae {}. Parece mezclada con otros datos (¿el seeder masivo?): "
                        + "todo irá más lento y el directorio mostrará datos que no son de la feria. Usa una base nueva con FERIA_BASE_DE_DATOS.",
                        mongo.getDb().getName(), ofertas, datos.ofertas.size());
        } catch (RuntimeException ignorada) {
        }
    }
}
