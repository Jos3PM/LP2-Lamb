# Diagrama de base de datos FIA

Esquema Oracle `FIA_*` del proyecto, incluyendo las migraciones v3–v8. Las conexiones del diagrama son claves foráneas declaradas en el DDL. `PK` = clave primaria, `FK` = clave foránea, `UK` = única. Los campos no etiquetados como `NOT NULL` admiten nulos. `FIA_TARIFA` no tiene una FK declarada hacia matrícula; el periodo y la tarifa se relacionan funcionalmente al calcular el importe.

```mermaid
erDiagram
    FIA_PLAN ||--o{ FIA_CURSO : contiene
    FIA_PLAN ||--o{ FIA_CARGA : programa
    FIA_PLAN ||--o{ FIA_ELECCION_CARRERA : elegido_en
    FIA_PLAN ||--o{ FIA_MATRICULA : corresponde
    FIA_PLAN ||--o{ FIA_SOLICITUD_MATRICULA : solicitado
    FIA_DOCENTE_FUENTE ||--o| FIA_PROFESOR_CUENTA : cuenta
    FIA_DOCENTE_FUENTE o|--o{ FIA_CARGA_DETALLE : asignado
    FIA_CARGA ||--o{ FIA_CARGA_DETALLE : contiene
    FIA_CARGA ||--o{ FIA_CARGA_AUDIT : audita
    FIA_CARGA_DETALLE ||--o{ FIA_HORARIO : agenda
    FIA_CURSO ||--o{ FIA_CARGA_DETALLE : ofertado
    FIA_ESTUDIANTE ||--o| FIA_ALUMNO_CUENTA : acceso
    FIA_ESTUDIANTE ||--o{ FIA_ELECCION_CARRERA : elige
    FIA_ESTUDIANTE ||--o{ FIA_MATRICULA : registra
    FIA_ESTUDIANTE ||--o{ FIA_SOLICITUD_MATRICULA : solicita
    FIA_ESTUDIANTE ||--o{ FIA_CURSO_APROBADO : aprueba
    FIA_CURSO ||--o{ FIA_CURSO_APROBADO : aprobado
    FIA_MATRICULA ||--o{ FIA_MATRICULA_DET : incluye
    FIA_CURSO ||--o{ FIA_MATRICULA_DET : matriculado
    FIA_MATRICULA ||--o{ FIA_PAGO : recibe
    FIA_PAGO ||--o{ FIA_NOTIFICACION_INTENTO : notifica
    FIA_MATRICULA ||--o{ FIA_NOTIFICACION_INTENTO : notificacion_de

    FIA_PLAN {
        NUMBER_19 id PK
        VARCHAR2_150 school NOT_NULL
        VARCHAR2_20 version_plan NOT_NULL
        VARCHAR2_30 variant NOT_NULL
        VARCHAR2_300 source
    }
    FIA_CURSO {
        NUMBER_19 id PK
        NUMBER_19 plan_id FK_NOT_NULL
        NUMBER_10 cycle NOT_NULL
        VARCHAR2_300 name NOT_NULL
        NUMBER_10 credits
        NUMBER_10 ht
        NUMBER_10 hp
        NUMBER_10 hnp
        NUMBER_10 total
        VARCHAR2_3000 prerequisites
        VARCHAR2_1500 notes
        VARCHAR2_300 source
        NUMBER_10 page
        VARCHAR2_300 horario
        NUMBER_10 cupos_NOT_NULL
    }
    FIA_DOCENTE_FUENTE {
        NUMBER_19 id PK
        VARCHAR2_150 school
        VARCHAR2_250 name
        VARCHAR2_500 course
    }
    FIA_PROFESOR_CUENTA {
        VARCHAR2_80 username PK
        VARCHAR2_100 password_hash NOT_NULL
        NUMBER_19 docente_id FK_UK_NOT_NULL
        VARCHAR2_30 codigo UK_NOT_NULL
        VARCHAR2_180 correo UK_NOT_NULL
        NUMBER_1 activo_NOT_NULL
        VARCHAR2_80 creado_por NOT_NULL
        VARCHAR2_80 modificado_por NOT_NULL
        TIMESTAMP creado_en NOT_NULL
        TIMESTAMP modificado_en NOT_NULL
    }
    FIA_CARGA {
        NUMBER_19 id PK
        NUMBER_19 plan_id FK_NOT_NULL
        VARCHAR2_50 sede NOT_NULL
        VARCHAR2_30 semestre NOT_NULL
        VARCHAR2_80 contrato NOT_NULL
        VARCHAR2_20 estado NOT_NULL
        TIMESTAMP fecha NOT_NULL
        NUMBER_10 total_horas
        VARCHAR2_80 creado_por
        VARCHAR2_80 modificado_por
    }
    FIA_CARGA_AUDIT {
        NUMBER_19 id PK
        NUMBER_19 carga_id FK_NOT_NULL
        VARCHAR2_20 accion NOT_NULL
        VARCHAR2_80 actor NOT_NULL
        TIMESTAMP fecha NOT_NULL
        VARCHAR2_300 resumen
    }
    FIA_CARGA_DETALLE {
        NUMBER_19 id PK
        NUMBER_19 carga_id FK_NOT_NULL
        NUMBER_19 curso_id FK_NOT_NULL
        NUMBER_19 docente_id FK
        NUMBER_10 grupo NOT_NULL
        NUMBER_10 cupos
        VARCHAR2_200 ambiente
        VARCHAR2_60 condicion
        VARCHAR2_60 modalidad
        VARCHAR2_300 curso_nombre
        NUMBER_10 ciclo
        NUMBER_10 creditos
        NUMBER_10 ht
        NUMBER_10 hp
        NUMBER_10 hnp
        NUMBER_10 horas
        VARCHAR2_250 docente_nombre
    }
    FIA_HORARIO {
        NUMBER_19 detalle_id FK_NOT_NULL
        NUMBER_10 dia
        NUMBER_10 inicio
        NUMBER_10 fin
    }
    FIA_ESTUDIANTE {
        NUMBER_19 id PK
        VARCHAR2_30 codigo UK_NOT_NULL
        VARCHAR2_180 nombre NOT_NULL
        VARCHAR2_180 correo
        VARCHAR2_30 telefono
        VARCHAR2_180 apoderado
    }
    FIA_ALUMNO_CUENTA {
        VARCHAR2_80 username PK
        VARCHAR2_100 password_hash NOT_NULL
        NUMBER_19 estudiante_id FK_NOT_NULL
        NUMBER_1 puede_editar_carrera NOT_NULL
    }
    FIA_ELECCION_CARRERA {
        NUMBER_19 id PK
        NUMBER_19 estudiante_id FK_NOT_NULL
        NUMBER_19 plan_id FK_NOT_NULL
        VARCHAR2_30 periodo NOT_NULL
        TIMESTAMP fecha NOT_NULL
    }
    FIA_TARIFA {
        NUMBER_19 id PK
        VARCHAR2_30 periodo UK_NOT_NULL
        NUMBER_12_2 monto_base NOT_NULL
        NUMBER_12_2 por_credito NOT_NULL
        VARCHAR2_3 moneda NOT_NULL
        TIMESTAMP actualizado NOT_NULL
    }
    FIA_MATRICULA {
        NUMBER_19 id PK
        NUMBER_19 estudiante_id FK_NOT_NULL
        NUMBER_19 plan_id FK_NOT_NULL
        VARCHAR2_30 periodo NOT_NULL
        VARCHAR2_20 estado NOT_NULL
        NUMBER_10 creditos NOT_NULL
        NUMBER_12_2 monto NOT_NULL
        NUMBER_12_2 pagado NOT_NULL
        VARCHAR2_3 moneda NOT_NULL
        TIMESTAMP fecha NOT_NULL
    }
    FIA_MATRICULA_DET {
        NUMBER_19 id PK
        NUMBER_19 matricula_id FK_NOT_NULL
        NUMBER_19 curso_id FK_NOT_NULL
        VARCHAR2_300 curso NOT_NULL
        NUMBER_10 creditos NOT_NULL
    }
    FIA_PAGO {
        NUMBER_19 id PK
        NUMBER_19 matricula_id FK_NOT_NULL
        NUMBER_12_2 monto NOT_NULL
        VARCHAR2_30 metodo NOT_NULL
        VARCHAR2_100 referencia
        TIMESTAMP fecha NOT_NULL
        VARCHAR2_30 estado_NOT_NULL
        VARCHAR2_300 observacion
        TIMESTAMP revisado_en
        BLOB voucher_imagen
        VARCHAR2_30 voucher_mime
    }
    FIA_SOLICITUD_MATRICULA {
        NUMBER_19 id PK
        NUMBER_19 estudiante_id FK_NOT_NULL
        NUMBER_19 plan_id FK_NOT_NULL
        VARCHAR2_30 periodo NOT_NULL
        VARCHAR2_3000 curso_ids NOT_NULL
        NUMBER_10 creditos NOT_NULL
        NUMBER_12_2 monto NOT_NULL
        VARCHAR2_3 moneda NOT_NULL
        VARCHAR2_20 estado NOT_NULL
        TIMESTAMP fecha NOT_NULL
    }
    FIA_CURSO_APROBADO {
        NUMBER_19 id PK
        NUMBER_19 estudiante_id FK_NOT_NULL
        NUMBER_19 curso_id FK_NOT_NULL
    }
    FIA_NOTIFICACION_INTENTO {
        NUMBER_19 id PK
        NUMBER_19 pago_id FK_NOT_NULL
        NUMBER_19 matricula_id FK_NOT_NULL
        VARCHAR2_180 destino NOT_NULL
        VARCHAR2_30 canal NOT_NULL
        VARCHAR2_30 estado NOT_NULL
        VARCHAR2_300 resultado
        TIMESTAMP fecha NOT_NULL
    }
    FIA_AGENDA_LOCK {
        NUMBER_19 id PK
    }
```

## Notas sobre cardinalidad y restricciones

- `FIA_PLAN` agrupa cursos y sirve de referencia a cargas, elecciones, matrículas y solicitudes.
- Una carga contiene detalles de grupos. Cada detalle puede tener cero o un docente asignado; el detalle puede tener varios bloques horarios.
- Una matrícula pertenece a un estudiante y un plan; tiene líneas por curso y pagos. Estudiante/periodo es único.
- Elección de carrera y solicitud de matrícula también tienen unicidad por estudiante/periodo. Curso aprobado es único por estudiante/curso.
- Cuenta de profesor tiene relación 1:0..1 con docente por la restricción `UNIQUE(docente_id)`. Cuenta de alumno referencia a estudiante, pero el DDL no impone unicidad sobre `estudiante_id`.
- `FIA_TARIFA` tiene un registro único por periodo, pero no FK a `FIA_MATRICULA`; la matrícula guarda el monto y moneda calculados.
- El esquema base está en [oracle.sql](../database/oracle.sql). Las entidades añadidas o extendidas se encuentran en `migracion-v3-oracle.sql`, `migracion-v5-solicitudes-oracle.sql`, `migracion-v6-rendimiento-notificaciones-oracle.sql`, `migracion-v7-profesores-auditoria-oracle.sql` y `migracion-v8-resumen-auditoria-oracle.sql`.
