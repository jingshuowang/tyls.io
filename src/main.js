import './style.css';
import { Game } from './game.js';
import { Network } from './network.js';

// Initialize game
const canvas = document.getElementById('gameCanvas');
console.log('Canvas element:', canvas);

if (!canvas) {
    console.error('❌ Canvas not found! Make sure index.html has <canvas id="gameCanvas">');
} else {
    console.log('✅ Canvas found, initializing game...');
}

const game = new Game(canvas);
console.log('✅ Game object created');

// Network is optional - comment out to play solo
// const network = new Network(game);
// network.connect();

// Set status to Solo Mode
const statusBadge = document.getElementById('onlineStatus');
if (statusBadge) {
    statusBadge.innerHTML = '<span class="status-dot" style="background:#a78bfa;box-shadow:0 0 12px rgba(167,139,250,0.8);"></span> Solo Mode';
    statusBadge.style.borderColor = 'rgba(167, 139, 250, 0.3)';
    statusBadge.style.color = '#c4b5fd';
}

// Start game
game.start();
console.log('✅ Game started!');

// Input handling
document.addEventListener('keydown', (e) => {
    game.keys[e.key] = true;

    // Toggle inventory with E
    if (e.key === 'e' || e.key === 'E') {
        game.inventory.toggle();
    }

    // Prevent default for game keys
    if (['w', 'a', 's', 'd', 'W', 'A', 'S', 'D', 'e', 'E'].includes(e.key)) {
        e.preventDefault();
    }
});

document.addEventListener('keyup', (e) => {
    game.keys[e.key] = false;
});

// Mouse handling
canvas.addEventListener('mousemove', (e) => {
    const rect = canvas.getBoundingClientRect();
    game.mouse.x = e.clientX - rect.left;
    game.mouse.y = e.clientY - rect.top;
});

canvas.addEventListener('mousedown', (e) => {
    if (e.button === 0) { // Left click
        game.mouse.leftButton = true;
    } else if (e.button === 2) { // Right click
        game.mouse.rightButton = true;
        e.preventDefault();
    }
});

canvas.addEventListener('contextmenu', (e) => {
    e.preventDefault(); // Prevent context menu on right click
});

// Focus canvas for keyboard input
canvas.tabIndex = 1;
canvas.focus();

console.log('🎮 Game initialized! Use WASD to move, E for inventory, Left-Click to mine, Right-Click to build');
console.log('Canvas size:', canvas.width, 'x', canvas.height);
console.log('Player position:', game.player.x, game.player.y);

