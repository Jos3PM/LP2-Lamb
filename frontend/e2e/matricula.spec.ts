import { test, expect, request as apiRequest } from '@playwright/test';

const base = process.env.FIA_BASE_URL || 'http://localhost:8080';
const adminPassword = process.env.FIA_ADMIN_PASSWORD;
if (!adminPassword) throw new Error('Define FIA_ADMIN_PASSWORD en el entorno de pruebas E2E');
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/vV8AAAAASUVORK5CYII=', 'base64');

test('alumno elige curso, paga con Yape y recibe constancia tras revisión', async ({ browser }) => {
  const unique = Date.now().toString(36);
  const admin = await apiRequest.newContext({ baseURL: base });
  await admin.get('/api/v1/auth/status');
  const csrf = (await admin.storageState()).cookies.find(cookie => cookie.name === 'XSRF-TOKEN')?.value;
  expect(csrf).toBeTruthy();
  const headers = { 'X-XSRF-TOKEN': decodeURIComponent(csrf!) };
  const login = await admin.post('/api/v1/auth/login', { headers, data: { username: 'admin', password: adminPassword } });
  expect(login.ok()).toBeTruthy();

  const plans = await (await admin.get('/api/v1/planes')).json();
  const plan = plans[0];
  const courses = await (await admin.get(`/api/v1/cursos?planId=${plan.id}&orden=cycle`)).json();
  const course = courses.find((c: { cycle: number; horario: string; credits: number }) => c.cycle === 1 && c.horario && c.credits > 0);
  expect(course).toBeTruthy();
  const period = `E2E-${unique}`;
  const username = `e2e${unique}`;
  const password = `Alumno-${unique}!Aa1`;
  const registered = await admin.post('/api/v1/auth/register', { headers, data: { username, password, codigo: `E2E-${unique}`, nombre: 'Alumno E2E', correo: 'alumno-e2e@example.test' } });
  expect(registered.ok()).toBeTruthy();
  const student = await registered.json();
  expect((await admin.post('/api/v1/elecciones-carrera', { headers, data: { estudianteId: student.estudianteId, planId: plan.id, periodo: period } })).ok()).toBeTruthy();
  expect((await admin.put('/api/v1/tarifas-matricula', { headers, data: { periodo: period, base: 100, porCredito: 25, moneda: 'PEN' } })).ok()).toBeTruthy();

  const alumnoContext = await browser.newContext();
  const alumno = await alumnoContext.newPage();
  await alumno.goto('/portal/');
  await alumno.getByLabel('Usuario').fill(username);
  await alumno.getByLabel('Contraseña').fill(password);
  await alumno.getByRole('button', { name: 'Ingresar al sistema' }).click();
  await expect(alumno.getByRole('heading', { name: 'Datos personales y responsable' })).toBeVisible();
  await alumno.getByLabel('Correo electrónico').fill('alumno-e2e@example.test');
  await alumno.getByLabel('Celular o teléfono').fill('999888777');
  await alumno.getByLabel(/Padre, madre o apoderado/).fill('Apoderado de prueba');
  await alumno.getByRole('checkbox', { name: /Confirmo que mis datos/ }).check();
  await alumno.getByRole('button', { name: /Guardar y continuar/ }).click();
  await expect(alumno.getByRole('heading', { name: 'Elige tus cursos y revisa el horario' })).toBeVisible();
  await alumno.locator('.course-tile').filter({ hasText: course.name }).getByRole('checkbox').check();
  await expect(alumno.locator('.selected-panel')).toContainText(course.name);
  await expect(alumno.locator('.timetable-panel')).toContainText(course.name);
  await alumno.getByRole('button', { name: /Confirmar cursos y ocupar cupos/ }).click();
  await expect(alumno.getByRole('heading', { name: 'Pago de matrícula' })).toBeVisible();
  await alumno.getByLabel('Número de operación').fill(`YAPE-${unique}`);
  await alumno.locator('input[type=file]').setInputFiles({ name: 'voucher.png', mimeType: 'image/png', buffer: png });
  await alumno.getByRole('button', { name: /Enviar pago de/ }).click();
  await expect(alumno.getByText('Pago enviado · pendiente de verificación')).toBeVisible();

  const pending = await (await admin.get('/api/v1/pagos/pendientes')).json();
  const payment = pending.find((p: { codigo: string }) => p.codigo === `E2E-${unique}`);
  expect(payment).toBeTruthy();
  expect((await admin.post(`/api/v1/matriculas/${payment.matriculaId}/pagos/${payment.id}/confirmar`, { headers })).ok()).toBeTruthy();
  await alumno.getByRole('button', { name: 'Actualizar estado' }).click();
  await expect(alumno.getByRole('button', { name: 'Ver boleta' })).toBeVisible();
  await alumno.getByRole('button', { name: 'Ver boleta' }).click();
  await expect(alumno.getByText('COMPROBANTE DE DEMOSTRACIÓN', { exact: true })).toBeVisible();
  await alumno.getByRole('button', { name: 'Ver contrato de matrícula' }).click();
  await expect(alumno.locator('.contract')).toContainText(course.name);
  await alumnoContext.close();
  await admin.dispose();
});

test('rechazo de 26 créditos no crea cabecera ni detalle', async () => {
  const admin = await apiRequest.newContext({ baseURL: base });
  await admin.get('/api/v1/auth/status');
  const csrf = (await admin.storageState()).cookies.find(cookie => cookie.name === 'XSRF-TOKEN')?.value;
  expect(csrf).toBeTruthy();
  const headers = { 'X-XSRF-TOKEN': decodeURIComponent(csrf!) };
  expect((await admin.post('/api/v1/auth/login', { headers, data: { username: 'admin', password: adminPassword } })).ok()).toBeTruthy();
  const plan = (await (await admin.get('/api/v1/planes')).json())[0];
  const courses = await (await admin.get(`/api/v1/cursos?planId=${plan.id}&orden=cycle`)).json();
  const selected: number[] = [];
  let credits = 0;
  for (const c of courses) { selected.push(c.id); credits += c.credits; if (credits > 25) break; }
  test.skip(credits <= 25, 'El plan de prueba necesita más de 25 créditos.');
  const unique = Date.now().toString(36);
  const period = `ERR-${unique}`;
  const student = await (await admin.post('/api/v1/estudiantes', { headers, data: { codigo: `ERR-${unique}`, nombre: 'Prueba rollback', correo: 'rollback@example.test' } })).json();
  await admin.post('/api/v1/elecciones-carrera', { headers, data: { estudianteId: student.id, planId: plan.id, periodo: period } });
  await admin.put('/api/v1/tarifas-matricula', { headers, data: { periodo: period, base: 100, porCredito: 25, moneda: 'PEN' } });
  const attempt = await admin.post('/api/v1/matriculas', { headers, data: { estudianteId: student.id, planId: plan.id, periodo: period, cursos: selected } });
  expect(attempt.status()).toBe(400);
  expect((await attempt.json()).detail).toContain('25 créditos');
  const page = await (await admin.get(`/api/v1/matriculas/pagina?periodo=${period}`)).json();
  expect(page.total).toBe(0);
  await admin.dispose();
});
