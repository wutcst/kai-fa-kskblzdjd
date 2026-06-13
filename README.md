# 勇者传说 — 多人联机2D图形游戏

基于 "World of Zuul" 样例工程扩展开发的多人联机 2D 图形游戏。支持玩家注册登录、实时对战、装备系统、击杀排行榜、GM 调试面板等功能。

## 功能特性

### v1.0 — 单人游戏
- 多物品房间系统（描述/重量/负重上限）
- back 命令（逐层回退到起点）
- 传送门房间（随机传送）
- Player 类（属性/背包/装备栏）
- take/drop/items 命令
- 魔法饼干（永久增加负重）

### v2.0 — 联机游戏
- 玩家注册/登录 + JWT 鉴权
- WebSocket 实时通信（心跳/断线重连）
- PVP 对战系统（攻击/防御/生命值）
- 17 件装备物品（武器/防具/消耗品/被动）
- 观察者模式事件系统（受伤/死亡/击杀）
- 游戏状态持久化（H2/MySQL）

### v3.0 — 图形界面
- Vue3 + Canvas 2D 瓦片地图引擎
- WASD 移动、J 攻击、空格拾取
- 实时击杀排行榜
- 10 分钟沙盘重置
- GM 调试面板
- 帮助面板（操作说明/战斗机制/装备图鉴）
- 全中文界面

### v4.0 — 系统优化
- 防御公式优化：max(max(3, ATK×20%), ATK-DEF)
- 死亡复活流程重构（位置同步修复）
- 线程安全加固（ConcurrentHashMap/synchronizedList/moveLock）
- 装备系统缺陷修复（不朽核心销毁/BerserkerTotem 永久buff）
- AOE 自伤修复、物品放置逻辑完善
- 击杀奖励逻辑修正
- 轮次倒计时、单元测试、Redis 跨实例广播

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3.5 + Java 17 |
| 数据库 | H2（开发）/ MySQL 8.0（生产） |
| 缓存 | Redis 7 |
| ORM | Spring Data JPA + Hibernate 6 |
| 实时通信 | WebSocket (Spring WebSocket) |
| 认证 | JWT (jjwt 0.12.6) |
| 前端 | Vue 3 + Composition API + Vite 8 |
| 前端 UI | Element Plus + GSAP 3 |
| 路由 | Vue Router 4 |
| 容器化 | Docker + Docker Compose |
| 反向代理 | Nginx |
| CI/CD | GitHub Actions |

## 快速开始

### 环境要求
- JDK 17+
- Node.js 20+
- Maven 3.9+
- Redis 7+（联机模式需要）
- Docker（可选，用于容器化部署）

### 开发模式

```bash
# 1. 启动 Redis
redis-server

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`，自动代理 `/api` 和 `/game/websocket` 到后端 `http://localhost:8080`。

### Docker 部署

```bash
docker-compose up -d
```

访问 `http://localhost:80`。

## 项目结构

```
├── frontend/                     # Vue3 前端
│   ├── src/
│   │   ├── components/
│   │   │   ├── Game2D.vue        # 2D 游戏主界面（Canvas引擎）
│   │   │   ├── Home.vue          # 登录/注册页
│   │   │   └── Game.vue          # 文字模式（备选）
│   │   ├── views/GameView.vue    # 游戏视图容器
│   │   └── router/index.ts       # 路由配置
│   ├── vite.config.js            # Vite 配置（端口5173，API代理）
│   └── index.html
├── src/main/java/cn/edu/whut/sept/zuul/
│   ├── game/
│   │   ├── Player.java           # 玩家（属性/背包/装备）
│   │   ├── Room.java             # 房间（多人/物品/传送）
│   │   ├── Direction.java        # 方向枚举
│   │   ├── TileType.java         # 瓦片类型
│   │   ├── Game.java             # 游戏核心逻辑
│   │   ├── Parser.java           # 命令解析器
│   │   ├── command/              # 命令模式（11个指令）
│   │   ├── item/                 # 物品系统（17件装备）
│   │   ├── combat/event/         # 对战事件系统
│   │   ├── store/                # 状态持久化
│   │   └── websocket/            # WebSocket 通信
│   │       └── vo/               # 前后端交互VO
│   ├── user/                     # 用户系统（JWT鉴权）
│   └── message/                  # 消息桥接
├── nginx/nginx.conf              # Nginx 反向代理配置
├── docker-compose.yml            # Docker 编排
├── .github/workflows/            # CI/CD 流水线
└── REPORT.md                     # 实训报告
```

## 游戏操作

| 按键 | 功能 |
|------|------|
| W/A/S/D | 上下左右移动 |
| J | 攻击附近玩家 |
| 空格 | 拾取物品 |
| E | 打开/关闭背包 |
| H | 打开/关闭帮助面板 |
| G | 打开GM面板（需输入密钥 `gm123`） |

## 装备一览

| 装备 | 类型 | 效果 |
|------|------|------|
| 铁剑 | 武器 | ATK+3 |
| 暗影裁决 | 武器 | ATK+6 |
| 风暴斩刃 | 武器 | ATK+8，CD 1000ms |
| 霜寒弓 | 武器 | ATK+5，暴击1.5倍 |
| 血棘匕首 | 武器 | ATK+4，每次攻击回血2 |
| 影殇弩 | 武器 | ATK+12，CD 1500ms |
| 战锤 | 武器 | ATK+5，AOE 3×3 |
| 龙骨壁垒 | 防具 | DEF+12 |
| 荆棘铠甲 | 防具 | DEF+6，反弹30%伤害 |
| 疾风靴 | 防具 | ATK+5，DEF+3 |
| 不朽核心 | 被动 | 免疫一次死亡 |
| 狂战士图腾 | 被动 | ATK+8，12秒后消退 |
| 吸血獠牙 | 被动 | 攻击回复33%伤害 |
| 血瓶 | 消耗品 | 恢复40HP |
| 大血瓶 | 消耗品 | 恢复80HP |
| 石化药剂 | 消耗品 | DEF+15，持续20秒 |
| 魔法饼干 | 消耗品 | 永久负重+5 |

## 开发团队

| 成员 | GitHub | 角色 |
|------|--------|------|
| 管名杨 | GrapeEly (gmy) | 系统架构、代码审计、游戏策划、数值平衡、前端核心 |
| 周毅 | ZZYandZY (zy) | 前端基础框架、Redis 管理、单元测试、倒计时 |
| 刘凡恺 | godzhuzhu (lfk) | Spring Boot 骨架、Player/Room、Go/Back/Help、碰撞修复 |

## AI 辅助开发

本项目在 v2.0 扩展开发中使用 OpenCode (DeepSeek-v4 Pro) 进行代码审查与问题排查。详见 [REPORT.md](./REPORT.md) 中 AI 辅助开发说明章节。
