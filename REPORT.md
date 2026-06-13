# 软件工程实践2 实践报告

## 功能实现情况
本小组实现了以下功能扩展点：

### 基础功能扩展（v1.0）

1. 扩展游戏，使得一个房间里可以存放任意数量的物件，每个物件可以有一个描述和一个重量值
2. 在游戏中实现一个"back"命令，玩家输入该命令后会把玩家带回上一个房间
3. 实现支持逐层退回的高级back命令，重复使用可回到游戏起点
4. 在游戏中增加具有传输功能的房间（传送门），随机传输到另一个房间
5. 在游戏中新建一个独立的Player类表示玩家，保存基本属性、背包、负重、装备栏
6. 实现 take 和 drop 命令，支持负重上限检测
7. 设计物品系统，包含铁剑、龙鳞壁垒、风暴斩刃、魔法饼干、血瓶、石化药剂等

### 联机游戏功能（v2.0）
8. 支持网络多人游戏模式，具备玩家注册/登录/JWT鉴权功能
9. 实现 WebSocket 实时通信，支持心跳检测和断线重连
10. 拓展玩家战斗属性(攻击/防御/生命值)和PVP对战系统
11. 设计17件装备物品，分为武器、防具、消耗品、被动道具四大类
12. 实现观察者模式的物品事件系统（受伤/死亡/击杀监听器）
13. 实现游戏状态保存与读取功能（数据库持久化）
14. 增加数据库功能，保存游戏状态和用户数据

### 图形化界面与2D游戏（v3.0）
15. 设计基于 Vue3 + Element Plus 的图形化用户界面
16. 实现 2D 瓦片地图的 Canvas 游戏引擎（WASD移动、J攻击、空格拾取）
17. 实现实时击杀排行榜
18. 实现10分钟沙盘重置机制（平衡性设计）
19. 实现 GM 调试面板（密钥认证、物品生成、满血恢复）
20. 全面中文化：物品、房间、命令、GUI全部中文
21. 实现游戏帮助面板（按键H打开，包含操作说明、战斗机制、装备图鉴）

### 系统优化与质量提升（v4.0）
22. 防御系统平衡优化：伤害公式从 max(1, ATK-DEF) 调整为 max(max(3, ATK×20%), ATK-DEF)
23. 死亡复活流程重构：复活逻辑内联到 hitPlayer 中，修复位置不同步问题
24. 线程安全加固：HashMap→ConcurrentHashMap、ArrayList→synchronizedList/CopyOnWriteArrayList、moveLock
25. 装备系统缺陷修复：不朽核心/凤凰羽毛销毁、BerserkerTotem 永久buff、消耗品防具误判
26. AOE 攻击自伤修复：攻击范围循环排除 (0,0) 坐标
27. 物品放置逻辑完善：新增 isPlaceable() 排除传送口瓦片
28. 击杀奖励逻辑修正：仅在目标确认死亡且未复活时发放奖励
29. 服务端轮次倒计时：全服统一倒计时同步
30. 单元测试覆盖：Player、Room、JwtUtil、AttackCommand 核心类
31. Redis 跨实例广播：房间消息双推去重、玩家在线状态管理

---

## 项目分工情况

小组共 3 人，分工如下：

| 角色 | 人员 | GitHub | 工作范围 |
| :-: | - | - | - |
| 后端全栈 + 前端核心 | 管名杨 | GrapeEly (gmy) | 系统架构设计、代码审计、游戏策划、数值平衡、翻译本地化、Docker部署 |
| 前端基础 + 后端辅助 | 周毅 | ZZYandZY (zy) | 前端页面框架/路由/WebSocket客户端、Redis房间管理、AOE范围修正、中文化UI、单元测试、轮次倒计时、小地图优化 |
| 后端基础 | 刘凡恺 | godzhuzhu (lfk) | SpringBoot骨架、Player/Room类、Go/Back/Help命令、Redis会话管理、碰撞/传送修复 |

### 详细分工（按GitHub提交记录）

#### 管名杨（gmy / GrapeEly）
- 物品系统架构设计（#4）：AbstractItem抽象基类定义、17种装备属性数值策划
- WebSocket多人通信协议设计（#9）：消息格式定义、心跳机制、状态推送方案
- HTTP API用户系统设计与代码审计（#10）：注册/登录接口、JWT鉴权流程
- PVP对战系统事件链设计（#11）：AttackEvent/DeathEvent/FightWinEvent事件定义、观察者模式
- 游戏状态存档方案设计（#12）：StoreManager序列化策略、数据库持久化方案
- Nginx反代 + Docker容器化部署方案（#19-24）：docker-compose编排、CI/CD流程配置
- Take/Drop/Use命令代码审计与优化（#6）
- 消息系统抽象层设计（#8）：AbsMessageBridge双模式架构
- 2D瓦片地图Canvas引擎方案设计（#25-31）：瓦片坐标系统、WASD/攻击/拾取交互方案
- GUI界面优化与代码审计：多行消息系统、防具槽位、血条/伤害数字、小地图物品标记
- GM调试面板功能设计：密钥认证、物品生成、满血恢复
- 帮助面板内容撰写：操作说明、战斗机制、17件装备图鉴
- 游戏文本翻译与本地化：17件物品、5个房间、所有命令、GUI全部中文
- 击杀排行榜方案设计与代码审计：前后端排名逻辑、广播同步
- 游戏逻辑审计与平衡优化：17件装备属性平衡、伤害公式调整、死亡复活流程审计
- 10分钟沙盘重置机制设计：resetAllPlayers/rerollAllItems定时调度
- 防御数值策划：伤害公式 max(max(3,ATK×20%), ATK-DEF) 方案设计

#### 周毅（zy / ZZYandZY）
- 前端登录/注册页面开发（#13）：Element Plus表单、API交互、路由跳转
- 前端游戏主界面开发（#14）：WebSocket实时通信、状态同步、断线重连
- 前端路由配置（#15）：Vue Router、导航守卫、JWT过期检测
- AOE武器攻击范围修正：attackType字段修正、攻击动画匹配武器range
- 前端物品displayName中文化显示、消息翻译
- Google Fonts加载修复：避免Vite预加载阻塞
- 单元测试开发：Player、Room、JwtUtil、AttackCommand 测试用例
- 服务端轮次倒计时：全服统一倒计时同步（2c447c9）
- 小地图其他玩家显示优化（303763b）
- Redis房间管理：玩家移动时Redis成员更新、跨实例广播

#### 刘凡恺（lfk / godzhuzhu）
- Spring Boot项目骨架搭建（#1）：Maven配置、项目初始化
- Player玩家类设计（#2）：属性/背包/负重/战斗属性
- Room房间类重构（#3）：多人适配、物品管理、传送门
- Go/Back/Help命令实现（#5）：移动命令、路径回退、帮助系统
- Redis会话管理（#20）：PubSub跨实例广播、玩家在线状态管理
- 碰撞穿墙修复：玩家卡墙和传送房间问题修复
- 玩家卡箱子无法移动修复
- 硬编码路径修复、profiles丢失修复、GM密钥修复、死亡掉落修复

---

## 软件过程与项目管理
项目过程通过 GitHub Issues 和 Milestone 进行管理。

项目的各个版本 milestone如下：

| 内容 | 版本 |
| :- | - |
| 单人游戏版本 | v1.0 |
| 联机游戏版本 | v2.0 |
| 网页联机游戏版本 | v3.0 |
| 游戏平衡与扩展 | v4.0 |

---

## 代码版本管理
### 代码托管
使用 GitHub 平台仓库 [wutcst/kai-fa-kskblzdjd](https://github.com/wutcst/kai-fa-kskblzdjd) 进行代码托管。

### 分支管理
本项目采用 GitFlow 工作流进行代码版本管理：

- `master` — 稳定发布版本
- `develop` — 开发主线
- `feat/deploy-nginx-cicd` — 部署与CI/CD配置
- `feat/game-polish` — 游戏逻辑优化、GUI中文化、平衡调优
- `feat/combat-balance` — 防御系统削优、复活逻辑重构、位置同步修复
- `hotfix/v2.0-bugs` — v2.0 Bug 修复
- `feature-game-core` — 游戏核心功能

---

## 软件代码评审
- 所有代码需通过 Pull Request 并由其他成员 review 后合并
- 代码推送时，利用 GitHub Actions 自动运行 super-linter 进行代码格式检查
- 编写了单元测试，覆盖 Player、Room、JwtUtil、AttackCommand 等核心类

---

## 项目 CI/CD 应用

项目利用 GitHub Actions 实现了以下自动化流程：

1. **代码检查** — 推送时自动运行 super-linter，检查 Java/XML/Markdown 代码规范
2. **自动化测试** — Maven 运行单元测试，生成测试报告并上传到 Artifacts
3. **自动化打包** — Docker 构建镜像并推送到仓库
4. **持续部署** — 通过自托管 Runner 在服务器上自动拉取镜像并重启容器

---

## 项目技术架构

项目采用前后端分离架构：

| 层级 | 技术栈 |
|------|--------|
| 后端框架 | Spring Boot 3.3.5 + Java 17 |
| 数据库 | MySQL 8.0（生产）/ H2（开发） |
| 缓存 | Redis 7 |
| ORM | Spring Data JPA + Hibernate 6 |
| 实时通信 | WebSocket (Spring WebSocket) |
| 认证 | JWT (jjwt 0.12.6) |
| 构建工具 | Maven 3.9 |
| 前端框架 | Vue 3 + Composition API |
| 前端UI | Element Plus |
| 前端构建 | Vite 8 |
| 路由 | Vue Router 4 |
| 动画 | GSAP 3 |
| 容器化 | Docker + Docker Compose |
| 反向代理 | Nginx |
| CI/CD | GitHub Actions |

---

## 后端详细设计

### 游戏逻辑架构
游戏后端分为以下几个模块：

- **game** — 游戏核心逻辑（Player、Room、Direction、TileType）
- **game/command** — 命令模式设计（Go/Back/Take/Drop/Use/Attack/Look/Items/Help/Save/Load）
- **game/item** — 物品系统（17件装备物品，观察者模式）
- **game/combat/event** — 对战事件系统（AttackEvent/DeathEvent/FightWinEvent）
- **game/websocket** — WebSocket 通信与实时状态推送
- **game/store** — 游戏状态保存与加载
- **game/user** — 用户注册/登录/JWT鉴权
- **game/message** — 消息桥接（控制台/WebSocket双模式）

### 对战系统设计
玩家可指定目标进行PVP对战。战斗伤害公式：

```
最终伤害 = max( max(3, 攻击力 × 20%) , 攻击力 - 目标防御力 )
```

对战特色机制：
- **反击伤害**：被攻击方以 25% 攻击力反击
- **荆棘铠甲**：被攻击时反弹 30% 伤害给攻击者
- **击杀奖励**：击败玩家获得 +2 攻击力、+10 最大生命值
- **不朽核心**：免疫一次死亡，保留 1HP
- **凤凰羽毛**：死亡时复活并恢复 50% HP
- **吸血效果**：攻击回复造成伤害的 33%
- **AOE武器**：战锤攻击 3×3 范围，伤害减半

### 10分钟沙盘重置机制
为保持游戏平衡性，每 10 分钟自动执行全服重置：
- 所有玩家回到出生点，属性重置为基础值
- 所有房间物品清空并重新随机生成
- 击杀数在重置后保留（用于累计排名）
- 前端显示实时倒计时

---

## 前端详细设计

### 2D游戏引擎
基于 HTML5 Canvas 实现 2D 瓦片地图游戏：
- 15×10 瓦片地图，每格 48px
- WASD 键盘移动，J 攻击，空格拾取
- 平滑像素移动插值算法
- 浮动伤害数字、攻击动画特效（近战/远程/AOE）
- 玩家血条、稀有物品光效
- 小地图实时显示其他玩家和物品位置

### 实时GUI面板
- 击杀排行榜（按击杀数降序，🥇🥈🥉）
- 10分钟倒计时（最后60秒红色闪烁警告）
- GM 调试面板（密钥 `gm123` 认证后可生成任意装备）
- 帮助面板（H键打开，操作说明+战斗机制+17件装备图鉴）
- 多行消息日志（击杀金色、受伤红色、拾取绿色）

---

## AI 辅助开发说明

本项目的 v2.0 扩展开发中使用了 AI 辅助编程工具 **OpenCode (DeepSeek-v4 Pro)** 进行代码审查与问题排查。

### 使用方式
通过与 AI 的交互式对话，对项目进行代码审查、Bug 排查和方案讨论。

### AI 辅助完成的主要工作

| 类别 | 内容 |
|------|------|
| **代码审查** | 对 90+ Java 源文件和 Vue 组件进行代码审计，发现 30+ 逻辑缺陷 |
| **Bug 排查** | 协助定位装备属性叠加、AOE自伤、死亡位置不同步、不朽核心引用等 15+ 问题根因 |
| **方案讨论** | 讨论防御公式优化方向、物品事件系统设计、10分钟重置平衡方案 |
| **翻译辅助** | 协助检查中文化翻译的一致性与准确性 |
| **文档辅助** | 帮助面板、README 等内容的部分起草辅助 |

---

## 实施过程问题记录与分析

开发过程中遇到的主要技术问题及解决方案：

| # | 问题 | 根因 | 解决方案 |
|---|------|------|----------|
| 1 | 装备武器后属性叠加两次 | equipWeapon 重复调用 takenBy | 重构装备系统：拾取不加属性，仅装备时加属性 |
| 2 | 龙鳞壁垒无法作为防具装备 | attackRange=1 被误判为武器 | 移除 attackRange 属性，改为纯防具 |
| 3 | AOE攻击击中自身 | 攻击范围循环未排除 (0,0) | 增加 if(ox==0&&oy==0) continue |
| 4 | 死亡复活后位置不同步 | 复活逻辑跨方法调用导致引用过期 | 复活逻辑内联到 hitPlayer 中 |
| 5 | 不朽核心触发后不消失 | removeFromBag 只查背包不查装备栏 | 增加 discardArmor() 方法直接销毁 |
| 6 | 消耗品可当作防具穿戴 | equipArmor 仅判断 weight>=1 | 增加 isConsumable() 方法排除 |
| 7 | Hibernate 不执行 DDL 建表 | YAML 多文档 profile 加载时序问题 | 改为扁平 YAML 并启用 show-sql 验证 |
| 8 | 物品刷新在传送口上 | addItem 仅检查 isWalkable | 新增 isPlaceable() 排除门瓦片 |
| 9 | 满背包拾取导致物品消失 | 先 takeItemAt 后 takeItem | 先 canCarry 判定再移除房间物品 |
| 10 | BerserkerTotem 丢弃后攻击力永久保留 | 监听器绑定在实例上，丢弃未触发 onUnequip | 丢弃时调用 onUnequip 清除 +8 ATK；超时检测覆盖 equippedArmor |
| 11 | 多人并发操作导致状态错乱 | HashMap/ArrayList 非线程安全 | gmSessions→ConcurrentHashMap；bag→synchronizedList；listeners→CopyOnWriteArrayList；moveLock 锁 |
| 12 | 房间消息双推 | WebSocket 直推与 Redis PubSub 广播重复 | 固定走直接推送，Redis 仅用于跨实例备份 |
| 13 | 玩家碰撞卡墙无法移动 | 2D 碰撞检测未区分墙体瓦片 | 修复碰撞检测逻辑，玩家边界与墙体瓦片对齐 |
| 14 | 互相击杀双双获得奖励 | 奖励代码在死亡判定之前执行 | 将奖励逻辑移至判定死亡且未复活之后 |
| 15 | 凤凰羽毛/不朽核心死亡时未销毁 | unequipArmor 将物品放回背包 | 改用 discardArmor() 直接从装备栏移除 |
| 16 | 保存/读取后 portal 历史记录重复 | LoadCommand 未清空 Room.history | 增加历史记录清理逻辑 |

---

## 总结

本小组基于 "World of Zuul" 样例工程，完成了从单机文字游戏到多人联机 2D 图形游戏的全面扩展开发。项目经历了四个里程碑版本：

**v1.0 单人游戏** — 扩展了物品系统、Player 类、传送门、back 命令等基础功能；
**v2.0 联机游戏** — 搭建 WebSocket 通信、JWT 用户鉴权、PVP 对战系统、17 件装备物品；
**v3.0 图形界面** — 基于 Vue3 + Canvas 实现 2D 瓦片地图引擎、实时排行榜、GM 面板；
**v4.0 平衡优化** — 防御公式调整、死亡复活流程重写、线程安全加固、25+ 个 Bug 修复。

开发过程中：

1. **掌握了 Git 协同开发** — 采用 GitFlow 工作流，通过 Issue 跟踪任务、Pull Request 进行代码审查，团队成员在不同分支上并行开发。
2. **实践了 CI/CD 自动化** — 配置 GitHub Actions 实现代码检查（super-linter）、自动化测试（Maven）、Docker 镜像构建与自动部署。
3. **应用了设计模式** — 命令模式（11 个游戏指令）、观察者模式（物品事件监听器、战斗事件链）、策略模式（伤害计算）。
4. **积累了多人联机游戏开发经验** — 前后端分离架构、WebSocket 实时通信、Redis 跨实例广播、线程安全并发控制、坐标同步与碰撞检测。
5. **进行了游戏平衡性设计** — 伤害公式推演、17 件装备数值调优、10 分钟沙盘重置、击杀奖励/反击/吸血/荆棘等机制设计。
6. **经历了完整的测试与修复流程** — 全盘代码审计发现 30+ 逻辑缺陷，编写单元测试覆盖核心类，逐一修复并回归验证。

项目最终实现了具有完整 2D 图形界面、多人实时对战、击杀排行榜、GM 调试面板、10 分钟沙盘重置等功能的联机游戏，并通过 Docker + Nginx 成功部署到服务器。

---

## 参考资料

1. Spring Boot 官方文档：https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
2. Vue 3 官方文档：https://cn.vuejs.org/guide/introduction.html
3. Hibernate ORM 文档：https://hibernate.org/orm/documentation/
4. GitHub Actions 文档：https://docs.github.com/en/actions
5. Docker 文档：https://docs.docker.com/
6. Element Plus 组件库：https://element-plus.org/zh-CN/
7. Canvas API MDN：https://developer.mozilla.org/zh-CN/docs/Web/API/Canvas_API
8. WebSocket MDN：https://developer.mozilla.org/zh-CN/docs/Web/API/WebSocket
