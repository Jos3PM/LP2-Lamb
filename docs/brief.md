# Brief técnico del proyecto FIA

## Datos del equipo

Nombre del equipo, sección, integrantes, cursos de cada integrante, repositorio y topics: **pendientes de completar por el equipo**. No se asigna autoría ficticia.

## Dominio

**Proyecto:** Gestión académica FIA: planes, carga y matrícula.

**Necesidad:** consultar los planes de FIA, preparar cargas académicas y generar matrículas con cursos seleccionados y montos calculados. Los datos curriculares proceden del ZIP; las relaciones de docentes, del Word. No se recibió tarifario ni reglamento institucional de matrícula.

**Actor del alcance:** persona que consulta los planes y prepara la carga desde la interfaz de gestión. El nombre institucional del rol está pendiente de confirmar. En U1 no se implementa autenticación.

Continuidad de un proyecto anterior: no informada.

## Módulos

**Plan de estudios — módulo no transaccional de referencia.** Entidades Plan y Curso; relación de docentes documentada como catálogo auxiliar del mismo módulo.

1. El sistema debe listar las escuelas y planes de FIA presentes en los PDF.
2. El sistema debe consultar y mantener cursos asociados a un plan con DTO validado y manejo de errores.
3. El sistema debe filtrar cursos por plan, ciclo y nombre y mostrar créditos, horas y origen documental.

**Carga académica — módulo transaccional.** Cabecera Carga y colección DetalleCarga; bloques horarios asociados a cada detalle.

1. El sistema debe registrar en una operación la cabecera y todos sus detalles.
2. El sistema debe comprobar que cada curso pertenece al plan seleccionado y evitar repetir curso y grupo dentro de la carga.
3. El sistema debe calcular el total de horas a partir de los cursos y revertir la operación completa si falla un detalle.
4. El sistema debe permitir consultar las cargas mediante filtros y un resumen agregado.

**Matrícula y pagos — módulo transaccional.** Entidades Estudiante, ElecciónCarrera, Tarifa, Matrícula, DetalleMatrícula y Pago.

1. **Registro de estudiantes:** validar y guardar código único, nombre y correo.
2. **Elección de carrera:** asociar estudiante, plan y periodo; actualizar la elección de ese periodo como una operación atómica.
3. **Configuración de tarifas:** registrar por periodo el importe base, importe por crédito y moneda. El equipo ingresa los valores oficiales; no se precargan precios inventados.
4. **Selección de cursos:** consultar el catálogo del plan elegido y aceptar solo cursos de ese plan, sin duplicados.
5. **Generación de matrícula y cálculo de deuda:** verificar elección, tarifa y ausencia de matrícula previa; guardar cabecera y detalles en una transacción. Fórmula de esta versión: tarifa base + (precio por crédito × créditos seleccionados).
6. **Registro de pagos:** aceptar pagos parciales hasta el saldo; guardar el pago y actualizar el estado (Pendiente, Parcial o Pagada) dentro de una transacción.
7. **Consulta de matrícula y pagos:** mostrar cursos, créditos, total, pagos acumulados, saldo y constancias registradas.

La app registra constancias manuales y no procesa cobros bancarios. Los prerrequisitos vienen como texto libre y no se pueden validar automáticamente en esta versión. Cambios de tarifa no alteran matrículas ya generadas.

Asignación de los módulos a integrantes: pendiente de completar. El brief de clase exige acordar la distribución por integrante con el docente.

## Alcance

Incluye planes, carga, matrícula y pagos para los planes FIA disponibles, sus servicios REST y la persistencia propuesta en Oracle. Fuera de alcance: otras facultades, integración con pasarelas de pago, autenticación/JWT, reglas de matrícula no documentadas y programación oficial no proporcionada.

Arquitectura: monolito modular Spring Boot con un datasource; Carga y Matrícula llaman a la API pública de Planes. Objetos Oracle y contrato REST en `database/oracle.sql` y `lp2-demo.md`.

## Aprobación

Docentes ADS, BD2 y LP2 y fecha: pendientes. Este documento no implica aprobación institucional.
