import { test, expect } from "@microsoft/tui-test";
import path from "node:path";

const appPath = String.raw`C:\Users\kyoya\works\samples\2026\Firebases\15_FB_AutoTest\build\Release\FirebaseApp.exe`;

test.use({ 
    program: { file: appPath },
    rows: 30,
    columns: 100,
    env: { API_KEY: "dummy" }
});

test("FirebaseApp launch and exit", async ({ terminal }) => {
  // 1. Wait for the application UI to appear
  // Header shows "Mail" instead of "Mail Address" in the actual build
  await expect(terminal.getByText("Status:")).toBeVisible();
  await expect(terminal.getByText("Name")).toBeVisible();
  await expect(terminal.getByText("Mail")).toBeVisible();
  await expect(terminal.getByText("Op")).toBeVisible();

  // 2. Take a snapshot of the initial state
  await expect(terminal).toMatchSnapshot();

  // 3. Send 'q' to exit the application
  terminal.write("q");

  // Allow some time for the process to exit
  await new Promise(resolve => setTimeout(resolve, 1000));
});
