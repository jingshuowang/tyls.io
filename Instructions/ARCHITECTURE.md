# 项目架构文档 (Project Architecture)

## 📁 项目结构 (Project Structure)

```
New Game/
├── 📂 Assets/                    # 游戏资源
│   ├── Font/                    # 自定义字体 (pixel.ttf)
│   ├── Image/                   # 图像资源
│   │   ├── Tile/               # 地砖贴图 (dirt.png, grass.png, water.png, sand.png)
│   │   ├── cursor.png          # 自定义鼠标光标
│   │   ├── crosshair.png       # 准星 (已废弃，使用HTML跟随)
│   │   ├── frame.png           # UI框架
│   │   └── select.png          # 选择高亮
│   └── Sound/                   # 音频资源
│
├── 📂 Frontend/                  # 前端 (浏览器客户端)
│   ├── index.html              # 主游戏页面 (PixiJS + WebSocket)
│   ├── tweaker.html            # 配置调整工具
│   └── ui.html                 # UI测试页面
│
├── 📂 Server/                    # 后端 (Java服务器)
│   ├── Main.java               # 服务器主程序 (WebSocket Server)
│   └── Main.class              # 编译后的字节码
│
├── 📂 Instructions/              # 项目文档
│   ├── README.md               # 项目说明
│   ├── TODO.md                 # 待办事项
│   ├── ui.md                   # UI设计说明
│   └── ARCHITECTURE.md         # 架构文档 (本文件)
│
├── 📂 lib/                       # 依赖库
│   ├── Java-WebSocket-1.5.4.jar # WebSocket支持
│   ├── slf4j-api-1.7.36.jar    # 日志接口
│   ├── slf4j-simple-1.7.36.jar # 简单日志实现
│   └── sqlite-jdbc-3.42.0.0.jar # SQLite数据库驱动
│
├── config.json                  # 游戏配置参数
├── start.bat                    # 服务器启动脚本
└── world.db                    # 世界数据存储 (SQLite)
```

---

## 🖥️ 服务器主要功能 (Server Main Functions)

### 技术栈
- **语言**: Java
- **数据库**: SQLite (WAL模式)
- **协议**: HTTP + WebSocket (RFC 6455)
- **端口**: 8001 (HTTP静态文件) / 8002 (WebSocket游戏通信)
- **依赖管理**: 手动管理 (`lib/` 目录)

### 核心功能

#### 1. 🌍 世界生成 (World Generation)
```
┌─────────────────────────────────────────────┐
│  Perlin Noise 世界生成器                      │
├─────────────────────────────────────────────┤
│  • 世界大小: 1024×1024 chunks (~100万个区块)    │
│  • 区块大小: 16×16 tiles                      │
│  • 生成方式: 按需生成 (Lazy Generation) + 预生成 │
│  • 算法: 双层Perlin噪声 (Height + Density)    │
│  • 安全区: 中心2 chunk半径内为草地 (Grid Safe)  │
└─────────────────────────────────────────────┘
```

#### 2. 📦 Chunk数据管理
- **存储**: SQLite数据库 (`world.db`)
- **表结构**: `chunks (id TEXT PRIMARY KEY, data TEXT)`
- **格式**: `id="x,y"`, `data="[1,0,2...]"` (JSON Array String)
- **持久化**: 
    - 启动时预生成/检查世界
    - 运行时按需读取/生成
    - 更新时批量写入 (Batch implementation pending/manual)

#### 3. 🌐 HTTP静态文件服务器 (Port 8001)
- **实现**: JDK内置 `com.sun.net.httpserver.HttpServer`
- **功能**: 提供前端HTML/JS/CSS/图片等静态资源
- **安全**: 阻止访问 `/Server/`、`/lib/`、`/.git` 路径
- **默认路由**: `/` → `/Frontend/index.html`
- **CORS**: 允许跨域请求 (开发便利)

#### 4. 🌐 WebSocket游戏服务器 (Port 8002)
| 消息类型 (Type) | 方向 | 格式示例 | 功能 |
|---|---|---|---|
| `getChunk` | C->S | `{"type":"getChunk", "key":"0,0"}` | 请求指定Chunk数据 |
| `chunk` | S->C | `{"type":"chunk", "key":"0,0", "data":"..."}` | 返回Chunk数据 |
| `setBlock` | C->S | `{"type":"setBlock", "x":10, "y":20, "val":1}` | 修改方块 (广播+存储) |
| `pos` | C->S | `{"type":"pos", ...}` | 玩家位置同步 (暂未完全实装) |
| `save` | C->S | `{"type":"save", "player":{...}, "chunks":[...]}` | 保存玩家数据和修改的区块 |

#### 5. 🔒 安全特性
- 防止SQL注入 (使用PreparedStatement)
- HTTP路径遍历防护 (阻止`..`)
- 源代码/依赖目录访问阻断 (403)

---

## 🎮 前端主要功能 (Frontend Main Functions)

### 技术栈
- **引擎**: PixiJS v8
- **语言**: Vanilla JavaScript
- **渲染**: WebGL (GPU加速)
- **通信**: WebSocket

### 核心功能

#### 1. 🗺️ 渲染系统
```
┌──────────────────────────────────────────┐
│  图层架构 (Z-Sorting)                     │
├──────────────────────────────────────────┤
│  Layer 0: Dirt Layer (底层 - 泥土/水/沙)   │
│  Layer 1: Grass Layer (顶层 - 草地)       │
│  Layer 2: Overlays (选择框, 指示器)        │
└──────────────────────────────────────────┘
```

#### 2. 🧩 Chunk管理
- **渲染距离**: 可配置 (config: 32 chunks)
- **LOD系统**: 缩放 < 0.8 时使用平均色块渲染 (Minimap风格)
- **垃圾回收**: 增量GC (每帧20个), 距离 > (RenderDist + 5) 时卸载
- **异步加载**: WebSocket回调驱动，无阻塞

#### 3. 🖱️ 交互系统
- **输入**: WASD移动, 鼠标操作
- **Freelook**: Alt键切换自由视角 (拖拽平移, 滚轮缩放)
- **建造/挖掘**: 
    - 左键 (LMB): 放置主要方块/挖掘
    - 右键 (RMB): 放置次要方块
    - Bresenham算法: 支持快速拖动时的线性插值建造

---

## 🔄 前后端交互 (Frontend-Backend Interaction)

### 通信架构
```
                        ┌──────────────────────────────┐
                        │     Main.java (单进程)         │
┌───────────────┐       ├───────────┬──────────────────┤
│               │  HTTP │ HttpServer│ WebSocketServer  │
│   Browser     │◄─────►│ Port:8001 │ Port:8002        │
│  (PixiJS v8)  │  WS   │ 静态文件   │ 游戏逻辑          │
│               │◄─────►│           │                  │
└───────────────┘       └───────────┴──────────────────┘
```

### 交互流程

#### 1. 🚀 启动流程
```
1. 运行 start.sh (或 start.bat)
2. Java编译并启动 → HTTP(8001) + WebSocket(8002) 就绪
3. 浏览器自动打开 http://localhost:8001/Frontend/index.html
4. 初始化 PixiJS Application
5. 动态计算WS端口 (HTTP端口+1) → 连接 ws://localhost:8002
6. socket.onopen -> 隐藏Loading层 -> startGameLoop()
7. 游戏循环开始，根据摄像机位置请求可视范围内的Chunks
```

#### 2. 📡 Chunk加载流程
```
前端 (Main Thread)                    服务器
       │                                │
       │ ──SEND {"getChunk"}──────────► │
       │                                │ 1. 查询DB / 生成Perlin
       │                                │ 2. 构造JSON响应
       │ ◄──ONMESSAGE {"chunk"}─────────│
       │                                │
       ▼                                ▼
   解析数据 -> 渲染Chunk
```

---

## 📝 配置参数 (config.json)

```json
{
  "playerSpeed": 300,        // 玩家移动速度
  "defaultZoom": 2,          // 默认缩放
  "minZoom": 0.1,            // 最小缩放
  "maxZoom": 50,             // 最大缩放
  "chunkSize": 16,           // Chunk大小 (tiles)
  "renderDistance": 32,      // 渲染距离 (chunks)
  "lodThreshold": 0.8,       // LOD切换阈值
  "maxChunksPerFrame": 20    // 每帧最大加载数
}
```

---

## 🏗️ 架构总结

### 优点
- ✅ **实时性**: WebSocket支持低延迟交互
- ✅ **性能**: PixiJS v8 + LOD + 增量GC
- ✅ **简单性**: 单一端口，无复杂的Web Server配置
- ✅ **持久化**: SQLite本地存储

### 待优化 (TODO)
- **二进制传输**: 目前使用JSON字符串传输Chunk，效率较低
- **增量更新**: 每次SetBlock直接广播，未做节流或合并
- **服务端校验**: 目前服务端信任客户端的所有坐标输入
