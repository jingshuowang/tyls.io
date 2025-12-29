✅ scrolliing in alt mode is not centered.
✅ start to add grass tiles. we can start by adding a visual grid. which shows as a white line when g is pressed VERY THIN. the images in grass.png is aligned to this grid. image selector code starts from 0. code is: l;ets make this make sense 
  1. The Void (0000)
   * Condition: TL:E, TR:E, BL:E, BR:E
   * Visual: Completely empty. No grass.
   * Instruction: If (TL==E && TR==E && BL==E && BR==E) use Image ID: [_12__]

  2. Inner Corner (Sharp) - Top Left (1000)
   * Condition: TL:F, TR:E, BL:E, BR:E
   * Visual: A sharp corner of grass pointing towards the bottom-right. (Grass only in top-left quadrant).
   * Instruction: If (TL==F && TR==E && BL==E && BR==E) use Image ID: [_15__]

  3. Inner Corner (Sharp) - Top Right (0100)
   * Condition: TL:E, TR:F, BL:E, BR:E
   * Visual: A sharp corner of grass pointing towards the bottom-left. (Grass only in top-right quadrant).
   * Instruction: If (TL==E && TR==F && BL==E && BR==E) use Image ID: [_8__]

  4. Inner Corner (Sharp) - Bottom Left (0010)
   * Condition: TL:E, TR:E, BL:F, BR:E
   * Visual: A sharp corner of grass pointing towards the top-right. (Grass only in bottom-left quadrant).
   * Instruction: If (TL==E && TR==E && BL==F && BR==E) use Image ID: [__0_]

  5. Inner Corner (Sharp) - Bottom Right (0001)
   * Condition: TL:E, TR:E, BL:E, BR:F
   * Visual: A sharp corner of grass pointing towards the top-left. (Grass only in bottom-right quadrant).
   * Instruction: If (TL==E && TR==E && BL==E && BR==F) use Image ID: [__13_]

  6. Horizontal Edge - Ceiling (1100)
   * Condition: TL:F, TR:F, BL:E, BR:E
   * Visual: The top half is grass. The bottom edge is flat (or slightly rough) grass.
   * Instruction: If (TL==F && TR==F && BL==E && BR==E) use Image ID: [__9_]

  7. Horizontal Edge - Floor (0011)
   * Condition: TL:E, TR:E, BL:F, BR:F
   * Visual: The bottom half is grass. The top edge is flat grass.
   * Instruction: If (TL==E && TR==E && BL==F && BR==F) use Image ID: [_3__]

  8. Vertical Edge - Right Face (1010)
   * Condition: TL:F, TR:E, BL:F, BR:E
   * Visual: The left half is grass. The right edge is a vertical grass wall.
   * Instruction: If (TL==F && TR==E && BL==F && BR==E) use Image ID: [__11_]

  9. Vertical Edge - Left Face (0101)
   * Condition: TL:E, TR:F, BL:E, BR:F
   * Visual: The right half is grass. The left edge is a vertical grass wall.
   * Instruction: If (TL==E && TR==F && BL==E && BR==F) use Image ID: [__1_]

  10. Diagonal - Forward Slash (0110)
   * Condition: TL:E, TR:F, BL:F, BR:E
   * Visual: Two sharp corners touching or a thin diagonal line of grass connecting Top-Right to Bottom-Left.
   * Instruction: If (TL==E && TR==F && BL==F && BR==E) use Image ID: [_14__]

  11. Diagonal - Back Slash (1001)
   * Condition: TL:F, TR:E, BL:E, BR:F
   * Visual: Two sharp corners touching or a thin diagonal line of grass connecting Top-Left to Bottom-Right.
   * Instruction: If (TL==F && TR==E && BL==E && BR==F) use Image ID: [_4__]

  12. Outer Corner (Rounded/Inverted) - Bottom Right (1110)
   * Condition: TL:F, TR:F, BL:F, BR:E
   * Visual: Mostly solid grass, but the Bottom-Right corner is cut out (empty).
   * Instruction: If (TL==F && TR==F && BL==F && BR==E) use Image ID: [_7__]

  13. Outer Corner (Rounded/Inverted) - Bottom Left (1101)
   * Condition: TL:F, TR:F, BL:E, BR:F
   * Visual: Mostly solid grass, but the Bottom-Left corner is cut out (empty).
   * Instruction: If (TL==F && TR==F && BL==E && BR==F) use Image ID: [10]

  14. Outer Corner (Rounded/Inverted) - Top Right (1011)
   * Condition: TL:F, TR:E, BL:F, BR:F
   * Visual: Mostly solid grass, but the Top-Right corner is cut out (empty).
   * Instruction: If (TL==F && TR==E && BL==F && BR==F) use Image ID: [2]

  15. Outer Corner (Rounded/Inverted) - Top Left (0111)
   * Condition: TL:E, TR:F, BL:F, BR:F
   * Visual: Mostly solid grass, but the Top-Left corner is cut out (empty).
   * Instruction: If (TL==E && TR==F && BL==F && BR==F) use Image ID: [5]

  16. Full Solid (1111)
   * Condition: TL:F, TR:F, BL:F, BR:F
   * Visual: Completely filled with grass (Center of a landmass).
   * Instruction: If (TL==F && TR==F && BL==F && BR==F) use Image ID: [6] 

 1. ✅ add alt mode and fix scrolling in that too. like dragging stuff around.
 1extra: lets fix the tiles. the tiles for grass should load in white grid, wait WHERE IS THE WHITE VISUAL GRID, ADD THE THIN WHITE GRID BACK. ok yeah thats it for 1extra.
 2. add highlight for blocks that are being hovered over. and when mosue pressed go to image 1 when not image 0. for spriotesheet
3. add a crossbar in the center of the screen4
4. use my special cursor :D.
5. block not allighned to white grid ;-;
6. later unlaoded chunks will be fog and would be loaded when player gets close to it. unlaoded ones arew kept in worldfil, just not rendered.
7. enchance block placement/obliteration. it isnt connected now and iosnt smooth. it should be reallu smooth like last timne when we smoothened it it connects the 2 points that are loaded if it detects a part not loaded. so it should not be separated like more conneted and firm. same for destroying. 
8. grid have rendering issues. unstable.
9. blocks are blurry, which is BAD >:
10. unstable, half blocks apeear
11. optimising, render can be cached but world grid and accual data can not like block palce ment and obliteration. and block loading, porbably the reason for half blocks. 
12. the image for grass.png isnt on the white line
13. add pixel.ttf font to activity indicator and other UI elements
14. breaking blocks causes severe lag
15. grid rendering causes aw snap lag
16. white grid lines not rendering at all
17. render center not centered at 0.25x zoom (offset issue)
18. chunks not rendering in Alt mode freelook (black areas when panning)
19. add heights (terrain elevation/depth)