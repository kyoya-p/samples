import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('https://batspi.com/');
  await page.locator('.hitarea').first().click();
  await page.getByRole('link', { name: 'カード絞込み' }).click();
  await page.locator('iframe[name="aswift_7"]').contentFrame().getByRole('button', { name: '広告を閉じる' }).click();
  await page.getByRole('listitem').filter({ hasText: 'BS01-001ゴラドン [スピリット/赤] スピリット0' }).locator('div').click();
  await page.getByText('スピリット0(0)/赤/爬獣<1>Lv1 1000 <3>').click();
  await page.getByText('BS01-001ゴラドン [スピリット/赤] スピリット0').click();
  await page.getByText('1', { exact: true }).nth(1).click();
  await page.getByText('スピリット0(0)/赤/爬獣<1>Lv1 1000 <3>').click({
    button: 'right'
  });
  await page.getByText('スピリット0(0)/赤/爬獣<1>Lv1 1000 <3>').click({
    button: 'right'
  });
  await page.getByText('BS01-001ゴラドン [スピリット/赤] スピリット0').click();
  await page.getByText('スピリット0(0)/赤/爬獣<1>Lv1 1000 <3>').click({
    button: 'right'
  });
  await page.getByText('スピリット0(0)/赤/爬獣<1>Lv1 1000 <3>').click();
});