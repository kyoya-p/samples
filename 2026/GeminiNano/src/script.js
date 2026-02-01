/**
 * Copyright 2024 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

import { marked } from "https://cdn.jsdelivr.net/npm/marked@13.0.3/lib/marked.esm.js";
import DOMPurify from "https://cdn.jsdelivr.net/npm/dompurify@3.1.6/dist/purify.es.mjs";

const SYSTEM_PROMPT = "You are a helpful and friendly assistant.";

(async () => {
  const errorMessage = document.getElementById("error-message");
  const promptInput = document.getElementById("prompt-input");
  const responseArea = document.getElementById("response-area");
  const form = document.querySelector("form");
  const submitButton = document.getElementById("submit-button");

  // Keep references to hidden inputs if they exist, or use defaults
  const sessionTemperature = document.getElementById("session-temperature") || { value: 1 };
  const sessionTopK = document.getElementById("session-top-k") || { value: 3 };

  let session = null;

  // Check for Prompt API support
  if (!('LanguageModel' in self)) {
    errorMessage.style.display = "block";
    errorMessage.innerHTML = `
      Your browser doesn't support the Prompt API. 
      If you're on Chrome, ensure you have enabled the flags:
      chrome://flags/#optimization-guide-on-device-model
      chrome://flags/#prompt-api-for-gemini-nano-multimodal-input
    `;
    return;
  }

  // Auto-resize textarea
  promptInput.addEventListener('input', function() {
    this.style.height = 'auto';
    this.style.height = (this.scrollHeight) + 'px';
    if (this.value === '') {
        this.style.height = '24px'; // Reset to initial height
    }
  });

  const scrollToBottom = () => {
    responseArea.scrollTop = responseArea.scrollHeight;
  };

  const promptModel = async () => {
    const prompt = promptInput.value.trim();
    if (!prompt) return;

    // Reset input
    promptInput.value = "";
    promptInput.style.height = '24px';

    // Add User Message
    const userDiv = document.createElement("div");
    userDiv.classList.add("prompt", "speech-bubble");
    userDiv.textContent = prompt;
    responseArea.append(userDiv);
    scrollToBottom();

    // Add AI Placeholder
    const aiDiv = document.createElement("div");
    aiDiv.classList.add("response", "speech-bubble");
    const p = document.createElement("p");
    p.textContent = "Thinking...";
    aiDiv.append(p);
    responseArea.append(aiDiv);
    scrollToBottom();

    try {
      if (!session) {
        await updateSession();
      }
      
      // Clone session if needed to manage context window, but for now simple prompting
      const stream = await session.promptStreaming(prompt);

      let result = '';
      let previousChunk = '';
      
      for await (const chunk of stream) {
        const newChunk = chunk.startsWith(previousChunk)
            ? chunk.slice(previousChunk.length) : chunk;
        result += newChunk;
        
        // Render Markdown
        p.innerHTML = DOMPurify.sanitize(marked.parse(result));
        previousChunk = chunk;
        scrollToBottom();
      }
    } catch (error) {
      p.textContent = `Error: ${error.message}`;
      console.error(error);
      
      // If session is destroyed or invalid, try to reset
      if (error.message.includes("session")) {
          session = null;
      }
    }
  };

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    await promptModel();
  });

  promptInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      form.dispatchEvent(new Event("submit"));
    }
  });

  promptInput.addEventListener("focus", () => {
    // promptInput.select(); // Don't select all text on focus for chat apps
  });

  const updateSession = async () => {
    if (self.LanguageModel) {
      try {
        // Check availability first
        const availability = await LanguageModel.availability();
        console.log("Model availability:", availability);

        if (availability === 'no') {
          errorMessage.textContent = "Gemini Nano is not available on this device. Please check your browser settings and hardware requirements.";
          errorMessage.style.display = "block";
          return;
        }

        // If 'after-download', the model will be downloaded upon creation.
        // We can optionally show a loading state here.
        if (availability === 'after-download') {
           console.log("Model needs to be downloaded.");
        }

        const params = await LanguageModel.params();
        session = await LanguageModel.create({
          temperature: Number(sessionTemperature.value) || params.defaultTemperature,
          topK: Number(sessionTopK.value) || params.defaultTopK,
          initialPrompts: [
            {
              role: 'system',
              content: SYSTEM_PROMPT,
            }
          ],
        });
        
        // Hide error message if successful
        errorMessage.style.display = "none";

      } catch (e) {
        console.error("Failed to create session:", e);
        
        // Improve error message for common "service not running" issue
        let msg = "Failed to create AI session: " + e.message;
        if (e.message.includes("service is not running")) {
            msg = "Gemini Nano service is not running. Please restart Chrome or check chrome://flags.";
        }
        
        errorMessage.textContent = msg;
        errorMessage.style.display = "block";
      }
    }
  };

  // Initialize session on load
  if (!session) {
    await updateSession();
  }
})();