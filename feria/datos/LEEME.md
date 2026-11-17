# Datos curados de la feria

Esta carpeta es lo que ve el público en la demo. **Los archivos que vienen aquí son de ejemplo (ficticios):** reemplázalos por los
datos reales de la feria. Se editan con Excel, Google Sheets o cualquier editor de texto; guarda siempre como **CSV UTF-8**.

Las líneas que empiezan con `#` se ignoran. Las listas se separan con `;` (por ejemplo `Java;Spring Boot;Docker`).

## Cómo revisar y cargar

```bash
FERIA_SOLO_VALIDAR=true MONGODB_URI=... java -jar target/devcoast-0.0.1-SNAPSHOT.jar   # solo revisa; dice archivo y registro de cada error
DEVCOAST_PERFIL=feria ... java -jar ...                                                # carga al arrancar (se puede repetir sin duplicar)
./feria/reiniciar-demo.sh                                                              # con la app corriendo: deja la demo como el primer día
```

Si hay errores, **no se importa nada** y la app lo dice en el registro. Importar dos veces no duplica: las empresas y los
talentos se identifican por **correo**, las ofertas por **empresa + título** y los proyectos por **talento + nombre**.
Quitar una fila del CSV no borra lo ya cargado (usa el reinicio con `--registros` o borra desde la app).

## Archivos

### `empresas.csv`
`correo`*, `nombre`*, `sector`*, `tamano`, `ubicacion`, `sitio_web`, `representante`, `cargo`, `plan` (`GRATIS` o `PRO`), `logo`, `descripcion`

- `sector`: SaaS / Software, Fintech, E-commerce, Agencia creativa, Startup, Educacion, Salud, Videojuegos, Diseño, Marketing u Otro.
- `tamano`: `1 – 10 personas`, `11 – 50 personas`, `51 – 200 personas`, `201 – 500 personas` o `500+ personas`.
- El plan **GRATIS** admite 2 ofertas activas y el **PRO** hasta 15; si te pasas, la validación lo dice.

### `ofertas.csv`
`empresa_correo`*, `titulo`*, `categoria`*, `modalidad`*, `stack`*, `descripcion`*, `seniority`, `contrato`, `vacantes`, `moneda`, `salario_min`,
`salario_max`, `periodo`, `mostrar_salario` (`si`/`no`), `soft_skills`, `requisitos`, `ingles`, `timezone`, `destacar` (`si`; solo plan PRO),
`estado` (`ACTIVO`/`CERRADO`), `fase` (`ABIERTO`, `SCREENING`, `ENTREVISTAS`, `OFERTA`), `dias_publicado`

- `categoria`: `Programación · Frontend`, `Programación · Backend`, `Programación · Full Stack`, `Programación · Mobile`, `Diseño · UI/UX`,
  `Diseño · Grafico`, `Diseño · Motion`, `Diseño · 3D`, `DevOps / Infraestructura`, `Data / Analytics`, `Producto / UX`, `Marketing digital`…
- `modalidad`: Remoto, Híbrido o Presencial. `seniority`: Junior, Mid, Senior, Lead o vacío (sin restricción).
- Para que el **match** se vea bien, el `stack` de la oferta y el de los talentos deben usar los mismos nombres (`React`, `Java`, `Figma`…).

### `talentos.csv`
`correo`*, `nombre`*, `rol`*, `especialidad`* (`frontend`, `backend`, `fullstack`, `ui-ux`, `diseno-grafico`, `data`, `mobile`), `stack`*, `permiso`*, `bio`,
`modalidad`, `disponibilidad`, `ubicacion`, `salario`, `github`, `linkedin`, `portfolio`, `foto`, `mostrar_correo`, `destacar`, `nivel_ingles` (1-5),
`habilidades_blandas`, `cv_bullets`, `experiencias`, `educacion`, `password`

- **`permiso` debe ser `si`**: sin el permiso de la persona la fila no se importa (ni sus proyectos). Guarda ese permiso por escrito.
- El correo **no se muestra** salvo que `mostrar_correo` sea `si`. Para la demo pública, mejor dejarlo en `no`.
- `experiencias`: `cargo|empresa|periodo|descripcion` y varias separadas por `;;`. `educacion`: `institucion|titulo|periodo`, también con `;;`.
- Sin `password`, la cuenta queda con una clave aleatoria que nadie conoce (aparece en el directorio, pero no se puede entrar a ella).

### `proyectos.csv`
`talento_correo`*, `nombre`*, `categoria`*, `descripcion_corta`* (máx. 200 caracteres), `descripcion`, `tipo` (Personal, Cliente, Open source, Concepto), `anio`,
`link`, `stack`, `etiquetas`, `rol`, `duracion`, `destacar`, `imagen`

### `imagenes/`
`logos/`, `fotos/` y `proyectos/`. En el CSV se escribe la ruta relativa a `imagenes/` (por ejemplo `logos/mi-empresa.png`). Usa PNG o JPG; logos y fotos
cuadrados (256–400 px) y capturas de proyecto 16:9 (1200×675). Se sirven directamente desde el disco (`/feria-img/...`), no pasan por Cloudinary.

\* obligatorio

## Cuentas demo
Estos cuatro correos son especiales (clave `demo1234`, historia armada al reiniciar). No los cambies:
`demo@devcoast.co` (talento), `diseno@devcoast.co` (talento), `empresa@devcoast.co` (empresa PRO), `startup@devcoast.co` (empresa gratis).
Puedes cambiar su nombre, bio, stack, etc.; para que la historia funcione, la empresa PRO necesita ofertas activas y debe haber otros talentos en `talentos.csv`.
