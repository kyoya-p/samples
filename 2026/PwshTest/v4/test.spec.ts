import { test, expect } from "@microsoft/tui-test";
import fs from 'node:fs';

test.use({ program: { file: "pwsh.exe" } });

test("check $PID in pwsh", async ({ terminal }) => {
  // Wait for the prompt
  await expect(terminal.getByText("PS")).toBeVisible();

  // Wait for prompt and submit $PID
  await terminal.submit("$PID");
  
  // Give it a moment to output
  await new Promise(resolve => setTimeout(resolve, 1000));

  const output = (await terminal.serialize()).view;
  fs.writeFileSync('pid_output.txt', output);

  // Verify output contains a number (the PID)
  // We'll look for something like "1234" in the output
  await expect(terminal.getByText(/\d+/)).toBeVisible();
});
