# Diagramas de arquitectura STOCKEA

Esta carpeta conserva los archivos `.drawio` editables y describe cómo consultarlos desde Markdown.

## Archivos

- `Diagrama de contenedores.drawio`: vista de contenedores y arquitectura objetivo. La solución incluye componentes planificados; el alcance implementado actualmente se limita al cambio de contraseña RF-007.7. Cloudflare/WAF/CDN, proxy inverso y demás capacidades aún no deben presentarse como desplegadas.
- `Diagramas_STOCKEA.drawio`: conjunto de seis vistas: componentes del backend, componentes del frontend, paquetes, módulo de cuenta, secuencia de registrar venta y secuencia de cambiar contraseña. La secuencia de contraseña refleja el endpoint `POST /api/v1/cuenta/contrasena`, autenticación JWT, límite de intentos y respuestas documentadas.

Los `.drawio` son las fuentes editables. Markdown no puede convertir por sí solo el diagrama en contenido editable; se recomienda conservar la fuente y exportar cada página como SVG para visualizarla en la documentación.

## Exportar una página a SVG

1. Abre el archivo `.drawio` en [diagrams.net](https://app.diagrams.net/).
2. En el archivo multipágina, selecciona la página que quieres documentar.
3. Ve a **Archivo → Exportar como → SVG**. Exporta una página a la vez.
4. Guarda los SVG en `docs/arquitectura/imagenes/` (crea la carpeta si no existe). Usa nombres descriptivos, por ejemplo `contenedores.svg` y `secuencia-cambio-contrasena.svg`.
5. Inserta la imagen en un documento Markdown con una ruta relativa:

```md
![Secuencia de cambio de contraseña](./imagenes/secuencia-cambio-contrasena.svg)
```

Para mantener una referencia sencilla a las fuentes editables, enlázalas desde Markdown:

```md
[Diagrama de contenedores editable](./Diagrama%20de%20contenedores.drawio)
[Diagramas STOCKEA editables](./Diagramas_STOCKEA.drawio)
```

Al editar un diagrama, vuelve a exportar su SVG y actualiza la imagen correspondiente. Conserva el archivo `.drawio` junto al SVG para permitir futuras modificaciones.
