package com.example.devcoast.feria;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

// FERIA_SIMULADOR=true: cada cierto tiempo pasa algo en las cuentas demo (una postulación nueva, un mensaje, una invitación) para que
// el dashboard de la empresa y la campana se vean vivos durante la demostración. Se apaga quitando la variable o reiniciando la app.
@Component
@ConditionalOnExpression("'${devcoast.perfil:}' == 'feria' and ${devcoast.feria.simulador:false}")
public class ActividadEnVivo {

    private static final Logger log = LoggerFactory.getLogger(ActividadEnVivo.class);
    private static final int MAX_POSTULACIONES_SIMULADAS = 14;
    private static final String[] MENSAJES_TALENTO = { "¡Gracias por la respuesta! Quedo atento a los siguientes pasos.",
            "Perfecto, esa fecha me sirve. ¿Hay algo que deba preparar?",
            "Me emociona mucho la oportunidad, ¿me comparten más detalles del equipo?" };
    private static final String[] MENSAJES_EMPRESA = { "¡Hola! Nos gustó tu perfil. ¿Tienes un espacio esta semana para conversar?",
            "Te confirmamos la reunión. Te enviamos el enlace por este chat en un momento.",
            "Gracias por tu tiempo. Estamos revisando y te respondemos pronto." };

    private final MongoTemplate mongo;
    private final FeriaService feria;
    private final int segundos;
    private final Random azar = new SecureRandom();
    private final AtomicInteger contador = new AtomicInteger(new SecureRandom().nextInt(0xFFFFFF));
    private final AtomicBoolean enMarcha = new AtomicBoolean();
    private ScheduledExecutorService ejecutor;

    public ActividadEnVivo(MongoTemplate mongo, FeriaService feria, @Value("${devcoast.feria.simulador-segundos:45}") int segundos) {
        this.mongo = mongo;
        this.feria = feria;
        this.segundos = Math.max(5, segundos);
        iniciar();
    }

    private void iniciar() {
        ejecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "devcoast-simulador");
            t.setDaemon(true);
            return t;
        });
        ejecutor.scheduleWithFixedDelay(this::tick, segundos, segundos, TimeUnit.SECONDS);
        log.info("[Feria] Simulador de actividad en vivo encendido: un evento cada {} s.", segundos);
    }

    @EventListener(ContextClosedEvent.class)
    public void apagar() {
        if (ejecutor != null)
            ejecutor.shutdownNow();
    }

    void tick() {
        if (!enMarcha.compareAndSet(false, true))
            return;
        try {
            switch (azar.nextInt(4)) {
                case 0, 1 -> postulacionNueva();
                case 2 -> mensajeNuevo();
                default -> invitacionNueva();
            }
        } catch (RuntimeException e) {
            log.warn("[Feria] El simulador no pudo generar un evento: {}", e.toString());
        } finally {
            enMarcha.set(false);
        }
    }

    private String id(Date f) {
        return new ObjectId(f, contador.updateAndGet(c -> (c + 1) & 0xFFFFFF)).toHexString();
    }

    private <T> T uno(List<T> l) {
        return l.isEmpty() ? null : l.get(azar.nextInt(l.size()));
    }

    private List<Empresa> empresasDemo() {
        return mongo.find(Query.query(Criteria.where("correo").in(FeriaService.DEMO_EMPRESA, FeriaService.DEMO_STARTUP)), Empresa.class);
    }

    // solo talentos de los archivos de la feria: nunca cuentas que visitantes se hayan creado durante el evento
    private List<Usuario> talentosCurados() {
        List<String> correos = new ArrayList<>();
        for (var f : feria.leerDatos().talentos) {
            String c = f.get("correo").toLowerCase(java.util.Locale.ROOT);
            if (!FeriaService.DEMO_CORREOS.contains(c))
                correos.add(c);
        }
        return correos.isEmpty() ? List.of() : mongo.find(Query.query(Criteria.where("correo").in(correos)), Usuario.class);
    }

    private void postulacionNueva() {
        Empresa em = uno(empresasDemo());
        if (em == null || postulacionesDesdeElReinicio(em) >= MAX_POSTULACIONES_SIMULADAS)
            return;
        List<Empleo> ofertas = mongo.find(Query.query(Criteria.where("empresaId").is(em.getId()).and("estado").is("ACTIVO")), Empleo.class);
        List<Usuario> talentos = talentosCurados();
        for (int intento = 0; intento < 12 && !ofertas.isEmpty() && !talentos.isEmpty(); intento++) {
            Empleo e = uno(ofertas);
            Usuario u = uno(talentos);
            if (mongo.exists(Query.query(Criteria.where("usuarioId").is(u.getId()).and("empleoId").is(e.getId())), Solicitud.class))
                continue;
            Date ahora = new Date();
            Solicitud s = Documentos.solicitud(mongo, u, e, "revision", ahora, null, id(ahora));
            mongo.insert(s);
            mongo.insert(Documentos.notificacion(em.getId(), "NUEVA_SOLICITUD", "Nueva postulación",
                    u.getNombre() + " se postuló a " + e.getTitulo(), ahora, "/empleos/" + e.getId() + "/postulaciones/" + s.getId(), id(ahora)));
            log.info("[Feria] Simulador: {} se postuló a {} ({}).", u.getNombre(), e.getTitulo(), em.getNombreEmpresa());
            return;
        }
    }

    // el tope se cuenta desde el último reinicio de la demo, así «Reiniciar mi demo» le devuelve el cupo al simulador
    private long postulacionesDesdeElReinicio(Empresa em) {
        org.bson.Document marca = mongo.findById("historias", org.bson.Document.class, "feria_meta");
        Date desde = marca == null ? new Date(0) : marca.getDate("fecha");
        return mongo.count(Query.query(Criteria.where("empleoEmpresaNombre").is(em.getNombreEmpresa()).and("fechaSolicitud").gt(desde)), Solicitud.class);
    }

    private void mensajeNuevo() {
        List<Usuario> demos = mongo.find(Query.query(Criteria.where("correo").in(FeriaService.DEMO_TALENTO, FeriaService.DEMO_DISENO)), Usuario.class);
        Usuario t = uno(demos);
        if (t == null)
            return;
        List<Solicitud> aprobadas = mongo.find(Query.query(Criteria.where("usuarioId").is(t.getId()).and("estado").is("aprobada")), Solicitud.class);
        Solicitud s = uno(aprobadas);
        if (s == null)
            return;
        Mensaje ultimo = mongo.findOne(Query.query(Criteria.where("solicitudId").is(s.getId())).with(Sort.by(Sort.Direction.DESC, "fechaEnvio")),
                Mensaje.class);
        Empleo e = mongo.findById(s.getEmpleoId(), Empleo.class);
        if (e == null)
            return;
        // la empresa escribe si el último mensaje es del talento (o no hay), y el talento responde en caso contrario
        boolean deEmpresa = ultimo == null || t.getId().equals(ultimo.getRemitenteId());
        Date ahora = new Date();
        Mensaje m = new Mensaje();
        m.setSolicitudId(s.getId());
        m.setRemitenteId(deEmpresa ? e.getEmpresaId() : t.getId());
        m.setRemitenteNombre(deEmpresa ? e.getEmpresaNombre() : t.getNombre());
        m.setDestinatarioId(deEmpresa ? t.getId() : e.getEmpresaId());
        m.setTexto(deEmpresa ? MENSAJES_EMPRESA[azar.nextInt(MENSAJES_EMPRESA.length)] : MENSAJES_TALENTO[azar.nextInt(MENSAJES_TALENTO.length)]);
        m.setFechaEnvio(ahora);
        m.setLeido(false);
        m.setId(id(ahora));
        mongo.insert(m);
        mongo.insert(Documentos.notificacion(m.getDestinatarioId(), "NUEVO_MENSAJE", "Nuevo mensaje", m.getRemitenteNombre() + " te escribió", ahora,
                "/chat/" + s.getId(), id(ahora)));
        log.info("[Feria] Simulador: {} escribió en el chat de {}.", m.getRemitenteNombre(), s.getEmpleoTitulo());
    }

    private void invitacionNueva() {
        List<Usuario> demos = mongo.find(Query.query(Criteria.where("correo").in(FeriaService.DEMO_TALENTO, FeriaService.DEMO_DISENO)), Usuario.class);
        Usuario t = uno(demos);
        List<Empresa> otras = mongo.find(Query.query(Criteria.where("correo").nin(FeriaService.DEMO_CORREOS)), Empresa.class);
        Empresa em = uno(otras);
        if (t == null || em == null)
            return;
        if (mongo.count(Query.query(Criteria.where("talentoId").is(t.getId()).and("estado").is("pendiente")), com.example.devcoast.model.Invitacion.class) >= 4)
            return;
        List<Empleo> ofertas = new ArrayList<>(mongo.find(Query.query(Criteria.where("empresaId").is(em.getId()).and("estado").is("ACTIVO")), Empleo.class));
        Empleo e = uno(ofertas);
        if (mongo.exists(Query.query(Criteria.where("talentoId").is(t.getId()).and("empresaId").is(em.getId())
                .and("empleoId").is(e == null ? null : e.getId())), com.example.devcoast.model.Invitacion.class))
            return;
        Date ahora = new Date();
        mongo.insert(Documentos.invitacion(em, t, e, "pendiente", ahora, id(ahora)));
        mongo.insert(Documentos.notificacion(t.getId(), "NUEVA_INVITACION", "Una empresa quiere conocerte", em.getNombreEmpresa() + " te envió una invitación",
                ahora, "/solicitudes?estado=invitaciones", id(ahora)));
        log.info("[Feria] Simulador: {} invitó a {}.", em.getNombreEmpresa(), t.getNombre());
    }
}
