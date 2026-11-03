# 🌸 Sakura Live Hybrid

Minecraft Java 1.21.11 · Fabric client + Windows 11 app.

**Status: Early prototype, NOT released or gameplay-tested.** The repository contains real source code and a GitHub Actions build workflow for both components.

## Components

- `Desktop/` – WPF (.NET 8) dark pink/purple GUI with rounded outer window, Minecraft instance selector, local handshake and macro settings.
- `Fabric/` – Minecraft 1.21.11 Fabric client mod, simple Anchor (G) and Crystal (H) actions, localhost settings API.
- `.github/workflows/build.yml` – builds both components (where dependencies are available).

## Download builds

After a successful GitHub Actions run: **Actions → Build Sakura Live → Artifacts**.

## Important limitations

- The macros are **prototypes**, not the professionally tuned implementation requested.
- **FastPlace is not yet implemented.**
- A successful compile does **not** guarantee compatibility with every modpack or server.
- The desktop app uses loopback networking with the Fabric mod, **not Minecraft memory injection**.
