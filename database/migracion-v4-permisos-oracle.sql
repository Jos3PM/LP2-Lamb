-- Ejecutar una sola vez si ya se aplicó migracion-v3-oracle.sql.
ALTER TABLE FIA_ALUMNO_CUENTA ADD (puede_editar_carrera NUMBER(1) DEFAULT 0 NOT NULL);
