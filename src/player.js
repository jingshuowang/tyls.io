export class Player {
    constructor(x, y) {
        console.log('Player constructor received:', x, y);
        // Force valid position - fallback to center of world if NaN
        this.x = (typeof x === 'number' && !isNaN(x)) ? x : 3200;
        this.y = (typeof y === 'number' && !isNaN(y)) ? y : 3200;
        this.width = 32;
        this.height = 32;

        // Stats
        this.health = 100;
        this.maxHealth = 100;

        // Movement
        this.speed = 200; // pixels per second
        this.vx = 0;
        this.vy = 0;
        console.log('Player initialized at:', this.x, this.y);
    }

    update(deltaTime, keys, world) {
        // Calculate velocity based on input
        this.vx = 0;
        this.vy = 0;

        if (keys['w'] || keys['W']) this.vy -= 1;
        if (keys['s'] || keys['S']) this.vy += 1;
        if (keys['a'] || keys['A']) this.vx -= 1;
        if (keys['d'] || keys['D']) this.vx += 1;

        // Normalize diagonal movement
        if (this.vx !== 0 && this.vy !== 0) {
            this.vx *= 0.707; // 1/sqrt(2)
            this.vy *= 0.707;
        }

        // Apply speed
        this.vx *= this.speed * deltaTime;
        this.vy *= this.speed * deltaTime;

        // Move with collision detection
        this.moveWithCollision(world);

        // Update health bar
        this.updateHealthBar();
    }

    moveWithCollision(world) {
        // Move X
        const newX = this.x + this.vx;
        if (!this.checkCollision(newX, this.y, world)) {
            this.x = newX;
        }

        // Move Y
        const newY = this.y + this.vy;
        if (!this.checkCollision(this.x, newY, world)) {
            this.y = newY;
        }
    }

    checkCollision(x, y, world) {
        // Check the four corners of the player
        const corners = [
            { x: x - this.width / 2, y: y - this.height / 2 },
            { x: x + this.width / 2, y: y - this.height / 2 },
            { x: x - this.width / 2, y: y + this.height / 2 },
            { x: x + this.width / 2, y: y + this.height / 2 }
        ];

        for (const corner of corners) {
            const tileX = Math.floor(corner.x / world.tileSize);
            const tileY = Math.floor(corner.y / world.tileSize);
            const tile = world.getTile(tileX, tileY);

            if (tile && tile.solid) {
                return true;
            }
        }

        return false;
    }

    updateHealthBar() {
        const healthBar = document.getElementById('healthBar');
        if (healthBar) {
            const percent = (this.health / this.maxHealth) * 100;
            healthBar.style.width = `${percent}%`;
        }
    }

    render(ctx) {
        // Draw player body
        ctx.fillStyle = '#8b5cf6';
        ctx.fillRect(this.x - this.width / 2, this.y - this.height / 2, this.width, this.height);

        // Draw player outline
        ctx.strokeStyle = '#c4b5fd';
        ctx.lineWidth = 2;
        ctx.strokeRect(this.x - this.width / 2, this.y - this.height / 2, this.width, this.height);

        // Draw player face (simple)
        ctx.fillStyle = '#fff';
        ctx.fillRect(this.x - 8, this.y - 8, 4, 4); // Left eye
        ctx.fillRect(this.x + 4, this.y - 8, 4, 4); // Right eye
    }

    takeDamage(amount) {
        this.health = Math.max(0, this.health - amount);
        this.updateHealthBar();
    }

    heal(amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
        this.updateHealthBar();
    }
}
