TF Storage (Minecraft Forge 1.20.1)
================================

A storage mod featuring TF Bags and TF Chests with modular TF Units (storage cards).
Supports auto-pickup and restock behaviors, plus optional Curios integration.

Features
--------
- TF Bags with selectable TF Units and multiple tiers
- TF Chests with swappable TF Units and per-tier capacity
- Auto-pickup modes (matching/all) and restock mode
- Sort and quick-move actions in bag/chest GUI
- Optional Curios slot support

Project Structure
-----------------
- `src/main/java` - mod source code
- `src/main/resources` - assets and data
- `build.gradle` and `gradle.properties` - build config

Build
-----
Windows:
1. Run `gradlew.bat build`
2. Output jar: `build/libs/`

Development
-----------
- Java 17 is required
- Forge: ${minecraft_version}-${forge_version}

Notes
-----
- Curios is optional. The mod will run without it.
- This project is based on the original TF Storage mod (1.12.2) and updated for 1.20.1.
