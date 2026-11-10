# Base de datos de matrícula FIA

El backend usa JPA con las tablas Oracle `FIA_*` del esquema `FIA_APP`. El portal Angular se comunica con la API REST; nunca se conecta directamente a Oracle.

## Esquema nuevo

1. Conéctate a `FREEPDB1` como administrador y ejecuta `01-crear-usuario-fia.sql` **solo si `FIA_APP` no existe**.
2. Conéctate como `FIA_APP` y ejecuta `oracle.sql` **solo si el esquema está vacío**.
3. Habilita la importación inicial de planes y cursos con `IMPORTAR_FUENTES=true` durante el primer arranque. Después desactívala.

## Esquema FIA existente

Si `FIA_PLAN`, `FIA_CURSO` y las tablas de carga ya existen, no vuelvas a ejecutar `oracle.sql`. Ejecuta **una vez** `migracion-matricula-oracle.sql` como `FIA_APP`. Antes de hacerlo, confirma que no existen las seis tablas de matrícula ni sus secuencias. Si ya existen, no ejecutes de nuevo el script: Oracle devolverá errores por objetos duplicados.

Las tarifas se registran desde el módulo 2 con los importes oficiales del periodo; no se insertan cifras supuestas. Las matrículas conservan el monto y la moneda calculados en su fecha de emisión aunque después se cambie la tarifa del periodo. Los pagos son registros manuales y no procesan transferencias bancarias.

Para probar sin Oracle usa el perfil `demo`: crea una base H2 persistente en `data/fia.mv.db`, importa los planes y permite recorrer los seis módulos. Esa base de demostración es independiente de Oracle.

## Relaciones principales

| Tabla | Referencia |
| --- | --- |
| `FIA_ELECCION_CARRERA` | Estudiante y plan elegido por periodo |
| `FIA_TARIFA` | Importe base, precio por crédito y moneda por periodo |
| `FIA_MATRICULA` | Estudiante, plan, periodo, monto, pagado y estado |
| `FIA_MATRICULA_DET` | Cursos de cada matrícula |
| `FIA_PAGO` | Pagos de cada matrícula |

Hay claves foráneas, restricciones de importes y una clave única por estudiante/periodo. Al registrar un pago, el backend bloquea la matrícula en la transacción, rechaza importes mayores al saldo y actualiza pagado y estado.
