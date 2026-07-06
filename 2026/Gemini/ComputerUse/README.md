# JetBrains Koog - Computer Use Demo (Gemini)

This project demonstrates a functional implementation of **Computer Use (GUI Automation)** in pure Kotlin using **JetBrains Koog** and Google's **Gemini** multimodal models.

## Overview

[JetBrains Koog](https://github.com/JetBrains/koog) is an open-source, pure-Kotlin agentic AI framework. It allows building stateful, multi-platform, and graph-based workflow agents that can execute on the JVM, Android, iOS, and Web.

This demo uses Koog's **Prompt DSL** and Google AI's Gemini integration (`prompt-executor-google-client`) to automate OS tasks on Windows.

### How It Works (Feedback Loop)

```
       +---------------------------------------------+
       |                                             |
       v                                             |
[Capture Screenshot]                                 |
       |                                             |
       v                                             |
[Multimodal Prompt (Koog DSL)]                       |
       |                                             |
       v                                             |
[Gemini Vision Model Analysis]                       |
       |                                             |
       v                                             |
[Output Next Action (JSON)]                          |
       |                                             |
       v                                             |
[OS Control Execution (java.awt.Robot)] ------------+
```

1. **Capture Screenshot**: Captures the current Windows screen to a PNG file using `java.awt.Robot`.
2. **Multimodal Prompt**: Builds a prompt containing the target goal and the screenshot attachment using Koog's `prompt` builder DSL.
3. **Gemini Execution**: Sends the prompt to a Gemini model (e.g. Gemini 2.5 Flash / Pro) using Koog's `simpleGoogleAIExecutor`.
4. **Action Parsing**: Extracts and parses the JSON action specified by Gemini.
5. **Execution**: Performs the mouse move, click, double click, right click, type, press key, or wait action via `java.awt.Robot`.
6. **Iterate**: Loops back, captures the new screen, and proceeds to the next step until Gemini outputs `"action": "complete"`.

## Project Structure

```
ComputerUse/
├── build.gradle.kts       # Dependencies: koog-agents and prompt-executor-google-client
├── settings.gradle.kts
└── src/
    └── main/
        └── kotlin/
            └── ai/koog/samples/
                └── ComputerUseDemo.kt  # Main Loop & Robot automation logic
```

## Requirements

- **Java JDK 21+**
- A Google Gemini API key (from Google AI Studio or Vertex AI)

## Getting Started

### 1. Set Environment Variables

Set your Gemini API key in your terminal session:

**PowerShell:**
```powershell
$env:GEMINI_API_KEY="your_api_key_here"
```

**CMD:**
```cmd
set GEMINI_API_KEY=your_api_key_here
```

### 2. Run the Demo

Run the application using Gradle. You can optionally provide the goal as arguments:

```bash
./gradlew run --args="Launch search, search for notepad, and open it"
```

If no arguments are provided, the default goal is: `"Launch search, search for notepad, and open it."`

## Supported Actions

The model can output the following structured actions:

- `move`: Moves the mouse cursor to `x` and `y`.
- `click`: Performs a left click at `x` and `y`.
- `double_click`: Performs a double left click.
- `right_click`: Performs a right click.
- `type`: Types a string of `text` on the keyboard.
- `press_key`: Presses a special `key` (ENTER, ESC, TAB, BACKSPACE, UP, DOWN, LEFT, RIGHT).
- `wait`: Pauses execution for `seconds` to allow the OS UI to load.
- `complete`: Finalizes the loop when the target goal is successfully met.
