# TXT Texture Manager — Fabric 1.21.11 (prototype)

Early prototype, **not yet verified in Minecraft**. Right Shift opens a basic texture selection screen. It reads existing ZIP or folder resource packs, allows choosing PNG texture files and exports `resourcepacks/TXT-Mix.zip`. Activate the resulting pack in Minecraft's Resource Packs screen.

## Build
Requires JDK 21 and Gradle 8.14+. Run `gradle build` from this directory. See GitHub Actions artifact after a successful workflow. Fabric API must also be installed in Minecraft.

## Current limitations
No live preview, search, profiles, animations, or automatic pack activation. May require fixes to compile for Minecraft 1.21.11. The generated `pack.mcmeta` pack format is provisional.

This is a separate development branch; the original repository's default branch is not modified.
