package com.example.devcoast.feria;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.example.devcoast.feria.CsvFeria.Fila;
import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.MatchEmpleo;
import com.example.devcoast.service.PlanService;

// arma la «historia» de las cuatro cuentas demo para que, al entrar, haya algo que contar: postulaciones en varios estados,
// invitaciones, un chat con mensajes sin leer, avisos en la campana y un cobro. Siempre con fechas relativas a hoy.
final class HistoriasDemo {

    private static final long DIA = 86_400_000L;

    private final MongoTemplate mongo;
    private final DatosFeria datos;
    private final Date ahora = new Date();
    private final AtomicInteger contador = new AtomicInteger(new SecureRandom().nextInt(0xFFFFFF));

    private final List<Solicitud> solicitudes = new ArrayList<>();
    private final List<Invitacion> invitaciones = new ArrayList<>();
    private final List<Mensaje> mensajes = new ArrayList<>();
    private final List<Notificacion> notificaciones = new ArrayList<>();
    private final List<Cobro> cobros = new ArrayList<>();
    private final Set<String> pares = new HashSet<>();

    private Usuario t1;
    private Usuario t2;
    private Empresa e1;
    private Empresa e2;
    private List<Usuario> otros = new ArrayList<>();
    private List<Empleo> ofertas = new ArrayList<>();

    HistoriasDemo(MongoTemplate mongo, DatosFeria datos) {
        this.mongo = mongo;
        this.datos = datos;
    }

    String reconstruir() {
        cargar();
        borrarAnterior();
        if (t1 != null)
            historiaTalentoPrincipal();
        if (t2 != null)
            historiaTalentoDiseno();
        if (e1 != null || e2 != null)
            historiaEmpresas();
        notificar();
        guardar();
        return solicitudes.size() + " postulaciones, " + invitaciones.size() + " invitaciones, " + mensajes.size() + " mensajes, "
                + notificaciones.size() + " avisos, " + cobros.size() + " cobros.";
    }

    // ---------------------------------------------------------------- carga y limpieza

    private void cargar() {
        t1 = usuario(FeriaService.DEMO_TALENTO);
        t2 = usuario(FeriaService.DEMO_DISENO);
        e1 = empresa(FeriaService.DEMO_EMPRESA);
        e2 = empresa(FeriaService.DEMO_STARTUP);
        List<String> correos = new ArrayList<>();
        for (Fila f : datos.talentos) {
            String c = f.get("correo").toLowerCase(Locale.ROOT);
            if (!FeriaService.DEMO_CORREOS.contains(c))
                correos.add(c);
        }
        if (!correos.isEmpty())
            otros = mongo.find(Query.query(Criteria.where("correo").in(correos)), Usuario.class);
        ofertas = mongo.find(Query.query(Criteria.where("estado").is("ACTIVO")), Empleo.class);
        ofertas.sort(Comparator.comparing(Empleo::getFechaPublicacion).reversed());
    }

    private Usuario usuario(String correo) {
        return mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Usuario.class);
    }

    private Empresa empresa(String correo) {
        return mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Empresa.class);
    }

    // lo que cuelga de las cuentas demo (postulaciones, invitaciones, chats, avisos y cobros) se borra antes de rehacerlo
    private void borrarAnterior() {
        List<String> talentos = new ArrayList<>();
        List<String> empresas = new ArrayList<>();
        if (t1 != null)
            talentos.add(t1.getId());
        if (t2 != null)
            talentos.add(t2.getId());
        if (e1 != null)
            empresas.add(e1.getId());
        if (e2 != null)
            empresas.add(e2.getId());
        List<String> ofertasEmpresas = new ArrayList<>();
        if (!empresas.isEmpty())
            for (Empleo e : mongo.find(Query.query(Criteria.where("empresaId").in(empresas)), Empleo.class))
                ofertasEmpresas.add(e.getId());

        Criteria deSolicitud = new Criteria().orOperator(
                Criteria.where("usuarioId").in(talentos),
                Criteria.where("empleoId").in(ofertasEmpresas));
        List<String> idsSolicitud = new ArrayList<>();
        for (Solicitud s : mongo.find(new Query(deSolicitud), Solicitud.class))
            idsSolicitud.add(s.getId());
        if (!idsSolicitud.isEmpty()) {
            mongo.remove(Query.query(Criteria.where("solicitudId").in(idsSolicitud)), Mensaje.class);
            mongo.remove(Query.query(Criteria.where("id").in(idsSolicitud)), Solicitud.class);
        }
        mongo.remove(new Query(new Criteria().orOperator(Criteria.where("talentoId").in(talentos),
                Criteria.where("empresaId").in(empresas))), Invitacion.class);
        List<String> todos = new ArrayList<>(talentos);
        todos.addAll(empresas);
        mongo.remove(Query.query(Criteria.where("destinatarioId").in(todos)), Notificacion.class);
        mongo.remove(Query.query(Criteria.where("empresaId").in(empresas)), Cobro.class);
    }

    // ---------------------------------------------------------------- historias

    private void historiaTalentoPrincipal() {
        List<Empleo> candidatas = new ArrayList<>(ofertas);
        candidatas.sort(Comparator.comparingInt((Empleo e) -> -MatchEmpleo.porcentaje(t1, e)));
        // la mejor oferta de la empresa PRO queda sin postular: es la que se usa en vivo en el guion del stand
        Empleo reservada = e1 == null ? null : candidatas.stream().filter(e -> e1.getId().equals(e.getEmpresaId())).findFirst().orElse(null);
        String[] estados = { "aprobada", "revision", "rechazada", "revision", "revision", "aprobada", "revision", "rechazada", "revision" };
        int hechos = 0;
        for (Empleo e : candidatas) {
            if (hechos >= estados.length)
                break;
            if (e == reservada)
                continue;
            Date f = atras(2 + hechos * 3.2);
            if (agregarSolicitud(t1, e, estados[hechos], f) != null)
                hechos++;
        }
        Solicitud aprobada = solicitudes.stream().filter(s -> t1.getId().equals(s.getUsuarioId()) && "aprobada".equals(s.getEstado()))
                .findFirst().orElse(null);
        if (aprobada != null)
            hilo(aprobada, new String[] {
                    "Hola %s, revisamos tu postulación a %s y nos encantó tu perfil. ¿Podemos agendar una conversación esta semana?",
                    "¡Hola! Muchas gracias. Claro que sí, tengo disponibilidad el jueves por la tarde o el viernes en la mañana.",
                    "Perfecto, agendemos el jueves a las 3:00 p. m. Te enviamos el enlace de la videollamada por aquí.",
                    "Quedo atento al enlace. ¡Gracias!",
                    "Listo, ya está el enlace en tu calendario. Nos vemos el jueves." }, true);
        // una segunda conversación, ya leída, para que la bandeja de mensajes no se vea con un solo chat
        Solicitud otraAprobada = solicitudes.stream().filter(s -> t1.getId().equals(s.getUsuarioId()) && "aprobada".equals(s.getEstado()) && s != aprobada)
                .findFirst().orElse(null);
        if (otraAprobada != null)
            hilo(otraAprobada, new String[] {
                    "Hola %s, gracias por postularte a %s. Revisamos tu portafolio y nos gustó. ¿Cuándo podríamos hablar?",
                    "¡Hola! Gracias a ustedes. Me sirve cualquier día de la próxima semana por la tarde.",
                    "Perfecto, te escribimos por aquí con la hora exacta." }, false);

        if (e1 != null) {
            Empleo o = ofertas.stream().filter(e -> e1.getId().equals(e.getEmpresaId()) && e != reservada).findFirst().orElse(null);
            if (o != null)
                invitaciones.add(invitacion(e1, t1, o, "pendiente", atras(1.5)));
        }
        if (e2 != null)
            invitaciones.add(invitacion(e2, t1, null, "pendiente", atras(4)));
        // un par de favoritos para que «Guardados» no aparezca vacío
        List<String> fav = new ArrayList<>();
        for (Empleo e : candidatas)
            if (fav.size() < 3 && !pares.contains(t1.getId() + "|" + e.getId()))
                fav.add(e.getId());
        t1.setEmpleosFavoritos(fav);
        mongo.save(t1);
    }

    private void historiaTalentoDiseno() {
        List<Empleo> candidatas = new ArrayList<>(ofertas);
        candidatas.sort(Comparator.comparingInt((Empleo e) -> -MatchEmpleo.porcentaje(t2, e)));
        String[] estados = { "revision", "aprobada", "revision", "rechazada", "revision" };
        int hechos = 0;
        for (Empleo e : candidatas) {
            if (hechos >= estados.length)
                break;
            Date f = atras(1 + hechos * 4.5);
            if (agregarSolicitud(t2, e, estados[hechos], f) != null)
                hechos++;
        }
        Solicitud aprobada = solicitudes.stream().filter(s -> t2.getId().equals(s.getUsuarioId()) && "aprobada".equals(s.getEstado()))
                .findFirst().orElse(null);
        if (aprobada != null)
            hilo(aprobada, new String[] {
                    "Hola %s, nos gustó mucho tu portafolio para %s. ¿Cuándo podrías mostrarnos tu proceso en una llamada corta?",
                    "¡Gracias! Puedo mañana a cualquier hora después de las 2:00 p. m.",
                    "Perfecto, mañana a las 3:00 p. m. te escribimos por aquí con el enlace." }, true);
        if (e2 != null)
            invitaciones.add(invitacion(e2, t2, null, "pendiente", atras(2)));
    }

    private void historiaEmpresas() {
        Empresa[] empresas = { e1, e2 };
        int[][] reparto = { { 6, 4, 3, 2 }, { 4, 3 } };
        String[] mezcla = { "revision", "revision", "aprobada", "revision", "rechazada", "revision", "revision", "aprobada", "revision" };
        for (int k = 0; k < empresas.length; k++) {
            Empresa em = empresas[k];
            if (em == null)
                continue;
            List<Empleo> propias = ofertas.stream().filter(e -> em.getId().equals(e.getEmpresaId())).toList();
            int rotacion = 0;
            for (int i = 0; i < propias.size(); i++) {
                Empleo e = propias.get(i);
                int cuantos = i < reparto[k].length ? reparto[k][i] : 2;
                List<Usuario> pool = new ArrayList<>(otros);
                pool.sort(Comparator.comparingInt((Usuario u) -> -MatchEmpleo.porcentaje(u, e)));
                int puestos = 0;
                for (Usuario u : pool) {
                    if (puestos >= cuantos)
                        break;
                    String estado = mezcla[(rotacion + puestos) % mezcla.length];
                    Date f = atras(1 + (rotacion + puestos) * 2.3);
                    if (agregarSolicitud(u, e, estado, f) != null)
                        puestos++;
                }
                rotacion += puestos;
            }
            // chat de la empresa con uno de sus candidatos aceptados
            Solicitud conChat = solicitudes.stream().filter(s -> "aprobada".equals(s.getEstado())
                    && propias.stream().anyMatch(o -> o.getId().equals(s.getEmpleoId())) && !FeriaService.DEMO_CORREOS.contains(s.getUsuarioCorreo()))
                    .findFirst().orElse(null);
            if (conChat != null)
                hilo(conChat, new String[] {
                        "Hola %s, bienvenido al proceso para %s. ¿Tienes disponibilidad para una prueba técnica corta esta semana?",
                        "¡Hola! Sí, con gusto. ¿Qué día les sirve?" }, true);
            if (em == e1)
                for (int i = 0; i < Math.min(3, otros.size()); i++)
                    invitaciones.add(invitacion(em, otros.get(i), primeraOferta(em), new String[] { "aceptada", "rechazada", "pendiente" }[i],
                            atras(8 + i * 5)));
        }
        // la empresa del plan gratis muestra el modelo de negocio: cada aceptación genera un cobro de colocación
        if (e2 != null)
            for (Solicitud s : solicitudes) {
                Empleo o = ofertas.stream().filter(x -> x.getId().equals(s.getEmpleoId())).findFirst().orElse(null);
                if (o != null && e2.getId().equals(o.getEmpresaId()) && "aprobada".equals(s.getEstado())) {
                    Cobro c = new Cobro();
                    c.setEmpresaId(e2.getId());
                    c.setSolicitudId(s.getId());
                    c.setEmpleoTitulo(s.getEmpleoTitulo());
                    c.setMonto(PlanService.TARIFA_COLOCACION);
                    c.setMotivo("Tarifa de colocación exitosa");
                    c.setFecha(fechaDe(s, "aprobada"));
                    c.setId(id(c.getFecha()));
                    cobros.add(c);
                }
            }
    }

    // ---------------------------------------------------------------- construcción de documentos

    private Empleo primeraOferta(Empresa em) {
        return ofertas.stream().filter(e -> em.getId().equals(e.getEmpresaId())).findFirst().orElse(null);
    }

    private Solicitud agregarSolicitud(Usuario u, Empleo e, String estado, Date fecha) {
        if (!pares.add(u.getId() + "|" + e.getId()))
            return null;
        Date decision = "revision".equals(estado) ? null : new Date(Math.min(ahora.getTime() - 3600_000L, fecha.getTime() + 2 * DIA));
        if (decision != null && decision.before(fecha))
            decision = fecha;
        Solicitud s = Documentos.solicitud(mongo, u, e, estado, fecha, decision, id(fecha));
        solicitudes.add(s);
        return s;
    }

    // plantillas con %s: primero el nombre de pila del talento, luego el título del empleo. Se alternan empresa, talento, empresa...
    private void hilo(Solicitud s, String[] textos, boolean ultimoSinLeer) {
        Empleo e = ofertas.stream().filter(x -> x.getId().equals(s.getEmpleoId())).findFirst().orElse(null);
        if (e == null)
            return;
        Date t = fechaDe(s, "aprobada");
        for (int i = 0; i < textos.length; i++) {
            boolean deEmpresa = i % 2 == 0;
            Mensaje m = new Mensaje();
            m.setSolicitudId(s.getId());
            m.setRemitenteId(deEmpresa ? e.getEmpresaId() : s.getUsuarioId());
            m.setRemitenteNombre(deEmpresa ? e.getEmpresaNombre() : s.getUsuarioNombre());
            m.setDestinatarioId(deEmpresa ? s.getUsuarioId() : e.getEmpresaId());
            String nombre = s.getUsuarioNombre() == null ? "" : s.getUsuarioNombre().split(" ")[0];
            m.setTexto(textos[i].contains("%s") ? String.format(textos[i], nombre, s.getEmpleoTitulo()) : textos[i]);
            m.setFechaEnvio(new Date(Math.min(ahora.getTime() - 60_000L, t.getTime() + i * 2 * 3600_000L)));
            m.setLeido(true);
            m.setId(id(m.getFechaEnvio()));
            mensajes.add(m);
        }
        // el último mensaje queda sin leer para que quien lo recibe vea la campana encendida
        if (ultimoSinLeer)
            mensajes.get(mensajes.size() - 1).setLeido(false);
    }

    private Invitacion invitacion(Empresa em, Usuario t, Empleo emp, String estado, Date fecha) {
        return Documentos.invitacion(em, t, emp, estado, fecha, id(fecha));
    }

    private void notificar() {
        Date corte = atras(60);
        for (Solicitud s : solicitudes) {
            Empleo e = ofertas.stream().filter(x -> x.getId().equals(s.getEmpleoId())).findFirst().orElse(null);
            if (e == null)
                continue;
            if (esDemoEmpresa(e.getEmpresaId()) && s.getFechaSolicitud().after(corte))
                notificaciones.add(notif(e.getEmpresaId(), "NUEVA_SOLICITUD", "Nueva postulación",
                        s.getUsuarioNombre() + " se postuló a " + s.getEmpleoTitulo(), s.getFechaSolicitud(),
                        "/empleos/" + e.getId() + "/postulaciones/" + s.getId()));
            String est = s.getEstado();
            if (esDemoTalento(s.getUsuarioId()) && ("aprobada".equals(est) || "rechazada".equals(est))) {
                boolean ok = "aprobada".equals(est);
                notificaciones.add(notif(s.getUsuarioId(), "ESTADO_SOLICITUD",
                        ok ? "¡Tu postulación fue aceptada! 🎉" : "Actualización de tu postulación",
                        s.getEmpleoEmpresaNombre() + (ok ? " aceptó tu postulación a " : " actualizó tu postulación a ") + s.getEmpleoTitulo(),
                        fechaDe(s, est), "/solicitudes/" + s.getId()));
            }
        }
        for (Invitacion i : invitaciones) {
            if ("pendiente".equals(i.getEstado()) && esDemoTalento(i.getTalentoId()))
                notificaciones.add(notif(i.getTalentoId(), "NUEVA_INVITACION", "Una empresa quiere conocerte",
                        i.getEmpresaNombre() + " te envió una invitación", i.getFechaEnvio(), "/solicitudes?estado=invitaciones"));
            else if (!"pendiente".equals(i.getEstado()) && esDemoEmpresa(i.getEmpresaId()))
                notificaciones.add(notif(i.getEmpresaId(), "ESTADO_INVITACION",
                        "aceptada".equals(i.getEstado()) ? "Invitación aceptada" : "Invitación rechazada",
                        i.getTalentoNombre() + ("aceptada".equals(i.getEstado()) ? " aceptó" : " rechazó") + " tu invitación",
                        new Date(i.getFechaEnvio().getTime() + DIA), "/talentos/" + i.getTalentoId()));
        }
        for (Solicitud s : solicitudes) {
            List<Mensaje> hilo = mensajes.stream().filter(m -> m.getSolicitudId().equals(s.getId())).toList();
            if (hilo.isEmpty())
                continue;
            Mensaje ult = hilo.get(hilo.size() - 1);
            if (!ult.isLeido())
                notificaciones.add(notif(ult.getDestinatarioId(), "NUEVO_MENSAJE", "Nuevo mensaje",
                        ult.getRemitenteNombre() + " te escribió", ult.getFechaEnvio(), "/chat/" + s.getId()));
        }
        // lo viejo ya está leído; lo reciente queda sin leer para que la campana tenga algo que mostrar
        for (Notificacion n : notificaciones)
            n.setLeida(n.getFechaCreacion().before(atras(5)));
        for (Notificacion n : notificaciones)
            if ("NUEVO_MENSAJE".equals(n.getTipo()) || "NUEVA_INVITACION".equals(n.getTipo()))
                n.setLeida(false);
        notificaciones.sort(Comparator.comparing(Notificacion::getFechaCreacion));
    }

    private boolean esDemoEmpresa(String id) {
        return (e1 != null && e1.getId().equals(id)) || (e2 != null && e2.getId().equals(id));
    }

    private boolean esDemoTalento(String id) {
        return (t1 != null && t1.getId().equals(id)) || (t2 != null && t2.getId().equals(id));
    }

    private Notificacion notif(String destinatario, String tipo, String titulo, String mensaje, Date fecha, String url) {
        return Documentos.notificacion(destinatario, tipo, titulo, mensaje, fecha, url, id(fecha));
    }

    private void guardar() {
        if (!solicitudes.isEmpty())
            mongo.insert(solicitudes, Solicitud.class);
        if (!invitaciones.isEmpty())
            mongo.insert(invitaciones, Invitacion.class);
        if (!mensajes.isEmpty())
            mongo.insert(mensajes, Mensaje.class);
        if (!notificaciones.isEmpty())
            mongo.insert(notificaciones, Notificacion.class);
        if (!cobros.isEmpty())
            mongo.insert(cobros, Cobro.class);
    }

    private Date fechaDe(Solicitud s, String estado) {
        for (int i = s.getHistorial().size() - 1; i >= 0; i--)
            if (estado.equals(s.getHistorial().get(i).getEstado()))
                return s.getHistorial().get(i).getFecha();
        return s.getFechaSolicitud();
    }

    private Date atras(double dias) {
        return new Date(ahora.getTime() - (long) (dias * DIA));
    }

    private String id(Date fecha) {
        return new ObjectId(fecha, contador.updateAndGet(c -> (c + 1) & 0xFFFFFF)).toHexString();
    }
}
