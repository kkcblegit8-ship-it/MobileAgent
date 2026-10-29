# NexusCode — Local Autonomous Mobile AI Agent & Overlay Workspace

NexusCode brings the power of desktop agentic coding and automation environments (like Cursor, Claude Code, and Antigravity) natively to Android. It combines a real-time Gemini-powered autonomous agent, a local file and code workspace, cross-app automation via Android Intents and Accessibility Service, and a low-latency floating overlay bubble that operates seamlessly on top of any app.

## User Review & Critical Decisions

> [!IMPORTANT]
> Review the confirmed product and architectural decisions below before we proceed to build and provision the application.

- **Confirmed App Automation Engine**: Uses **Accessibility Service + System Intents & Deep Links** to launch installed apps (e.g., YouTube, Browser, File Managers, Social apps), inspect UI hierarchies, and perform automated actions (searching, clicking, typing, uploading/downloading, or sharing content).
- **Confirmed Overlay & Workspace Capabilities**: Includes all four core modules:
  1. **Floating Draggable Bubble** (`SYSTEM_ALERT_WINDOW`) with an expandable mini HUD for quick text prompts, push-to-talk voice chat (`SpeechRecognizer` + `TextToSpeech`), and 1-tap macro triggers over other apps.
  2. **Live Agent Execution Log & Step-by-Step Action Overlay** showing real-time agent thoughts, tool invocations, and cross-app execution progress.
  3. **Local File System & Workspace Code Editor** with syntax-aware editing, file creation/modification, device storage browsing, and instant script/intent execution.
  4. **Scheduled Background Tasks & Automation Macros** for saving and re-running multi-step workflows across apps.
- **Confirmed Visual Identity**: **Dark Cyber-Terminal Aesthetic** inspired by Cursor and Claude Code—deep obsidian surfaces, crisp 1dp terminal borders, neon emerald and cyan status accents, and monospace typography (`JetBrains Mono` + `Space Grotesk`).
- **AI Engine & Provisioning**: Uses **Firebase AI Logic (`gemini-3.8-flash`)** with structured tool-calling schemas and streaming inference, alongside optional custom Gemini API key support via `BuildConfig` for full flexibility.

---

## 1. Overview & Core Concept

- **What It Does**: NexusCode acts as an omnipresent local mobile operator. Inside the main app, users have a full agentic terminal, multi-tab code/file workspace, installed app inspector, and macro scheduler. When switching to other apps (like YouTube, Chrome, or Files), NexusCode transitions into a zero-lag **Floating Agent Bubble** over the screen. Tapping the bubble expands a compact Command HUD where users can speak or type instructions (e.g., *"Open YouTube and search for Kotlin Coroutines tutorials"*, *"Write a Python data parser into my workspace"*, or *"Automate posting this note"*), watch the live step-by-step execution trace, and control the device hands-free.
- **Target Audience / Persona**: Mobile power users, developers, and automation enthusiasts who want an autonomous, low-latency AI operator with full access to their local workspace and installed applications.
- **Key Value**: Eliminates constant app-switching friction by embedding a voice-and-text AI agent directly into an Android system overlay and accessibility automation pipeline.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **System Permissions & Readiness Cockpit**:
   - On launch, a sleek **Permission & Capabilities Status Bar** displays live toggles for:
     - *Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)* — launches system overlay permission settings and starts the Floating Bubble Foreground Service.
     - *Cross-App Accessibility Agent (`AccessibilityService`)* — deep-links to Android Accessibility Settings so the agent can read screen context and perform gestures/clicks in target apps.
     - *Microphone & Voice Engine (`RECORD_AUDIO`)* — enables low-latency voice prompts and spoken agent feedback.
2. **Agent Terminal & Live Execution Stream (Home Tab)**:
   - Users type or speak natural language commands.
   - The Gemini agent responds with a real-time streaming thought trace and structured **Tool Call Cards** (`LAUNCH_APP`, `OPEN_URL_OR_SEARCH`, `WRITE_FILE`, `READ_FILE`, `UI_AUTOMATION_STEP`, `CREATE_MACRO`, `SYSTEM_SHARE`).
   - Each tool call executes immediately (e.g., launching YouTube with a search query or writing code into the local workspace) while updating its status badge (`RUNNING` → `COMPLETED`).
3. **Floating Overlay HUD Over Other Apps**:
   - Activating **Launch Overlay Bubble** spawns a hardware-accelerated draggable cyber-orb over the screen and optionally launches the target app.
   - Tapping the orb expands a compact **Mini Agent HUD** right on top of the active app (e.g., YouTube), featuring:
     - Live status pill showing current target app and Accessibility service state.
     - Quick prompt input bar + **Voice Chat Mic Button** with live waveform animation.
     - Real-time compact step log of what the agent is doing.
     - One-tap quick actions (*Search in App*, *Capture Context*, *Run Macro*, *Minimize to Bubble*).
4. **Local File System & Code Editor (Workspace Tab)**:
   - Full access to the local agent workspace and imported device files via Storage Access Framework.
   - Users and the AI agent can create, edit, format, search, and export code files (`.kt`, `.py`, `.js`, `.json`, `.md`, `.sh`) with line numbers and instant AI refactoring.
5. **Automation Macros & Scheduled Tasks (Automations Tab)**:
   - Users can build, schedule, or let the AI generate multi-step automation pipelines (e.g., *"Open YouTube → Search topic → Log summary to local Markdown file"*) and trigger them with one tap from either the main app or the floating overlay.

### Visual Identity & Theme
- **Aesthetic Direction**: **Dark Cyber-Terminal (Cursor / Claude Code / Antigravity)**—ultra-clean dark engineering cockpit with high-information density, zero visual clutter, and tactile 60fps micro-interactions.
- **Color Palette & Mood**:
  - `--background`: `#090A0F` (Deep Carbon Obsidian)
  - `--surface`: `#11131C` (Terminal Panel Dark)
  - `--surface-variant`: `#191C28` (Elevated Card / Code Block Surface)
  - `--primary`: `#10B981` (Terminal Emerald — active agent & execution pulse)
  - `--secondary`: `#06B6D4` (Cyber Cyan — tool calls, file paths, and overlay HUD accents)
  - `--tertiary`: `#8B5CF6` (Neural Violet — AI reasoning & voice synthesis)
  - `--error`: `#F43F5E` (Crimson Alert)
  - `--border`: `#262A3B` (Crisp 1dp hairline grid borders)
- **Typography & Hierarchy**:
  - **Display & UI Headers**: `Space Grotesk` for sharp technical headers and HUD titles.
  - **Code, Logs & Agent Prompts**: `JetBrains Mono` for authentic IDE/terminal legibility, tool schemas, and execution timestamps.
- **Interactive Feedback & Motion**:
  - Hardware-accelerated spring physics (`spring(dampingRatio = 0.8f, stiffness = 400f)`) when dragging or expanding the floating overlay bubble.
  - Pulsing emerald/cyan status indicators during live Gemini streaming and voice recognition.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: True System Overlay Service (`WindowManager` + In-App Preview Mode)**
  - *Chosen Approach*: Implement a real Android Foreground Service using `WindowManager` (`TYPE_APPLICATION_OVERLAY`) so the floating icon genuinely renders on top of YouTube and other installed apps once `Settings.canDrawOverlays` is granted, paired with an instant in-app interactive overlay preview so the user can also test and customize the floating HUD immediately without leaving the screen.
  - *Why*: Delivers authentic system-level multitasking over third-party apps while ensuring zero-friction testing in any emulator state.
- **Decision 2: Structured Agent Tool-Calling Engine with Real Intent & Accessibility Execution**
  - *Chosen Approach*: Configure Gemini (`gemini-3.8-flash`) with a strict JSON action schema that outputs both conversational reasoning and executable device actions (`LAUNCH_APP`, `YOUTUBE_ACTION`, `WEB_ACTION`, `FILE_WRITE`, `FILE_READ`, `ACCESSIBILITY_CLICK`, `ACCESSIBILITY_TYPE`, `SCHEDULE_MACRO`).
  - *Why*: Instead of a passive text chatbot, every prompt translates into deterministic, low-latency Android OS actions and file mutations.
- **Decision 3: Local-First Persistence with Room + Direct File System Storage**
  - *Chosen Approach*: Store workspace files directly on the device's local storage directory so real files exist on disk, backed by a Room database for indexing agent chat sessions, execution audit logs, and scheduled automation macros.
  - *Why*: Matches the user's explicit requirement for a fast, zero-latency local mobile agent with full control over local files.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NEXUSCODE MOBILE AGENT SYSTEM                        │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────────┐     ┌───────────────────────┐   ┌───────────────────────┐
│   Main Compose    │     │   Floating Overlay    │   │   Cross-App System    │
│     Cockpit       │     │   Service (Window)    │   │   Automation Layer    │
├───────────────────┤     ├───────────────────────┤   ├───────────────────────┤
│ • Agent Terminal  │     │ • Draggable App Orb   │   │ • AccessibilityService│
│ • Code/File IDE   │     │ • Expandable Mini HUD │   │   (Node Tree & Clicks)│
│ • App Launcher    │     │ • Push-to-Talk Voice  │   │ • PackageManager &    │
│ • Macro Scheduler │     │ • Live Step Ticker    │   │   Deep-Link Intents   │
│ • Permission Hub  │     │ • Quick Macro Bar     │   │ • SAF File Access     │
└─────────┬─────────┘     └───────────┬───────────┘   └───────────┬───────────┘
          │                           │                           │
          └───────────────────────────┼───────────────────────────┘
                                      ▼
                    ┌───────────────────────────────────┐
                    │     AgentOrchestrator & State     │
                    │        (Shared Singleton)         │
                    ├───────────────────────────────────┤
                    │ • Gemini AI Logic (3.8-flash)     │
                    │ • Tool Parser & Action Dispatcher │
                    │ • SpeechRecognizer & TTS Engine   │
                    │ • Real-Time Execution StateFlow   │
                    └─────────────────┬─────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
        ┌───────────────────────┐           ┌───────────────────────┐
        │   Local Disk File     │           │   Room SQLite DB      │
        │   Workspace Manager   │           │   (Sessions, Logs,    │
        │   (Read/Write/Exec)   │           │    Macros, Tasks)     │
        └───────────────────────┘           └───────────────────────┘
```

### Data Model & State
- **AgentMessage & ToolAction**: Represents each turn in the terminal or floating HUD, including streaming text, structured tool calls (`actionType`, `targetPackageOrPath`, `payload`, `executionStatus`), and execution latency in milliseconds.
- **WorkspaceFile**: Real local files stored on disk with metadata (`path`, `name`, `extension`, `sizeBytes`, `lastModified`, `content`) editable by both the user and the AI agent.
- **AutomationMacro & ScheduledTask**: Persisted Room entities representing reusable multi-step device automations, trigger conditions (manual, app-launch, or scheduled interval), and last-run status.
- **InstalledAppInfo**: Live device app catalog queried via `PackageManager` (with `<queries>` manifest declarations) so the agent can inspect, search, and launch installed apps on the phone.

### Interactive Component & State Mapping
- **Voice & Text Prompt Submission (Main Terminal & Floating Overlay)**:
  - Tapping the Mic button starts Android `SpeechRecognizer` with live partial transcript streaming into the prompt field; releasing or tapping Send dispatches the prompt to `AgentOrchestrator`.
  - `AgentOrchestrator` calls Gemini (`gemini-3.8-flash`), parses the structured action plan, executes each device/file/app action sequentially, logs every step to the shared `StateFlow`, and optionally speaks the concise completion summary via `TextToSpeech`.
- **Cross-App Overlay Transition**:
  - When the agent executes `LAUNCH_APP` or `YOUTUBE_ACTION` (e.g., opening YouTube or Chrome), it automatically ensures the **Floating Agent Bubble** is visible on screen so the user can immediately tap the floating icon inside the opened app to issue follow-up voice or text commands.
