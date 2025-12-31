# 🌲 无限体素世界 (Infinite Voxel World) - 设计文档

## 1. 项目愿景 (Project Vision)
打造一个基于 Web 的高性能无限 2D 沙盒游戏。核心特色是极其流畅的无限世界生成、独特的"双网格 (Dual-Grid)" 视觉风格，以及极简的交互体验。

---

## 2. 核心技术架构 (Core Architecture)

### 🛠️ 技术选型 (Tech Stack)
*   **渲染引擎**: **PixiJS v8**
    *   **为什么选择它? (Why?)**: 它是目前 Web 端最快的 2D 渲染引擎之一。
    *   **批处理 (Batch Rendering)**: 能够自动将成千上万个相同的方块（如草地）合并为一次 GPU 绘制调用 (Draw Call)，这对无限体素游戏至关重要。
    *   **WebGL 2 / WebGPU**: v8 版本底层重构，支持更现代的图形标准，性能比传统 Canvas 或 v7 版本更强。

### 🧱 无限区块系统 (Infinite Chunk System)
*   **分块加载**: 世界被划分为 `16x16` 的区块 (Chunks)。
*   **动态生成**: 随着玩家移动，周围的区块会自动生成。
*   **LOD (细节层次)**: 
    *   近处渲染高质量纹理。
    *   远处渲染简化的大色块，保证极高的渲染距离 (Render Distance) 而不卡顿。
*   **性能优化**: 使用 "时间切片 (Time Slicing)" 技术分批生成区块，避免掉帧。

### 🎨 双网格渲染系统 (Dual-Grid Rendering)
这是本项目最独特的视觉核心 (参考 README 中的 Auto-Tiling 算法)。
*   **原理**: 
    *   **逻辑网格 (Data Grid)**: 存储实际的方块数据 (0=土, 1=草)。
    *   **视觉网格 (Visual Grid)**: 渲染时偏移 `0.5` 个单位。
*   **效果**: 草地边缘不再是生硬的方块，而是根据周围 4 个邻居的状态，从 16 种形态中自动选择一种 (如圆角、斜坡)，实现平滑自然的过渡。
*   **实现**: `GRASS_MAP` 映射表将 `0000`-`1111` 的邻居状态映射到具体的 Sprite ID。

---

## 3. 交互设计 (Interaction)

### 🎮 操作控制
*   **移动**: `WASD` 或 方向键。
*   **视角**: 
    *   `Alt` 键切换 "自由观察模式" (Freelook)。
    *   滚轮缩放 (Zoom)。
*   **编辑**:
    *   🖱️ **左键**: 破坏方块 (Break)。
    *   🖱️ **右键**: 放置方块 (Place)。
    *   光标会自动跟随鼠标 (使用 `select.png`)。

---

## 4. 进度与路线图 (Roadmap)

### ✅ 已完成 (Completed)
*   [x] 核心引擎搭建 (PixiJS v8)。
*   [x] 双网格渲染 (Dual-Grid) 实现。
*   [x] 无限地形生成 (Perlin Noise)。
*   [x] 渲染性能优化 (LOD, Culling)。
*   [x] 自定义光标与交互修复。

### 🚧 待开发 (In Progress / Planned)
1.  **平滑建造 (Smooth Interaction)**: 鼠标拖动时的连续放置/破坏优化。
2.  **UI 系统**: 物品栏 (Hotbar), 设置菜单，特殊的 "模糊+像素" 风格 UI。
3.  **高度系统 (Heights)**: 引入高低差，增加地形深度。
4.  **代码重构**: 整理目前杂乱的文件结构。

---

## 5. 文件结构 (File Structure)
*   `index.html`: 游戏主入口，包含所有核心逻辑 (目前)。
*   `Assets/`: 存放图片 (`grass.png`, `select.png`) 和字体。
*   `README.md`: 包含详细的技术参考 (如 Dual-Grid 算法详解)。
