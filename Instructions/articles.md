
This is why I think the dual-grid system is so neat,  
00:02:22.920
because it pretty much solves all these problems.
00:02:26.920
So, what is the dual-grid system? Well, like it’s name suggests, it uses two grids instead of one.  
00:02:34.080
The world grid keeps track of which tile is where, but doesn’t display anything. The second grid is  
00:02:39.760
offset from the world grid by half a tile, and each tile changes based on it’s 4 overlapping  
00:02:44.960
neighbours. This means there is only 16 possible configurations of neighbours, instead of 256.
00:02:55.240
In terms of the actual tileset to use with this method, this time we actually *want*  
00:02:59.240
the edges to be drawn halfway through each tile. Once the offset is applied,  
00:03:03.840
it will appear perfectly aligned with the rest of the game’s world.
00:03:08.080
For learning more about the dual-grid system, I recommend checking out this talk by Oskar  
00:03:12.120
Stalberg or this devlog by ThinMatrix, both of which I will link in the description.
00:03:18.120
So, the dual-grid system sounds great and all, but how exactly can we implement it  
00:03:22.320
in a game engine like Godot? Well, today i’d like to go through how I did it for my games.
00:03:28.200
The two grids I mentioned before are implemented as two tilemaps, with this display Tilemap being  
00:03:32.880
offset by half a tile. The rest of the logic is handled with this custom tilemap script.  
00:03:40.560
This big chunk at the start is the 16 different neighbour configurations and their corresponding  
00:03:44.760
tiles. The tiles are represented as vectors here because they refer to atlas coordinates.
00:03:52.120
So back in Godot, you can edit these placeholder tiles like  
00:03:54.880
normal and then the offset tiles will generate over them once you hit play.  
00:03:59.800
You can of course update the tiles at runtime by calling this function here.
00:04:06.160
One of the trickier parts of writing the script was figuring out which coordinates to use for  
00:04:10.160
the neighbour tiles. To visualise this, lets label all the coordinates of our tilemap and  
00:04:15.360
then offset one of them. If we wanna update the tile at 0, 0 and refer to the offset neighbours,  
00:04:22.160
then we can see they are located at these positions here. This results in coordinates of (0,  
00:04:25.880
0), (1, 0), (0, 1), and (1, 1) relative to the tile you want to change.
00:04:32.080
But if you are on the offset grid and want to refer to the neighbours on the world grid,  
00:04:36.080
then it would be the negative version of those coordinates instead. This  
00:04:40.440
difference took me a while to wrap my head around, but in terms of the  
00:04:43.320
actual code it just meant adding a minus sign instead of a plus in some places.
00:04:49.560
This demo project is available on GitHub if you want to check it out,  
00:04:52.800
and I also made a version for Unity too.
00:04:55.760
When I implemented this in my games I also had to add a bit of extra logic for randomisation  
00:05:00.880
and animations. But I think the effort was particularly worth it for this water,  
00:05:06.000
because I wanted an 11-frame animation for the shoreline. Thanks to the dual grid system  
00:05:11.200
I got away with drawing 155 tiles instead of the 507 I would have otherwise needed.
00:05:18.280
Dual-grid aside, for those who will continue to use regular autotiling, one thing I can  
00:05:23.000
definitely recommend is at least using a tool like this one to help generate the full set of tiles.  
00:05:27.960
I personally wish I started doing this a lot earlier as it would have saved me so much time.
00:05:32.760
Before I end the video, I just wanted to provide a quick update on the status of my games. I have  
00:05:37.800
pretty much finished porting Interscape from Unity to Godot. It took me about 6 months,  
00:05:42.920
but considering I only worked on it in my free time and I had to learn  
00:05:46.200
a new engine I’m pretty happy with this progress. Plus it gave me the  
00:05:50.160
chance to improve a lot of the assets and code along the way.
00:05:55.880
However, I recently got a new idea for a game that I’m quite excited about - a  
00:05:59.880
simple game about exploring, collecting and selling houseplants. What you are seeing here  
00:06:04.960
is the first few months of progress. I’ve decided to prioritise it over Interscape  
00:06:09.640
for now since I think the scope will be much smaller, but we’ll see how things go.
00:06:15.160
Alright thats all from me today, thanks for watching!
For our game this is the code. For image selector it starts from top left and goes right until the edge of the image then it goes to the left of the second row, repoeate until bottom right and all of them are numebred. 
dual grid key:
image 0 (TL:0, TR:0, BL:1, BR:0)
image 1 (TL:0, TR:1, BL:0, BR:1)
image 2 (TL:1, TR:0, BL:1, BR:1) 
image 3 (TL:0, TR:0, BL:1, BR:1) 
image 4 (TL:1, TR:0, BL:0, BR:1)
image 5 (TL:0, TR:1, BL:1, BR:1)
image 6 (TL:1, TR:1, BL:1, BR:1)
image 7 (TL:1, TR:1, BL:1, BR:0)
image 8 (TL:0, TR:1, BL:0, BR:0) layered by dirt could be problem
image 9 (TL:1, TR:1, BL:0, BR:0)
image 10 (TL:1, TR:1, BL:0, BR:1)
image 11 (TL:1, TR:0, BL:1, BR:0)
image 12 (TL:0, TR:0, BL:0, BR:0)
image 13 (TL:0, TR:0, BL:0, BR:1) 
image 14 (TL:0, TR:1, BL:1, BR:0)
image 15 (TL:1, TR:0, BL:0, BR:0)
this plaies for eveyrthing except for dirt which u in dirt select randomly for now just dont load it since dirt isnt done yet
the 0 means non self tiles neighbors and 1 means self tile neibors. 