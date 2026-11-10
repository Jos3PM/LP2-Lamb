import { expect, test } from '@playwright/test';

test('las pantallas administrativas requieren sesión y abren desde el portal', async ({ page }) => {
  await page.goto('/portal/#/planes-academicos');
  await expect(page).toHaveURL(/\/portal\/#\/$/);
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();

  await page.getByLabel('Usuario').fill('admin');
  await page.getByLabel('Contraseña').fill(process.env.FIA_ADMIN_PASSWORD || (() => { throw new Error('Define FIA_ADMIN_PASSWORD en el entorno E2E'); })());
  await page.getByRole('button', { name: 'Ingresar al sistema' }).click();
  await expect(page.getByRole('link', { name: /Planes académicos/ })).toBeVisible();
  await page.getByRole('link', { name: /Planes académicos/ }).click();
  await expect(page.getByRole('heading', { name: 'Planes académicos' })).toBeVisible();
  await page.getByRole('link', { name: /Volver al portal/ }).click();
  await page.getByRole('link', { name: /Organización docente/ }).click();
  await expect(page.getByRole('heading', { name: 'Organización de docentes' })).toBeVisible();
});
