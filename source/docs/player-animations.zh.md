# 玩家动画

## 范围

本页说明 `core-player-features` 分支上，玩家如何播放待机、走路和近战攻击动画。

玩家是四向精灵。移动逻辑已在 `PlayerActions` 里。本工作负责播放对应图集动画，使朝向与移动、挥砍一致。

流程：

1. 游戏加载 `images/idle_down.atlas`（贴图 `idle_down.png`，页面尺寸 768×1280）。
2. `PlayerFactory` 注册循环播放的 `idle_*` / `walk_*`，以及只播一次的 `attack_*`。
3. 玩家初始为 `idle_down`。
4. WASD 使 `PlayerActions` 发出 `walkDown` / `walkLeft` / `walkRight` / `walkUp`。
5. 松开按键后，按最后走路方向发出 `idle*`。
6. 按 J 发出 `attack`，再带朝向向量发出 `weaponAttack`（默认朝下）。
7. `PlayerAnimationController` 播放一次 `attack_*`，期间忽略 idle/walk，结束后回到之前的待机或走路。

斜向朝向与走路相同：**竖直方向优先于水平方向**。

## 操作

| 输入 | 事件 | 动画 |
| --- | --- | --- |
| W / A / S / D | `walk`，然后 `walkUp` / `walkLeft` / `walkDown` / `walkRight` | `walk_*`（循环） |
| 松开 WASD | `walkStop`，然后 `idle*` | `idle_*`（循环） |
| J | `attack` → `weaponAttack` | `attack_*`（播一次） |
| 空格 | `dash` | 暂无独立冲刺动画（仍用 idle/walk） |
| K | `specialAttack` | 目前只有音效，没有动画 |

点 **Start** 后要点一下游戏窗口，并切到英文输入法，否则 WASD/J 可能进不到游戏。

## 组件约定

`PlayerAnimationController` 只监听玩家实体上的事件，不生成攻击判定盒。

- 移动事件：`idleDown`、`idleLeft`、`idleRight`、`idleUp`、`walkDown`、`walkLeft`、`walkRight`、`walkUp`。
- 攻击：`weaponAttack`，参数为朝向 `Vector2`（与剑判定使用同一事件）。
- `attacking` 为 true 时，idle/walk 只记录下一套移动动画名，不调用 `startAnimation`。
- `AnimationRenderComponent.isFinished()` 为 true 后，恢复移动动画。

`PlayerFactory` 必须先 `addAnimation` 注册全部图集名，再 `startAnimation("idle_down")`。

攻击片段使用 `PlayMode.NORMAL`，每帧 0.06 秒（8 帧约 0.48 秒），接近剑的 0.5 秒冷却。

## 相关类

- `com.csse3200.game.components.player.PlayerAnimationController`
- `com.csse3200.game.components.player.PlayerActions`
- `com.csse3200.game.components.player.KeyboardPlayerInputComponent`
- `com.csse3200.game.components.player.TouchPlayerInputComponent`
- `com.csse3200.game.entities.factories.PlayerFactory`
- 图集：`source/core/assets/images/idle_down.atlas`

## 自动测试

在 `source` 目录、JDK 21 下：

```bash
./gradlew :core:test --tests com.csse3200.game.components.player.*
```

- `PlayerAnimationControllerTest`：四向 idle/walk/attack；斜向（下和上）；攻击中不切走路；攻击结束后恢复 idle/walk。
- `PlayerActionsTest`：走路/待机事件；默认朝下攻击；按上次走路朝向攻击；特殊攻击不触发 `weaponAttack`；冲刺速度为走路 5 倍；冲刺中忽略第二次 dash 和 `walkStop`。
- `KeyboardPlayerInputComponentTest`：WASD（含 A/S 与 W+D）、J 攻击、K 特殊攻击、空格冲刺、E 同时 interact 与 itemPickup。
- `TouchPlayerInputComponentTest`：方向键走/停，点击触发攻击。

## 手动验收

1. 在 `source` 下运行 `./gradlew desktop:run`。
2. 点 **Start**，再点游戏窗口。
3. WASD 走路，确认 walk 朝向正确。
4. 停下，确认 idle 是最后走路方向。
5. 四个朝向各按一次 J，确认挥砍只播一次，然后回到 idle 或 walk。
6. 右下斜向走，应播 `walk_down`（竖直优先）；该朝向攻击应为 `attack_down`。

## 不在本任务范围

- K 特殊攻击动画
- 冲刺动画
- 玩家受伤 / 死亡动画
- 修改剑伤害或判定时长（武器组）

同一张精灵表上还有未使用的行，可作为后续 ticket。
