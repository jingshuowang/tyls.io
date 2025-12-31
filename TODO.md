# tyls.io Development TODO
the whole game is a pixel game with some lighting effects. would be between a 3d game and a 2d game.
## 🐛 Known Issues
- [ ] **Mouse Update Lag**: Game elements (highlight, placed blocks) don't seem to update visually unless the mouse is moved. (Suspect render/input loop issue).
- [ ] **Mining Lag**: Breaking/placng blocks causes stutter due to full chunk re-rendering. SAME FOR GRID RENDERING
- [x] **Visual Glitches**: Tile transitions at chunk boundaries can appear cut off or misaligned. half and quarter tiles. (Fixed via Layering system)
- [x] **pixijs problem**:  the blocks are rendering reallyl burry like the pixels arent being rendered as pixels. (Fixed via Nearest scaling)

## 🔮 Future Features
- [ ] **Custom Cursor**: Add custom cursor. and crosshair in the center of the screen.
- [ ] **Smooth Interaction**: Better dragging/continuous mining.
- [ ] **UI System**: Hotbar, Settings, Inventory. its a special frame. you enter a hieght width and location ect. and then it renders a frame with the inside of the frame blurred and the outside having a special frame. the corners of the frame should not change size. the image that would be givin would contain the corners and the side of the frame. the inside of the frame would be a blur effect. 
- [ ] **Height System**: Add terrain depth/elevation.
- [ ] **Performance**: Optimize chunk updates (partial updates instead of full rebuild).
- add a mini zoom in normal mode that zooms like a spring like a maginifying glass but only zooms once its like the magnifying glass in mincraft but can also zoom out by 1. accessed by scroll wheel.
