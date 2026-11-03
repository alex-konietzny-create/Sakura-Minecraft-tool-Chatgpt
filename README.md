# 🌸 Sakura Live — Pink Fabric Client

**Minecraft Java 1.21.11 · Fabric Loader · Java 21**

Only a Fabric mod. No desktop EXE or process connector required.

## Install

1. Open [GitHub Actions](../../actions/workflows/build.yml) and choose the latest **green** run.
2. Download artifact **SakuraLive-Pink-Fabric-1.21.11**, unzip it and install `SakuraLive-Fabric-0.1.0.jar` into Minecraft's `mods` folder.
3. Use **Minecraft 1.21.11**, Fabric Loader and Fabric API.
4. Press **Right Shift** for the pink, rounded in-game menu; **G** for Anchor and **H** for Crystal.

## Features

- Pink + near-black native Minecraft interface with rounded corners and clickable pill controls.
- Responsive layout for smaller GUI sizes (automatic scaling).
- Enable/disable anchor macro, crystal macro and FastPlace.
- Choose inventory slots, action delay and FastPlace delay.
- Save changes to `config/sakura-live.json` automatically.
- Final slot for Anchor macro (Totem slot).
- In-game GUI; no external EXE and no network connection.

**Important:** The macro logic is an experimental implementation, not a clone of a commercial client. CI compilation confirms code compiles but cannot confirm that every PvP interaction works on a particular server. Test safely in a private practice world.

The repository still contains old unused `Desktop/` sources as historical prototype files, but the workflow builds **only** the Fabric mod.
