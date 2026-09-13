# Stage 1 美术接入说明

基于 Boss 提交 `693c397e3375dcb8b92446d8d984b305f4cbfd8d`。
所有修改都在独立副本中准备；没有替你提交、推送或合并远程 PR。

## 应用到 Windows 项目

1. 将 `final-boss-stage1-visuals.patch` 保存到仓库根目录，也就是 `source` 文件夹旁边。
2. 在 Git Bash 的 `source` 目录确认当前是准备接入美术的 Boss 分支，并运行
   `git status --short`。先保存自己的未提交修改，再应用补丁。
3. 创建工作分支，然后先检查补丁：

```bash
git switch -c feature/final-boss-stage-1-visuals
cd ..
git apply --check final-boss-stage1-visuals.patch
```

检查成功通常没有输出。若检查报错，先保留报错信息，不要继续覆盖文件；可能是你的
本地分支已经包含其他更新，需要针对新版本整合。

4. 检查通过后应用：

```bash
git apply final-boss-stage1-visuals.patch
cd source
./gradlew core:test build
```

补丁已包含 PNG 和新增 Java 文件，无需手工切图或生成 atlas。
压缩包里的 `source` 文件夹是同一批完整文件，供查看；应用补丁后不需要再覆盖一次。

## 实现的播放顺序

- 普通 Grandpa 停留 1 秒。
- 蓝色变身特效播放 1.2 秒，在中点切换为蓝衣白胡子 Wizard。
- 举杖 0.7 秒后生成 Wave 1 召唤物，绿色出现特效随召唤物短暂播放。
- Wave 1：Wizard 漂浮，紫色护盾循环；攻击被挡时护盾变亮并播放黄色受击闪光。
- Wave 1 清空：落地，护盾关闭，魔法球动作循环；原有 7 秒可攻击窗口完整保留。
- 窗口结束：护盾恢复，再举杖 0.7 秒，随后进入 Wave 2。
- Wave 2：保留沿屏幕边缘移动；每次石化预警同步举杖，原来的固定地面警示圈保留。
- 石化结算：固定标记位置出现灰色尖石；命中时玩家脚下出现灰色提示环。
- 红色召唤物：普通状态循环 idle；Wave 1 预警播放蓄爆帧；真正引爆时播放末尾爆炸帧。
  Wave 2 和远程提前引爆直接进入爆炸段，不添加新的等待时间。

角色动画由更新时钟推进，爆炸结算和伤害仍由原有机制负责；爆炸残影播放完才回收实体。
Boss 的漂浮留在原有实体矩形内，不通过移动物理坐标制造漂浮。
房间尺寸、摄像机跟随设置和自定义 Boss 血条没有改动。

## 可调参数

在 `FinalBossStageOneConfig.java` 中：

- `bossIntroDuration = 1f`
- `bossTransformDuration = 1.2f`
- `bossSummonCastDuration = 0.7f`

更改动作播放时长不用修改 PNG。Boss 两波移动、召唤物阵型和石化机制配置继续使用已有值。

## 状态效果接口

当前 Boss 基线会发送 `PETRIFICATION_EFFECT_REQUESTED` 和
`PETRIFICATION_EFFECT_CLEAR_REQUESTED`，实际减速由状态效果组接入。
脚下灰环表示本次请求的持续时间，不是对实际移动速度已改变的确认。
本补丁没有重复实现减速，也没有改动当前石化触发的时机。

## 验证情况

- Java 21 编译全部 core 主代码、测试代码通过。
- JUnit Console 执行全部 451 项 core 测试通过，包含 40 项 Boss/配置相关测试。
- 新增用例覆盖入场结束前不召唤、破防窗口时长、第二波召唤与单次阶段转换、
  入场中销毁，以及爆炸帧和实体回收时机。
- 完整 Gradle 构建被验证环境的 SonarQube Gradle 插件解析问题阻止；没有宣称
  `./gradlew build` 已在此环境通过。项目构建配置没有为绕过问题而修改。
- 还没有在真实桌面窗口完成游玩测试；需要你在本地检查角色大小、特效对齐和节奏。

## 本地游玩检查

1. 进 Boss 房：先普通 Grandpa，再变身、举杖、生成四个红色召唤物。
2. 攻击护盾：不扣 Boss 血，出现受击闪光。
3. 清空四个召唤物：Boss 落地蓄力，7 秒内可按原规则扣血。
4. 下一波：恢复护盾并生成六个召唤物；无第一波的爆炸等待。
5. 石化预警时离开圆圈：尖石留在原标记位置；命中则出现灰色提示环。
6. 远程打爆召唤物：直接爆炸，伤害只结算一次，残影随后消失。
7. 更换房间：没有残留特效或缺失纹理报错。
