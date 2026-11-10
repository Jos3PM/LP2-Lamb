import { expect, test } from '@playwright/test';

test('plan nuevo permite horarios visuales, valida solapes y conserva bloques al editar', async ({ page }) => {
  const version = `E2E-${Date.now().toString(36)}`;
  await page.goto('/portal/#/planes-academicos');
  await page.getByLabel('Usuario').fill('admin');
  await page.getByLabel('Contraseña').fill(process.env.FIA_ADMIN_PASSWORD || (() => { throw new Error('Define FIA_ADMIN_PASSWORD en el entorno E2E'); })());
  await page.getByRole('button', { name: 'Ingresar al sistema' }).click();
  await page.getByRole('link', { name: /Planes académicos/ }).click();
  await expect(page.getByRole('heading', { name: 'Planes académicos' })).toBeVisible();

  await page.getByLabel('Escuela / carrera').fill('Escuela E2E Horarios');
  await page.getByLabel('Versión / periodo').fill(version);
  await page.getByRole('button', { name: 'Crear plan' }).click();
  await expect(page.getByRole('heading', { name: /Cursos de Escuela E2E Horarios/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Editar plan' })).toBeVisible();
  await page.getByLabel('Nombre', { exact: true }).fill('Curso de horario visual');
  await page.getByLabel('Créditos').fill('3');
  await page.getByLabel('HT', { exact: true }).fill('2');
  await page.getByRole('button', { name: 'Seleccionar Lun 08:00–09:00' }).focus();
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Seleccionar Mar 10:00–11:00' }).click();
  await page.locator('.schedule-block').nth(0).getByLabel('Fin').fill('10:00');
  await page.locator('.schedule-block').nth(1).getByLabel('Fin').fill('12:00');
  await expect(page.locator('.schedule-summary')).toContainText('Lun 08:00–10:00; Mar 10:00–12:00');
  await expect(page.getByRole('button', { name: 'Revisar Lun 09:00–10:00' })).toHaveAttribute('aria-pressed', 'true');

  await page.getByRole('button', { name: 'Añadir bloque con horas precisas' }).click();
  await page.locator('.schedule-block').nth(2).getByLabel('Inicio').fill('08:30');
  await page.locator('.schedule-block').nth(2).getByLabel('Fin').fill('09:30');
  await expect(page.locator('.field-error')).toContainText('superpuestos');
  await page.getByRole('button', { name: 'Quitar bloque 3' }).click();
  await page.getByRole('button', { name: 'Añadir curso' }).click();
  const course = page.locator('.courses .item').filter({ hasText: 'Curso de horario visual' });
  await expect(course).toContainText('Lun 08:00-10:00; Mar 10:00-12:00');
  await page.getByLabel('Nombre', { exact: true }).fill('Curso con cruce');
  await page.getByLabel('Créditos').fill('2');
  await page.getByRole('button', { name: 'Seleccionar Lun 08:00–09:00' }).click();
  await page.getByRole('button', { name: 'Añadir curso' }).click();
  await expect(page.locator('.schedule-panel .field-error')).toContainText('Cruce de horario');
  await course.getByRole('button', { name: 'Editar' }).click();
  await expect(page.locator('.schedule-block')).toHaveCount(2);
  await page.screenshot({ path: 'test-results/academic-plans-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 820, height: 900 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Planes académicos' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.screenshot({ path: 'test-results/academic-plans-mobile.png', fullPage: true });
  await page.getByRole('button', { name: 'Cancelar edición' }).click();
  await page.getByLabel('Nombre', { exact: true }).fill('Curso con horario propuesto');
  await page.getByLabel('Créditos').fill('1');
  await page.getByLabel('HT', { exact: true }).fill('1');
  await expect(page.locator('.schedule-block')).toHaveCount(0);
  await page.getByRole('button', { name: 'Añadir curso' }).click();
  await expect(page.locator('.courses .item').filter({ hasText: 'Curso con horario propuesto' }))
    .toContainText(/(?:Lun|Mar|Mie|Jue|Vie|Sab|Dom) \d{2}:\d{2}-/);
});
