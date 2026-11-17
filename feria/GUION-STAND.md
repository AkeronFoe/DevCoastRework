# Guion del stand (2 a 3 minutos)

Funciona con los datos de ejemplo que trae `feria/datos`. Si cambias los datos, ajusta los nombres. **Antes de abrir el stand** corre
`./feria/chequeo.sh` y `./feria/reiniciar-demo.sh`: la historia de las cuentas se arma con fechas de «hoy».

**Atajo:** si prefieres que la pantalla cuente la historia sola, abre `/demo` y pulsa **▶ Ver la demo automática** (avanza sola, se puede pausar y vuelve a empezar al terminar).
Para presentar tú a tu ritmo con los pasos narrados, pulsa **▶ Presentar** en el menú lateral y avanza con «Siguiente». La tabla de abajo es el guion para hacerlo a mano.

**Preparación:** dos ventanas del navegador lado a lado (o laptop + celular), una como talento y otra como empresa.
Abre `/demo` en ambas (o escanea el QR del stand) y pulsa **Probar como talento** / **Probar como empresa**. El recorrido guiado
aparece solo; ciérralo con «Saltar» (se vuelve a ver desde el botón «Recorrido» del menú).

| Tiempo | Quién | Qué hacer | Qué decir |
| --- | --- | --- | --- |
| 0:00 | Talento (Carlos) | Mostrar el dashboard: solicitudes en ruta, la campana (4 avisos), «Tu travesía». | «Carlos es desarrollador full stack. Aquí ve en qué etapa está cada una de sus postulaciones.» |
| 0:25 | Talento | Ir a **Empleos**. Arriba aparece *Full Stack Developer (Java + React)* de DevCoast Labs con **match 100 %**. Señalar los chips «lo tienes». | «DevCoast calcula cuánto de lo que pide la oferta ya está en tu perfil.» |
| 0:50 | Talento | Pulsar **Postularme**. Sale el aviso «Solicitud enviada a DevCoast Labs». | «Un clic y la empresa ya la tiene.» |
| 1:05 | Empresa (DevCoast Labs) | Ir a **Postulaciones**: arriba, *Carlos Andrés Ramírez · se postuló hoy · Match 100 %*. | «Del otro lado, la empresa ve llegar la postulación al instante.» |
| 1:25 | Empresa | **Aceptar → Sí, aceptar.** | «Con un clic la acepta; Carlos recibe un aviso.» |
| 1:40 | Talento | Recargar. La campana trae «DevCoast Labs aceptó tu postulación». Abrirla y entrar a la solicitud → **Escribir a la empresa**. Enviar un mensaje. | «Y ya pueden hablar sin salir de la plataforma.» |
| 2:05 | Empresa | Abrir **Mensajes**: el mensaje de Carlos llegó. Responder. | |
| 2:20 | Empresa | **Directorio** → abrir un perfil con proyectos → **Invitar a este talento**. | «Las empresas también pueden ir por el talento.» |
| 2:40 | Cierre | Mostrar el QR / invitar al público a probarlo. | «Escanea, elige un rol y pruébalo tú.» |

## Si algo sale mal
- **Un visitante dejó la demo hecha un desastre** → botón **Reiniciar** en el menú lateral (o `./feria/reiniciar-demo.sh`). Tarda ~1 s.
- **La oferta de 100 % ya no aparece como pendiente** (alguien ya se postuló) → reinicia la demo.
- **No hay internet / el hosting se cayó** → plan B: `./feria/plan-b-local.sh` (ver `feria/README.md`).
- **La subida de fotos falla** → es normal sin internet o con Cloudinary caído; en la demo no hace falta subir nada.
- **Varias personas usando la misma cuenta demo** ven los cambios de las demás; es esperado. Reinicia si estorba.
