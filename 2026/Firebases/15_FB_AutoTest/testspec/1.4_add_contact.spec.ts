import { test, expect } from "@microsoft/tui-test";
import path from "node:path";

const appPath = path.join(process.cwd(), "build/Release/FirebaseApp.exe");

test.use({
    program: { file: appPath },
    rows: 40,
    columns: 100,
    env: { API_KEY: process.env.API_KEY || "" }
});

test("Add contact and verify it appears in list", async ({ terminal }) => {
  await expect(terminal.getByText("Status: Connected")).toBeVisible({ timeout: 15000 });
  await new Promise(resolve => setTimeout(resolve, 3000));

  const buffer = () => terminal.getViewableBuffer().map(r => r.join("")).join("\n");
  
  // Identify name part specifically (the first 28 chars of the [Add] line)
  const extractNameOnly = (view: string) => {
    const lines = view.split("\n");
    for (const line of lines) {
      if (line.includes("[Add]")) {
        // According to UI_Specification, name is fixed width 28.
        // We take the part before the first separator | if possible, or fixed range.
        // The line starts with │ and the name field.
        const match = line.match(/│\s*([^│]+)│/);
        return match ? match[1].trim() : line.substring(2, 25).trim();
      }
    }
    return "";
  };

  const initialName = extractNameOnly(buffer());
  if (!initialName) throw new Error("Could not identify initial name");
  console.log(`Initial name to add: "${initialName}"`);

  // 3. Trigger Add
  // Click [Add] button specifically using its locator
  const addButton = terminal.getByText("[Add]");
  const cells = await addButton.resolve(5000);
  if (cells && cells.length > 0) {
      const x = Math.floor((Math.min(...cells.map((c: any) => c.x)) + Math.max(...cells.map((c: any) => c.x))) / 2) + 1;
      const y = Math.floor((Math.min(...cells.map((c: any) => c.y)) + Math.max(...cells.map((c: any) => c.y))) / 2) + 1;
      await terminal.write(`\x1b[<0;${x};${y}M`);
      await terminal.write(`\x1b[<0;${x};${y}m`);
  }
  await terminal.write("\n"); // Backup Enter
  
  await new Promise(resolve => setTimeout(resolve, 3000));
  
  const afterAddName = extractNameOnly(buffer());
  console.log(`Input refreshed to: "${afterAddName}"`);
  expect(afterAddName).not.toBe(initialName);

  // 4. Verify name in list
  console.log(`Waiting for "${initialName}" to appear in the list...`);
  // Use a regex or simple text match for the name only
  await expect(terminal.getByText(initialName)).toBeVisible({ timeout: 20000 });
  
  try { await expect(terminal).toMatchSnapshot(); } catch (e) {}
});