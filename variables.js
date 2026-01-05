// Game Constants
const TILE_SIZE = 16;
const CHUNK_SIZE = 16;
const SERVER_URL = 'http://localhost:25565';

// Configuration (Default values, overridden by config.json)
let config = {
    playerSpeed: 200,
    defaultZoom: 3.0,
    minZoom: 0.25,
    maxZoom: 10,
    chunkSize: 16, // Redundant but kept for compatibility
    renderDistance: 5,
    lodThreshold: 0.8,
    maxChunksPerFrame: 10
};

// Game State Containers
const chunks = new Map();         // Key: "cx,cy" -> Value: Int8Array[CHUNK_SIZE][CHUNK_SIZE]
const chunkContainers = new Map(); // Key: "cx,cy" -> Value: { dirt: Container, grass: Container }
const dirtyChunks = new Set();    // Chunks that need re-rendering
const pendingChunks = new Set();  // Chunks currently being fetched
let chunksRendered = 0;           // Counter for Render Budget

// Entities & Camera
const camera = { x: 0, y: 0, zoom: 3.0, freelook: false };
const player = { x: 128, y: 128, width: 16, height: 16, speed: 200 };
const mouse = { x: 0, y: 0, left: false, right: false, lastX: undefined, lastY: undefined };

// Interaction State
let isAltPressed = false;
let showGrid = false;
let hoveredTileX = null;
let hoveredTileY = null;
let selectSprite = null;

// Activity Tracking
const keys = {};
let pressedKeys = new Set();

// Texture Caches
const globalTextureCache = new Map();

// PIXI Globals (Initialized in index.html)
let app;
let world;
let dirtLayer;
let grassLayer;
let renderChunk = null;
