-- Solo para una instalación de la versión anterior; ejecutar una vez antes de iniciar v2.
ALTER TABLE FIA_CARGA_DETALLE ADD (curso_nombre VARCHAR2(300 CHAR), ciclo NUMBER(10), creditos NUMBER(10), ht NUMBER(10), hp NUMBER(10), hnp NUMBER(10));
CREATE TABLE FIA_AGENDA_LOCK (id NUMBER(19) PRIMARY KEY);
INSERT INTO FIA_AGENDA_LOCK(id) VALUES (1);
COMMIT;
