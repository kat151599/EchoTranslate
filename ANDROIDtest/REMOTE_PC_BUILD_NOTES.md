# Remote PC Android build notes

This variant intentionally does **not** bundle llama.cpp or any local LLM native runtime.

- No `third_party/llama.cpp` checkout is required.
- No CMake/NDK/Vulkan build is required for the `llama-android` module.
- The module is retained only as a lightweight API compatibility shim so the upstream UI/source tree compiles unchanged.
- Selecting a legacy on-device LLM engine will fail with an explicit "Remote PC build" message. Use the **PC server** pipeline instead.
- Screen capture, crop, frame deduplication, network upload, translated-block response handling, and overlay rendering remain in the Android app.
- OCR, context/history management, token budgeting, and LLM translation belong on the Windows server.
