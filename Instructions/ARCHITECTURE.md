# 项目架构文档 (Project Architecture)

## 📁 项目结构 (Project Structure)

```
New Game/
├── 📂 Assets/                    # 游戏资源
│   ├── Font/                    # 自定义字体 (pixel.ttf)
│   ├── Image/                   # 图像资源
│   │   ├── Tile/               # 地砖贴图 (dirt.png, grass.png)
│   │   ├── cursor.png          # 自定义鼠标光标
│   │   ├── crosshair.png       # 准星
│   │   ├── frame.png           # UI框架
│   │   └── select.png          # 选择高亮
│   └── Sound/                   # 音频资源
│
├── 📂 Frontend/                  # 前端 (浏览器客户端)
│   ├── index.html              # 主游戏页面 (PixiJS)
│   ├── tweaker.html            # 配置调整工具
│   └── ui.html                 # UI测试页面
│
├── 📂 Server/                    # 后端 (Java服务器)
│   ├── Main.java               # 服务器主程序
│   └── Main.class              # 编译后的字节码
│
├── 📂 Instructions/              # 项目文档
│   ├── README.md               # 项目说明
│   ├── TODO.md                 # 待办事项
│   ├── ui.md                   # UI设计说明
│   └── ARCHITECTURE.md         # 架构文档 (本文件)
│
├── config.json                  # 游戏配置参数
├── server.bat                   # 服务器启动脚本
├── sqlite-jdbc-3.42.0.0.jar    # SQLite数据库驱动
└── world.db                    # 世界数据存储 (SQLite)
```

---

## 🖥️ 服务器主要功能 (Server Main Functions)

### 技术栈
- **语言**: Java
- **数据库**: SQLite (WAL模式)
- **协议**: HTTP/1.1
- **端口**: 25565

### 核心功能

#### 1. 🌍 世界生成 (World Generation)
```
┌─────────────────────────────────────────────┐
│  Perlin Noise 世界生成器                      │
├─────────────────────────────────────────────┤
│  • 世界大小: 1024×1024 chunks (~100万个区块)    │
│  • 区块大小: 16×16 tiles                      │
│  • 生成方式: 同步阻塞 (服务器启动时完成)          │
│  • 算法: 双层Perlin噪声 (0.05 + 0.01 scale)   │
│  • 安全区: 中心2 chunk半径内为空地              │
└─────────────────────────────────────────────┘
```

#### 2. 📦 Chunk数据管理
- **存储**: SQLite数据库 (`world.db`)
- **格式**: `id TEXT` (如 "0,0") + `data TEXT` (如 "1010\n0101...")
- **批处理**: 每10,000个chunk执行一次批量写入
- **缓存策略**: 按需查询，无服务端缓存

#### 3. 🌐 HTTP服务器
| 端点 | 方法 | 功能 |
|------|------|------|
| `/chunk?x=&y=` | GET | 获取指定chunk数据 |
| `/status` | GET | 服务器状态检测 |
| `/*` | GET | 静态文件服务 |
| `OPTIONS` | ALL | CORS预检请求 |

#### 4. 🔒 安全特性
- CORS支持 (跨域资源共享)
- 路径遍历防护 (`..` 检测)
- 连接超时处理

---

## 🎮 前端主要功能 (Frontend Main Functions)

### 技术栈
- **引擎**: PixiJS v8
- **语言**: Vanilla JavaScript
- **渲染**: WebGL (GPU加速)

### 核心功能

#### 1. 🗺️ 渲染系统
```
┌──────────────────────────────────────────┐
│  双层渲染架构 (Z-Sorting)                  │
├──────────────────────────────────────────┤
│  Layer 0: Dirt Layer (底层 - 泥土)         │
│  Layer 1: Grass Layer (顶层 - 草地边缘)    │
│  Layer 2: Overlays (调试网格)              │
│  Layer 3: Select Sprite (选择框)           │
└──────────────────────────────────────────┘
```

#### 2. 🧩 Chunk管理
- **渲染距离**: 可配置 (默认16 chunks)
- **LOD系统**: 缩放<0.8时简化渲染
- **垃圾回收**: 增量GC (20 chunks/帧)
- **时间分片**: 每帧最多渲染10个新chunk

#### 3. 🎨 自动贴图系统 (Autotiling)
```
Dual-Grid Marching Squares算法
┌─────────────────────────────────────────┐
│  检测4角邻居 (TL, TR, BL, BR)              │
│  生成16种边缘变体                          │
│  示例: "1100" → 天花板边缘 (索引9)          │
└─────────────────────────────────────────┘
```

#### 4. 📷 相机系统
| 模式 | 描述 |
|------|------|
| **Normal** | 相机跟随玩家 |
| **Freelook** | 拖拽平移、滚轮缩放 |

---

## 🔄 前后端交互 (Frontend-Backend Interaction)

### 通信架构
```
┌─────────────────────┐     HTTP/REST      ┌─────────────────────┐
│                     │ ◄────────────────► │                     │
│   Frontend          │                    │   Backend (Java)    │
│   (Browser/PixiJS)  │                    │   Port: 25565       │
│                     │                    │                     │
└─────────────────────┘                    └─────────────────────┘
         │                                           │
         │  ┌────────────────────┐                   │
         │  │   Web Worker       │                   │
         └──│   (Chunk Fetcher)  │───────────────────┘
            └────────────────────┘
```

### 交互流程

#### 1. 🚀 启动流程
```
1. 用户访问 Frontend/index.html
2. 前端显示 "Connecting..." 加载屏
3. 轮询 /status 端点等待服务器就绪
4. 服务器返回 {"status":"Online"}
5. 前端随机生成玩家坐标
6. 启动 Chunk Fetcher Worker
7. 按圆形模式请求初始chunks
8. 淡入游戏界面
```

#### 2. 📡 Chunk加载流程
```
前端 (Main Thread)              Worker                    服务器
       │                          │                          │
       │ ──fetchChunkViaWorker──► │                          │
       │                          │ ───GET /chunk?x=&y=───► │
       │                          │                          │
       │                          │ ◄─── JSON "1010\n..."───│
       │                          │                          │
       │ ◄──── chunkData事件 ─────│                          │
       │                          │                          │
       ▼                          ▼                          ▼
   渲染chunk + 更新邻居
```

#### 3. 📊 数据格式
| 方向 | 格式 | 示例 |
|------|------|------|
| Request | URL参数 | `/chunk?x=5&y=-3` |
| Response | JSON字符串 | `"1010…\n0101…"` |
| 解析后 | 2D数组 | `[[1,0,1,0],[0,1,0,1],...]` |

### 关键API端点

| 端点 | 用途 | 响应 |
|------|------|------|
| `GET /status` | 服务器存活检测 | `{"status":"Online"}` |
| `GET /chunk?x=N&y=M` | 获取世界chunk | `"1010\n0101..."` |
| `GET /Frontend/*` | 静态资源 | HTML/JS/CSS |
| `GET /Assets/*` | 游戏资源 | 图片/字体 |

---

## 📝 配置参数 (config.json)

```json
{
  "playerSpeed": 300,        // 玩家移动速度 (像素/秒)
  "defaultZoom": 2,          // 默认缩放级别
  "minZoom": 0.1,            // 最小缩放
  "maxZoom": 50,             // 最大缩放
  "chunkSize": 16,           // Chunk大小 (tiles)
  "renderDistance": 16,      // 渲染距离 (chunks)
  "lodThreshold": 0.8,       // LOD切换阈值
  "maxChunksPerFrame": 10    // 每帧最大渲染chunk数
}
```

---

## 🏗️ 架构总结

### 优点
- ✅ **模块化**: 前后端完全分离
- ✅ **高性能**: WebGL渲染 + Web Worker异步加载
- ✅ **无限世界**: 动态chunk加载/卸载
- ✅ **持久化**: SQLite存储世界数据

### 技术特点
| 组件 | 技术选择 |
|------|----------|
| 服务器 | 原生Java Socket |
| 数据库 | SQLite + WAL |
| 渲染 | PixiJS v8 (WebGL2) |
| 网络 | HTTP REST + Web Worker |
| 地形 | Perlin Noise + Autotile |

### 未来扩展方向
- 🔮 云服务器部署 (tyls.io)
- 🔮 多人联机支持
- 🔮 桌面客户端 (Electron)
- 🔮 运动模糊特效
