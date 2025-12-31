# tyls.io

A Minecraft-inspired 2D survival game built with PixiJS.

## Features
- **Infinite World Generation**: Seamless chunk loading using Perlin noise.
- **Dual-Grid Rendering**: Smooth terrain transitions (grass edges) using a logic/visual grid offset system.
- **LOD System**: High performance rendering with specialized low-detail view for distant chunks.
- **Mining & Building**: Left click to break blocks, right click to place blocks.
- **Freelook Mode**: Hold Alt to look around without moving.

## Controls
- **WASD**: Move
- **Alt**: Freelook
- **Scroll**: Zoom In/Out
- **Left Click**: Mine (Dirt)
- **Right Click**: Place (Grass)

## Tech Stack
- **Engine**: PixiJS v8
- **Language**: Vanilla JavaScript / HTML5

## Known Issues
- **Mining Lag**: Breaking or placing blocks may cause a momentary stutter. This is because the entire chunk (and potentially neighbors) needs to be re-rendered to update the terrain transitions. This is a known optimization target for future updates.
- **Visual Glitches**: Some tile transitions at chunk boundaries might appear cut off or misaligned in certain browser resolutions and tiles images are cut off by anchoring.
- **Game Updates**: Game elements (highlight, placed blocks) don't seem to update visually unless the mouse is moved. (Note: Fixed in recent patch).
