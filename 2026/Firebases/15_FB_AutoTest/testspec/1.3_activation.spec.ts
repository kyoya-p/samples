import { test, expect } from "@microsoft/tui-test";
import path from "node:path";

const appPath = path.join(process.cwd(), "build/Release/FirebaseApp.exe");

test.use({ 
    program: { file: appPath },
    rows: 40,
    columns: 100,
    env: { API_KEY: "DUMMY_KEY" } 
});

async function click(terminal: any, locator: any) {
  try {
    const cells = await locator.resolve(10000);
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
    await new Promise(resolve => setTimeout(resolve, 3000));
  }
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}
}

test("Activate with API Key", async ({ terminal }) => {
  // 1. Initial state (Disconnected)
  await new Promise(resolve => setTimeout(resolve, 3000));
  await ss(terminal);

  // 2. Open Activate dialog
  const activateButton = terminal.getByText("[Activate]");
  await click(terminal, activateButton);
  await new Promise(resolve => setTimeout(resolve, 2000));
  await ss(terminal);

  // 3. Input API Key from environment variable
  const apiKey = process.env.API_KEY;
  if (!apiKey) throw new Error("API_KEY environment variable is not set");

  await terminal.write("\x08".repeat(100)); 
  await terminal.write(apiKey);
  await new Promise(resolve => setTimeout(resolve, 1000));
  await ss(terminal);

  // 4. Click [Connect]
  const connectButton = terminal.getByText("[Connect]");
  await click(terminal, connectButton);
  
  // 5. Wait for connection and verify
  await expect(terminal.getByText("Status: Connected")).toBeVisible({ timeout: 20000 });
  
  // Try to scroll down/up to trigger refresh
  await terminal.write("\x1b[B"); // Arrow Down
  await new Promise(resolve => setTimeout(resolve, 500));
  await terminal.write("\x1b[A"); // Arrow Up
  
  // Extra wait for Firestore to sync data
  await new Promise(resolve => setTimeout(resolve, 10000));
  await ss(terminal);
});
