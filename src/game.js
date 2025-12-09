import { Player } from './player.js';
import { World } from './world.js';
import { Inventory } from './inventory.js';

export class Game {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.width = 1200;
    this.height = 700;
    this.canvas.width = this.width;
    this.canvas.height = this.height;

    // Game state
    this.running = false;
    this.lastTime = 0;

    // Initialize systems
    this.world = new World();
    console.log('After World creation - spawnX:', this.world.spawnX, 'spawnY:', this.world.spawnY);

    // Hardcode spawn position to fix NaN issue
    const spawnX = 3200;
    const spawnY = 3200;
    this.player = new Player(spawnX, spawnY);
    console.log('After Player creation - player.x:', this.player.x, 'player.y:', this.player.y);
    this.inventory = new Inventory();

    // Camera
    this.camera = {
      x: 0,
      y: 0
    };

    // Input state
    this.keys = {};
    this.mouse = {
      x: 0,
      y: 0,
      worldX: 0,
      worldY: 0,
      leftButton: false,
      rightButton: false
    };

    // Other players (for multiplayer)
    this.otherPlayers = new Map();
  }

  start() {
    this.running = true;
    this.lastTime = performance.now();
    this.gameLoop();
  }

  gameLoop = (currentTime) => {
    if (!this.running) return;

    const deltaTime = (currentTime - this.lastTime) / 1000;
    this.lastTime = currentTime;

    this.update(deltaTime);
    this.render();

    requestAnimationFrame(this.gameLoop);
  };

  update(deltaTime) {
    // Update player
    this.player.update(deltaTime, this.keys, this.world);

    // Update camera to follow player
    this.updateCamera();

    // Handle mouse interactions
    this.handleMouseInput();

    // Update inventory display
    this.inventory.updateUI();
  }

  updateCamera() {
    // Center camera on player
    this.camera.x = this.player.x - this.width / 2;
    this.camera.y = this.player.y - this.height / 2;
  }

  handleMouseInput() {
    // Convert screen coordinates to world coordinates
    this.mouse.worldX = this.mouse.x + this.camera.x;
    this.mouse.worldY = this.mouse.y + this.camera.y;

    // Left click - mine/attack
    if (this.mouse.leftButton) {
      const tileX = Math.floor(this.mouse.worldX / this.world.tileSize);
      const tileY = Math.floor(this.mouse.worldY / this.world.tileSize);

      // Check if player is close enough
      const dist = Math.hypot(this.player.x - this.mouse.worldX, this.player.y - this.mouse.worldY);
      if (dist < 100) {
        const resource = this.world.mineTile(tileX, tileY);
        if (resource) {
          this.inventory.addItem(resource.type, resource.amount);
        }
      }

      this.mouse.leftButton = false; // Single action per click
    }

    // Right click - build/place
    if (this.mouse.rightButton) {
      const tileX = Math.floor(this.mouse.worldX / this.world.tileSize);
      const tileY = Math.floor(this.mouse.worldY / this.world.tileSize);

      const dist = Math.hypot(this.player.x - this.mouse.worldX, this.player.y - this.mouse.worldY);
      if (dist < 100) {
        // Try to place a wall (if player has wood)
        if (this.inventory.hasItem('wood', 2)) {
          if (this.world.placeTile(tileX, tileY, 'wall')) {
            this.inventory.removeItem('wood', 2);
          }
        }
      }

      this.mouse.rightButton = false;
    }
  }

  render() {
    // Fix NaN position if it somehow happened
    if (isNaN(this.player.x)) this.player.x = 3200;
    if (isNaN(this.player.y)) this.player.y = 3200;

    // Clear canvas
    this.ctx.fillStyle = '#1a1f3a';
    this.ctx.fillRect(0, 0, this.width, this.height);

    // Draw "GAME IS RUNNING" test text
    this.ctx.fillStyle = '#00ff00';
    this.ctx.font = '24px Arial';
    this.ctx.fillText('GAME IS RUNNING!', this.width / 2 - 100, 30);

    // Save context state
    this.ctx.save();
    this.ctx.translate(-this.camera.x, -this.camera.y);

    // Render world
    this.world.render(this.ctx, this.camera, this.width, this.height);

    // Render other players
    this.otherPlayers.forEach(otherPlayer => {
      this.ctx.fillStyle = '#fbbf24';
      this.ctx.fillRect(otherPlayer.x - 16, otherPlayer.y - 16, 32, 32);

      // Draw player name
      this.ctx.fillStyle = '#fff';
      this.ctx.font = '12px Inter';
      this.ctx.textAlign = 'center';
      this.ctx.fillText(otherPlayer.name || 'Player', otherPlayer.x, otherPlayer.y - 25);
    });

    // Render player
    this.player.render(this.ctx);

    // Restore context state
    this.ctx.restore();

    // Render debug info
    this.renderDebugInfo();
  }

  renderDebugInfo() {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    this.ctx.fillRect(10, this.height - 60, 200, 50);

    this.ctx.fillStyle = '#fff';
    this.ctx.font = '12px monospace';
    this.ctx.fillText(`Position: ${Math.floor(this.player.x)}, ${Math.floor(this.player.y)}`, 15, this.height - 40);
    this.ctx.fillText(`Players: ${this.otherPlayers.size + 1}`, 15, this.height - 20);
  }

  // Network methods (called from network.js)
  updateOtherPlayer(id, playerData) {
    this.otherPlayers.set(id, playerData);
  }

  removeOtherPlayer(id) {
    this.otherPlayers.delete(id);
  }

  getPlayerState() {
    return {
      x: this.player.x,
      y: this.player.y,
      health: this.player.health
    };
  }
}
