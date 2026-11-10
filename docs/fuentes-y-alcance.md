# Fuentes y decisiones de alcance

## Material de clase

- [Brief del proyecto integrador](https://262ciclo4.github.io/bomerp/proyecto-integrador/brief/): dos módulos por integrante, al menos uno transaccional; requisitos, entidades y límites explícitos.
- [S06 Evaluación de la Unidad I](https://262ciclo4.github.io/bomerp/lp2/sesiones/S06_Evaluacion_Unidad_1/): monolito modular, CRUD, DTO, validaciones, operación cabecera-detalle, transacción atómica, consultas, CORS, logs y pruebas. BomERP es una referencia estructural; el dominio de esta entrega es FIA.

Los enlaces no describen las reglas específicas de carga de la universidad. Las validaciones implementadas son de consistencia de los datos del módulo y no se presentan como reglamentos institucionales.

## Planes del ZIP

Se incorporan los 37 PDF, con 2638 registros de cursos distribuidos en 370 ciclos de planes. Un mismo curso puede existir en varios planes: no son 2638 asignaturas únicas. Se preservan los nombres de escuela, la versión del plan y las variantes SP tal como aparecen en los archivos.

Cada registro incluye procedencia y página. Se extrajeron créditos, HT, HP, HNP, total, prerrequisitos y texto de alternativas electivas. Las alternativas sin créditos propios se conservan como notas del electivo; no se duplican como cursos con horas inventadas.

`verificacion-datos.json` compara, por ciclo, las cinco sumas extraídas con el total impreso del PDF. Los 370 controles coinciden. Los PDF originales permanecen disponibles en la interfaz; no se editan al modificar el catálogo.

## Word Horarios FIA

Se extrajeron relaciones de cursos y docentes de los bloques identificados inequívocamente como Civil, Sistemas, Ambiental y Arquitectura. La coincidencia de cursos ignora mayúsculas y tildes; no equipara nombres diferentes, no corrige apellidos y no supone equivalencias curriculares.

El Word no especifica versiones de plan para los docentes. El buscador global permite seleccionar manualmente cualquier docente documentado de FIA. No se asignan docentes automáticamente. Se conservan las grafías originales; únicamente se deduplican diferencias de tildes, espacios y puntuación para el buscador.

El último bloque se titula AMBIENTAL pero contiene cursos alimentarios. Se conserva en `bloque-ambiguo-word.txt`; sus nombres de docentes se incorporan al catálogo global FIA sin atribuirles una carrera ni asignarles cursos por suposición.

## Capturas

Guían la paleta, barra superior, navegación lateral, filtros, tarjetas, tabla, formulario de asignación y cuadrícula de horario. La interfaz incluye planes, carga y matrícula. Sede Filial Juliaca, Regular 2026-2 y Escuela Profesional Pregrado son opciones observadas en las capturas. **2026-1 identifica la versión del plan y 2026-2 el semestre seleccionado: son conceptos distintos.**

No se proporcionó tarifario institucional. Por eso, Matrícula y pagos requiere configurar tarifa base, precio por crédito y moneda para cada periodo antes de generar matrículas. La fórmula se declara como regla configurable de esta entrega, no como política oficial de la universidad.

## Actualización solicitada por el usuario

El usuario autorizó generar ambientes y horarios para todas las carreras. En el perfil demo se guardan cinco cargas del último plan Regular de cada carrera, para Filial Juliaca y Regular 2026-2. Se conservan las cargas ya existentes. El botón de generación crea las faltantes sin reemplazar las guardadas.

Los ambientes FIA-01-C01 y similares son propuestas, no aulas institucionales verificadas. Se asigna un ambiente por carrera/ciclo. Los horarios distribuyen HT + HP en bloques de 50 minutos de lunes a viernes, usando los intervalos de las capturas. HNP no ocupa horario presencial. Esta es una regla de la propuesta, no una norma institucional acreditada. Cupos y docentes quedan sin definir hasta que el usuario los indique.

Se puede editar nombre, ciclo, grupo, créditos, HT, HP, HNP, cupos, docente, ambiente y bloques de cada fila; añadir grupos de cursos existentes y crear cursos manuales. Los cambios de carga no modifican el PDF original. El total se calcula en el servidor. Para cambiar HT/HP con horario existente debe ajustarse también la selección de bloques en Editar.

El servidor rechaza cruces en el mismo semestre para el mismo docente (por nombre normalizado), ambiente de la misma sede y cursos de la misma carrera/ciclo/grupo/sede, incluso entre versiones de plan. Intervalos adyacentes son válidos. No detecta matrículas individuales entre carreras porque no se proporcionaron esos datos.

Cada guardado es una transacción completa: un error revierte todos sus cambios. Un bloqueo pesimista de la agenda serializa las escrituras para comprobar disponibilidad antes de confirmar. No se añadieron aprobaciones, límites de contratación ni capacidades oficiales de aulas.
