# 有进（Youjin）— 个人目标进度记录

> 把“我想做到”变成“我正在做到”。
> 依据《目标进度APP产品方案_融合版.md》开发的首版实现。

## 首版功能（v0.1）

- 两类目标：累计数值型（如“跑步 60 公里”）、步骤清单型（如“完成个人网站”）
- 目标生命周期：进行中 / 暂停 / 完成 / 归档，删除带撤销
- 今天页：置顶目标卡片 + 今日记录；目标页：筛选、搜索、排序；回顾页：周回顾与复盘笔记
- 快捷记录与补记（发生日期不能在未来）、历史记录编辑/删除、补记标注
- 单个“下一步行动”、达成确认反馈、目标值/日期修改留痕（GoalChange）
- 每日/指定星期提醒（WorkManager + 通知，Android 13+ 运行时权限引导）
- JSON 全量备份与恢复（恢复前快照、事务替换）、CSV 记录导出（SAF 免存储权限）
- 深色模式、减少动效、空状态与失败反馈、数据仅存本地（无需账号）

## 技术栈

Kotlin · Jetpack Compose + Material 3 · MVVM + Repository（构造注入，无 Hilt）
Room 2.7 · DataStore · WorkManager 2.10 · 最低 Android 8.0（API 26）

## 设计风格（Apple Style）

依据 `apple-style-ui-prompt.md` 重做视觉：

- 配色克制：Apple 灰背景 `#F5F5F7`、纯白卡片、近黑文字 `#1D1D1F`，仅用 Apple 蓝 `#0071E3` 作强调；绿/红只用于完成与危险状态，不使用渐变
- 大量留白：页面左右 20dp、卡片内边距 20dp、分组间距 12dp，大标题 34sp semibold 配合负字距
- 精致圆角：卡片 20dp、控件 14dp、弹窗 22–24dp，筛选标签为胶囊
- 微妙阴影：仅悬浮按钮使用低透明度 6dp 阴影，卡片不使用重阴影
- 交互：可点元素统一按压缩放（0.94–0.98）并配合弹簧 / `cubic-bezier(0.25,0.1,0.25,1)` 曲线；卡片按压时内部图标轻微放大
- 自绘 Apple 风格日期选择器与时间滚轮，避免 Material 组件破坏整体风格
- 深色模式采用 iOS 分组背景：纯黑页面 + `#1C1C1E` 卡片

设计令牌集中在 `ui/theme/Theme.kt`（`AppleTheme.colors`），基础组件位于 `ui/components/AppleUI.kt` 与 `ApplePickers.kt`。

## 构建

```bat
:: 首次使用前确认 local.properties 中 sdk.dir 指向本机 Android SDK
gradlew.bat assembleDebug          :: 产物 app\build\outputs\apk\debug\app-debug.apk
gradlew.bat testDebugUnitTest      :: 运行进度计算规则单元测试
```

## 目录结构

```
app/src/main/java/com/youjin/app/
├── data/        Room 实体/DAO/数据库、仓库、备份管理、提醒调度
├── domain/      领域模型与进度计算规则（纯 Kotlin，可测试）
├── ui/          Compose 界面：今天/目标/回顾/设置、通用组件
└── util/        日期与格式化工具
```

## 开发约定

- 发生日期与录入时间分开存储；补记按发生日期统计并标注
- 业务计算集中在 domain/ProgressCalculator.kt，UI 不自行计算进度
- 目标类型与单位创建后不可修改（如需变更请新建目标）
