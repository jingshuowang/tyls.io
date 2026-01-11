# tyls.io Development TODO
the whole game is a pixel game with some lighting effects. would be between a 3d game and a 2d game.
## 🐛 Known Issues
- [ ] **Mouse Update Lag**: Game elements (highlight, placed blocks) don't seem to update visually unless the mouse is moved. (Suspect render/input loop issue).
- [ ] **Mining Lag**: Breaking/placng blocks causes stutter due to full chunk re-rendering. SAME FOR GRID RENDERING
- [ ] **Visual Glitches**: Tile transitions at chunk boundaries can appear cut off or misaligned (Recurring at borders?).
- [] **Visual Glitches (Fixed 1.0)**: Fixed initial cutoff via Layering.
- [x] **pixijs problem**:  the blocks are rendering reallyl burry like the pixels arent being rendered as pixels. (Fixed via Nearest scaling)

## 🔮 Future Features
- [x] **Custom Cursor**: Add custom cursor. and crosshair in the center of the screen. (Implemented)
- [ ] **UI System**: Hotbar, Settings, Inventory. Special 9-slice frame with blurred background.
- [x] **Smooth Interaction**: Better dragging/continuous mining. (Implemented via Bresenham's)
- [ ] **UI System**: Hotbar, Settings, Inventory. Special frame with blurred background.
- [ ] **Height System**: Add terrain depth/elevation.
- [x] **Performance**: Optimize chunk updates (Partial updates to fix Mining Lag).
- add a mini zoom in normal mode that zooms like a spring like a maginifying glass but only zooms once its like the magnifying glass in mincraft but can also zoom out by 1. accessed by scroll wheel.
-later we have cool visuals like shaders and stuf i ccould fill the epty/ boring parts of a block with redscreen and load flowers and stuf on it later
make render distance much more no black area when in alt mode. make world infinite and pregenrate more(like a miliion :D)