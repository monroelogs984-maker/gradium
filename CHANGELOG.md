# Changelog

## 2.0.1 (Fabric 1.21.1)
First Fabric release, same behaviour as Gradium 2.0.1 for NeoForge:
- Autosaves of POI data, world data (SavedData) and players are spread across ticks instead of all at once, scaled by server TPS.
- Shutdown and world unload always save everything immediately, so nothing is lost when you close the world.
- Config: `config/gradium.properties` (`minSavesPerTick`, `maxSavesPerTick`). Requires Fabric API.
