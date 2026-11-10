-- Ejecutar conectado como FIA_APP en FREEPDB1 después de database/oracle.sql.
SELECT table_name FROM user_tables WHERE table_name LIKE 'FIA_%' ORDER BY table_name;
SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'FIA_%' ORDER BY sequence_name;
