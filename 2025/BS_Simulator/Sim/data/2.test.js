import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('https://batspi.com/');
  await page.locator('.hitarea').first().click();
  await page.getByRole('link', { name: 'カード絞込み' }).click();
  await page.locator('iframe[name="aswift_7"]').contentFrame().getByRole('button', { name: '広告を閉じる' }).click();
  await page.getByRole('combobox').nth(1).selectOption('200');

  for(var i=1;i<=2;++i){
    await page.getByText(i.toString(), { exact: true }).nth(1).click();
    const htmlContent = await page.content();
    console.log(htmlContent)
  }
});
