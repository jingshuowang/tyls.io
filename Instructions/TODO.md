# tyls.io Development TODO
the whole game is a pixel game with some lighting effects. would be between a 3d game and a 2d game.

## 🚀 Immediate Plans (Upon Return)

### 1. WebAssembly Integration (sql.js) ✅
**Goal:** Run the game database LOCALLY in the browser. Zero loading times on movement.
- [x] **Download sql.js**: Added via CDN in `index.html`.
- [x] **Load `world.db` Locally**: Fetch logic implemented in `window.load` + IndexedDB persistence.
- [x] **Remove Network Chunking**: `getChunk` now queries WASM DB synchronously.
- [x] **Direct Queries**: frontend queries SQLite directly using WASM.
- [x] **Persistence**: `saveWorld` updates in-memory DB and syncs to IndexedDB `tyls_db`.

### 2. WebSocket Multiplayer (Phase 2) ⏳
**Goal:** Real-time player interaction.
- [ ] **Setup Socket Server**: Initialize Socket.IO or WS on the Java server (or separate Node process).
- [ ] **Player Sync**: Broadcast X/Y coordinates to all connected clients.
- [ ] **World Sync**: When a block changes, broadcast the update to everyone immediately.

## 🐛 Known Issues
- [x] **Visual Glitches**: Fix tile transitions at chunk boundaries (Fixed 1.0 - Improved Neighbor Updates & Layering).
- [x] **Auto Save**: Replaced manual button with background auto-save (Fixed 1.0).
- [x] **Diagonal Black Pattern**: Fixed by synchronous `getChunk` calls in `getTileSafe` (WASM).

## 🔮 Future Features
- [ ] **UI System**: Hotbar, Settings, Inventory. Special 9-slice frame with blurred background.
- [ ] **Height System**: Add terrain depth/elevation. 
- [ ] **Physics**: Add inertia to camera movement and player collision.