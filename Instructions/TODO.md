# tyls.io Development TODO
The game is a pixel-art style multiplayer sandbox with a Java backend and JS/HTML5 frontend.

## 🚀 Recently Completed
### 1. Java WebSocket Server (Replaced WASM) ✅
**Goal:** Centralized server for multiplayer synchronization.
- [x] **WebSocket Server**: Running on port 25567 (Java-WebSocket).
- [x] **Chunk Sync**: Server generates/loads chunks and sends to client as JSON.
- [x] **Block Updates**: `setBlock` broadcasts changes to all players and saves to SQLite.
- [x] **Player Sync**: Real-time X/Y movement interpolation.
- [x] **World Generation**: Perlin noise (Height/Density) with biome support.

### 2. Client Optimization ✅
- [x] **Asset Loading**: Fixed missing textures (Grass, Water, Sand).
- [x] **Performance**: Capped FPS to 60, added TPS display.
- [x] **Input**: Fixed Alt-mode cursor toggling.

## ⏳ Active Tasks (User Requests)
### 1. Fix Broken Features ("Stuff is gone")
- [ ] **Mining/Placing**: Fix inability to mine or place blocks (Critical).
- [ ] **Red Grid**: Recover the red visual grid.
- [ ] **Image Selector**: Fix it using the entire image instead of the tile.
- [ ] **Alt Mode**: 
    - [x] Cursor toggle (Fixed).
    - [ ] Camera Zoom (Needs verification).

### 2. Dual Grid (Auto-Tiling)
- [ ] **Expand Logic**: "if the block isn't itself then act as it is air and if it is then its 1".
- [ ] **New Blocks**: Apply dual-grid format to **Sand** and **Water** (from `TempImage`).
- [ ] **Standard**: "All blocks except for dirt should have the dual grid format".

## 🚨 CRITICAL BUGS (Current Session End)
- [ ] **Visual Corruption**: Render glitches and tearing on mouse movement.
- [ ] **Dual Grid**: Logic implemented for Water/Grass/Sand, but rendering may be incorrect/overlapping.
- [ ] **Dirt Layering**: Implemented TilingSprite background, but needs verification if it solved the black voids without causing new issues.

## 🔮 Future Features
- [ ] **UI System**: Hotbar, Settings, Inventory. Special 9-slice frame with blurred background.
- [ ] **Height System**: Add terrain depth/elevation. 
- [ ] **Physics**: Add inertia to camera movement and player collision.