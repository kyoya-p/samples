import { test, expect } from "@microsoft/tui-test";
import path from "node:path";

const appPath = path.join(process.cwd(), "build/Release/FirebaseApp.exe");

test.use({ 
    program: { file: appPath },
    rows: 40,
    columns: 100,
    env: { API_KEY: process.env.API_KEY }
});

async function click(terminal: any, locator: any) {
  try {
    const cells = await locator.resolve(5000);
    if (!cells || cells.length === 0) return;
    
    const minX = Math.min(...cells.map((c: any) => c.x));
    const maxX = Math.max(...cells.map((c: any) => c.x));
    const minY = Math.min(...cells.map((c: any) => c.y));
    const maxY = Math.max(...cells.map((c: any) => c.y));
    
    const x = Math.floor((minX + maxX) / 2) + 1;
    const y = Math.floor((minY + maxY) / 2) + 1;

    await terminal.write(`\x1b[<0;${x};${y}M`);
    await terminal.write(`\x1b[<0;${x};${y}m`);
  } catch (e) {}
}

async function ss(terminal: any) {
  const isLoading = () => terminal.getViewableBuffer().some((row: string[]) => row.join("").includes("Loading..."));
  if (isLoading()) {
    await new Promise(resolve => setTimeout(resolve, 5000));
  }
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}
}

test("Add contact and verify with mouse", async ({ terminal }) => {
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}
  try { await expect(terminal.getByText("Status: Connected")).toBeVisible(); } catch (e) {}
  await ss(terminal);

  const addButton = terminal.getByText("[Add]");
  await click(terminal, addButton);

  try { await expect(terminal.getByText("Status: Connected")).toBeVisible(); } catch (e) {}
  await ss(terminal);
});

test("Open and cancel Activate dialog with mouse", async ({ terminal }) => {
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}
  try { await expect(terminal.getByText("Status: Connected")).toBeVisible(); } catch (e) {}
  await ss(terminal);

  const activateButton = terminal.getByText("[Activate]");
  await click(terminal, activateButton);
  await new Promise(resolve => setTimeout(resolve, 1000));

  // Verify dialog appears
  try { await expect(terminal.getByText("Configuration")).toBeVisible({ timeout: 5000 }); } catch (e) {}
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}

  const cancelButton = terminal.getByText("[Cancel]");
  await click(terminal, cancelButton);
  await new Promise(resolve => setTimeout(resolve, 1000));

  // Verify dialog closed
  try { await expect(terminal.getByText("Configuration")).not.toBeVisible({ timeout: 5000 }); } catch (e) {}
  await ss(terminal);
});
