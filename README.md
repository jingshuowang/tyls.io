✅ scrolliing in alt mode is not centered.
✅ start to add grass tiles. we can start by adding a visual grid. which shows as a white line when g is pressed VERY THIN. the images in grass.png is aligned to this grid. image selector code starts from 0. code is: l;ets make this make sense 
# tactiq.io free youtube transcript
# Draw fewer tiles - by using a Dual-Grid system!
# https://www.youtube.com/watch/jEWFSv3ivTg

00:02:26.920 So, what is the dual-grid system? Well, like it’s name suggests, it uses two grids instead of one.   
00:02:34.080 The world grid keeps track of which tile is where, but doesn’t display anything. The second grid is offset from the world grid by half a tile, and each tile changes based on it’s 4 overlapping   
00:02:39.760 neighbours. This means there is only 16 possible configurations of neighbours, instead of 256.
00:02:55.240 In terms of the actual tileset to use with this method, this time we actually *want* the edges to be drawn halfway through each tile. Once the offset is applied, it will appear perfectly aligned with the rest of the game’s world.
00:03:08.080 For learning more about the dual-grid system, I recommend checking out this talk by Oskar Stalberg or this devlog by ThinMatrix, both of which I will link in the description.
00:03:18.120 So, the dual-grid system sounds great and all, but how exactly can we implement it   
00:03:22.320 in a game engine like Godot? Well, today i’d like to go through how I did it for my games.
00:03:28.200 The two grids I mentioned before are implemented as two tilemaps, with this display Tilemap being   
00:03:32.880 offset by half a tile. The rest of the logic is handled with this custom tilemap script.   
00:03:40.560 This big chunk at the start is the 16 different neighbour configurations and their corresponding   
00:03:44.760 tiles. The tiles are represented as vectors here because they refer to atlas coordinates.
00:03:52.120 So back in Godot, you can edit these placeholder tiles like   
00:03:54.880 normal and then the offset tiles will generate over them once you hit play.   
00:03:59.800 You can of course update the tiles at runtime by calling this function here.
00:04:06.160 One of the trickier parts of writing the script was figuring out which coordinates to use for   
00:04:10.160 the neighbour tiles. To visualise this, lets label all the coordinates of our tilemap and   
00:04:15.360 then offset one of them. If we wanna update the tile at 0, 0 and refer to the offset neighbours,   
00:04:22.160 then we can see they are located at these positions here. This results in coordinates of (0,   
00:04:25.880 0), (1, 0), (0, 1), and (1, 1) relative to the tile you want to change.
00:04:32.080 But if you are on the offset grid and want to refer to the neighbours on the world grid,   
00:04:36.080 then it would be the negative version of those coordinates instead. This   
00:04:40.440 difference took me a while to wrap my head around, but in terms of the   
00:04:43.320 actual code it just meant adding a minus sign instead of a plus in some places.
00:04:49.560 This demo project is available on GitHub if you want to check it out,   
00:04:52.800 and I also made a version for Unity too.
00:04:55.760 When I implemented this in my games I also had to add a bit of extra logic for randomisation   
00:05:00.880 and animations. But I think the effort was particularly worth it for this water,   
00:05:06.000 because I wanted an 11-frame animation for the shoreline. Thanks to the dual grid system   
00:05:11.200 I got away with drawing 155 tiles instead of the 507 I would have otherwise needed.
00:05:18.280 Dual-grid aside, for those who will continue to use regular autotiling, one thing I can  
00:05:23.000 definitely recommend is at least using a tool like this one to help generate the full set of tiles.  
00:05:27.960 I personally wish I started doing this a lot earlier as it would have saved me so much time.
00:05:32.760 Before I end the video, I just wanted to provide a quick update on the status of my games. I have  
00:05:37.800 pretty much finished porting Interscape from Unity to Godot. It took me about 6 months,  
00:05:42.920 but considering I only worked on it in my free time and I had to learn  
00:05:46.200 a new engine I’m pretty happy with this progress. Plus it gave me the  
00:05:50.160 chance to improve a lot of the assets and code along the way.
00:05:55.880 However, I recently got a new idea for a game that I’m quite excited about - a  
00:05:59.880 simple game about exploring, collecting and selling houseplants. What you are seeing here  
00:06:04.960 is the first few months of progress. I’ve decided to prioritise it over Interscape  
00:06:09.640 for now since I think the scope will be much smaller, but we’ll see how things go.
00:06:15.160 Alright thats all from me today, thanks for watching!

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
 1extra: ✅ lets fix the tiles. the tiles for grass should load in white grid, wait WHERE IS THE WHITE VISUAL GRID, ADD THE THIN WHITE GRID BACK. ok yeah thats it for 1extra.
 2. ✅ add highlight for blocks that are being hovered over. and when mosue pressed go to image 1 when not image 0. for spriotesheet
3. add a crossbar in the center of the screen4
4. use my special cursor :D.
5. ✅ block not allighned to white grid ;-; + white grid lines not rendering at all
6. later unlaoded chunks will be fog and would be loaded when player gets close to it. unlaoded ones arew kept in worldfil, just not rendered.
7. enchance block placement/obliteration. it isnt connected now and iosnt smooth. it should be reallu smooth like last timne when we smoothened it it connects the 2 points that are loaded if it detects a part not loaded. so it should not be separated like more conneted and firm. same for destroying. 
8. grid have rendering issues. unstable.
9. blocks are blurry, which is BAD >:
10. unstable, half blocks apeear
11. optimising, render can be cached but world grid and accual data can not like block palce ment and obliteration. and block loading, porbably the reason for half blocks. when a block is placed near the chunk border it does that 
12. the image for grass.png isnt on the white line
13. add pixel.ttf font to activity indicator and other UI elements
14. breaking blocks causes severe lag
15. grid rendering causes aw snap lag
16. render center not centered at camera but at player location which is bad 
17. chunks not rendering in Alt mode freelook (black areas when panning) when scroll big enought
18. add heights (terrain elevation/depth)
19. add special frame system for UI (inventory, settings, etc)
    - Command: /ui [length] [height] [location] creates bordered frame
    - frame.png spritesheet with corners (pixelated) and sides (horizontal/vertical)
    - Inside blurred, corners don't change size
    - Perfect for inventory/settings UI laoding bar and more 
    - Game style: blur + lighting + pixels + shaders = unique!
21. to be continued delete unnessisary stuf in code code celan. take 10 minutes make stuf tidy and great files folders and code. HOLY LAG OH MY GOd optimise