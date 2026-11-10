-- FIA Académico v8. Ejecutar sólo en esquema aislado validado, después de v7.
-- En producción requiere el procedimiento de respaldo y ventana DBA habitual.
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME='FIA_CARGA_AUDIT' AND COLUMN_NAME='RESUMEN';
  IF n=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE FIA_CARGA_AUDIT ADD (resumen VARCHAR2(300 CHAR))';
  END IF;
END;
/
