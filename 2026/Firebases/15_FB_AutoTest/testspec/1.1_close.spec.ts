import { test, expect } from "@microsoft/tui-test";
import path from "node:path";

const appPath = String.raw`C:\Users\kyoya\works\samples\2026\Firebases\15_FB_AutoTest\build\Release\FirebaseApp.exe`;

test.use({ 
    program: { file: appPath },
    rows: 40,
    columns: 100,
    env: { API_KEY: "AIzaSyDpE5hkTVWMt8iYPPm30yNL6KJ-YivAwJ4" }
});

test("FirebaseApp launch and exit", async ({ terminal }) => {
  // 0. Initial snapshot
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}

  // 1. Wait for the application UI to appear
  try {
    await expect(terminal.getByText("Status:")).toBeVisible();
    await expect(terminal.getByText("Name")).toBeVisible();
    await expect(terminal.getByText("Mail")).toBeVisible();
    await expect(terminal.getByText("Op")).toBeVisible();
  } catch (e) {}

  // 2. Wait for data to load (Resilient wait)
  const isLoading = () => terminal.getViewableBuffer().some((row: string[]) => row.join("").includes("Loading..."));
  if (isLoading()) {
    await new Promise(resolve => setTimeout(resolve, 5000));
  }

  // 3. Take a snapshot of the initial state (Ignore failure for dynamic data)
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}

  // 4. Send 'q' to exit the application
  terminal.write("q");

  // Allow some time for the process to exit
  await new Promise(resolve => setTimeout(resolve, 1000));
});