# tyls.io

A 2D survival game built with PixiJS.

## Features
- **Infinite World Generation**: Seamless chunk loading using Perlin noise.
- **Dual-Grid Rendering**: Smooth terrain transitions (grass edges) using a logic/visual grid offset system.
- **LOD System**: High performance rendering with specialized low-detail view for distant chunks.
- **Mining & Building**: Left click to break blocks, right click to place blocks.
- **Freelook Mode**: Toggle alt to look around without moving(dragging and zoomming).

## Controls
- **WASD**: Move
- **Alt**: Freelook
- **Scroll**: Zoom In/Out
- **Left Click**: Mine (Dirt)
- **Right Click**: Place (Grass)

## Tech Stack
- **Engine**: PixiJS v8
- **Language**: Vanilla JavaScript / HTML5

## Future Vision: tyls.io
The goal is to create a seamless IO game accessible via:
1.  **Browser (tyls.io)**: Instant play, no download. Ideal for school/quick play.
2.  **Desktop (Window)**: Downloadable version (C# Wrapper / Electron) for better performance and dedicated features.

### Architecture Note for Future
-   **Server**: The Java backend will run on a **Cloud Server**.
-   **Client**: Users trigger a "popup" or navigate to the URL to connect to this remote server.
-   **Activation**: Users won't run `.bat` files; the server runs 24/7 in the cloud. They just "Activate" by visiting the link.


### Feature Roadmap: Graphics & Immersion
**Motion Blur (Per-Pixel Smearing)**
To hide low FPS and enhance speed (like Spider-Man), we plan to implement **Per-Pixel Motion Blur** using velocity buffers.
-   **Why**: Hides stuttering, immersive speed sense.
-   **Technique**: Per-object buffering (better than full-screen "muddy" blur).
-   **Preference**: User-controllable (On/Off).
