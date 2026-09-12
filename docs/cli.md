# ReWeibo ADB / CLI

ReWeibo 的设置与现有运行时动作都可以通过 ADB 调用，不需要依赖截图、按钮坐标或模拟点击。模块设置由 `SettingsProvider` 统一保存；目标应用运行时命令只有在对应进程已启动、Hook 已加载且首页对象已捕获时才会执行。

The module exposes its settings and existing runtime actions through ADB, without screenshots or coordinate taps. Runtime commands require the target process and its Hook to be ready.

## 直接 ADB

Provider URI 为：

```text
content://com.tianqianguai.reweibo.settings/settings
```

所有命令直接通过 Android 的 `content` CLI 调用，不依赖仓库脚本。以下示例使用设备 `192.168.6.17:5555`。

小米系统可能冻结后台应用并拒绝动态广播。运行时命令前先用 Launcher Intent 激活轻享版，不需要截图或坐标点击：

```bash
adb -s 192.168.6.17:5555 shell am start -n com.weico.international/.appicon_white
```

帮助与设置：

```bash
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method help
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.list
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.get --arg weico_timeline_cache_days
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.set --arg weico_timeline_cache_days --extra value:i:30
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.set --arg weico_timeline_jump_button --extra value:b:false
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.reset --arg weico_timeline_jump_button
```

运行时动作：

```bash
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.status
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.status
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.read --extra max_chars:i:24000
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.read --extra start:s:2026-09-02_09-00-00 --extra end:s:2026-09-02_11-30-00 --extra max_chars:i:48000
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.export --extra start:s:2026-09-02_09-00-00 --extra end:s:2026-09-02_11-30-00
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.top
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.bottom
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.jump --extra "value:s:7-11 18:30"
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.stats
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra day:s:2026-07-07
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra "start:s:2026-07-01 00:00" --extra "end:s:2026-07-31 23:59"
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.preload.restart
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.settings.reload
```

`logs.export`、`cache.stats` 和 `cache.clear` 返回 `status=accepted` 后，通过重复调用 `weico.status` 读取 `last_operation_state`，直到变为 `completed` 或 `error`。日志导出完成后，`last_log_export_path` 是可直接用于 `adb pull` 的文件路径，并同时返回行数和字节数。日志的 `start`、`end` 可分别省略；Android `content` 的绑定值不能可靠携带空格或冒号，因此直接 ADB 使用 `yyyy-MM-dd_HH-mm-ss`，界面输入仍兼容 `yyyy-MM-dd HH:mm:ss` 与 ISO `T`。两者都省略表示全部日志。新版日志含完整日期；旧版只有 `HH:mm:ss` 的日志在“全部”模式中保留，但因日期无法可靠恢复，在精确日期范围中会被统计到 `skipped_legacy_lines` 并跳过。

例如导出完成后：

```bash
adb -s 192.168.6.17:5555 pull /storage/emulated/0/Android/data/com.weico.international/files/Documents/ReWeibo/exports/cli/ReWeibo-log-YYYYMMDD-HHMMSS-SSS.txt .
```

缓存清理必须提供单日 `day`，或同时提供 `start` 与 `end`；它复用应用内相同的日期解析、缓存改写、失败回滚与防旧任务写回逻辑。

## 命令

| 命令 | 作用 | 运行时要求 |
|---|---|---|
| `weico.status` | 查询首页、缓存任务和最近命令状态 | 轻享版已启动 |
| `weico.timeline.status` | 当前首页可见位置、微博 ID、上次阅读 ID、Adapter 条数 | Hook 已加载；无首页时返回 `timeline_ready=false` |
| `weico.timeline.refresh` | 调用原生 `loadNew()` 刷新，并复用刷新阅读锚点 | 当前首页已捕获，缓存/补齐/网络请求空闲 |
| `weico.timeline.load_more` | 调用原生 `loadMore()` 加载更多 | 同上；保留原有加载上限和无更多内容判断 |
| `weico.preload.status` | 查询预加载调度、请求中、停止、重试与页数状态 | Hook 已加载 |
| `weico.gap.status` | 查询断层补齐游标、目标、页数、空页、错误、fallback 和检查点 | Hook 已加载 |
| `weico.logs.status` | 查询日志路径、大小、行数及可识别时间边界 | 轻享版 Hook 已加载 |
| `weico.logs.read` | 返回最多 48 KiB 的可复制终端预览，可指定 `start`、`end`、`max_chars` | 轻享版 Hook 已加载 |
| `weico.logs.export` | 后台流式导出完整或指定时间范围，并返回可 `adb pull` 的路径 | 轻享版 Hook 已加载 |
| `weico.timeline.top`, `weico.timeline.bottom` | 复用顶部左右双击的绝对边界跳转 | 当前首页时间线已捕获 |
| `weico.timeline.jump` | 复用首页“跳转”日期/时间定位 | 当前首页和缓存已就绪 |
| `weico.cache.stats` | 后台读取缓存范围 | 轻享版 Hook 已加载 |
| `weico.cache.clear` | 后台清除精确发布日期范围 | 当前首页 presenter/action 已就绪 |
| `weico.preload.restart` | 重置并重新调度现有预加载 | 当前首页 presenter 已就绪 |
| `weico.settings.reload` | 重新读取模块设置并刷新快捷按钮/预加载 | 轻享版已启动 |

设置键仍为 `weico_profile_entry`、`weico_timeline_jump_button`、`weico_timeline_cache_clear_button` 和 `weico_timeline_cache_days`。旧版目标应用本地设置会作为迁移回退保留；Provider 中存在显式值后，以 Provider 为准。

## 新增命令与 raw ADB 时间参数

以下示例使用设备 `.90`；所有查询均为结构化键值结果，无需 UI dump。

```bash
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.status
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.preload.status
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.gap.status
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.refresh
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.load_more
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.jump --extra value:s:2026-09-12_08-30-00
adb -s 192.168.6.90:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra start:s:2026-09-01_00-00 --extra end:s:2026-09-01_23-59
```

`timeline.refresh/load_more` 的 `status=accepted` 只表示已调用原生入口，返回 `completion=not_tracked`，不写入 `last_operation_state=completed`，也不提供异步 operation ID。网络可能返回空页、出错或由原生逻辑忽略重复请求；应结合三个状态命令与日志观察结果，不能只凭缓存数量没变化判定失败。正在缓存清理、恢复、补齐、预加载或已有可观察网络请求时拒绝新增 CLI 请求，不修改这些已有任务。

`timeline.status` 中位置是 Adapter 位置（含 header），ID 为字符串，避免大整数精度损失；header/footer 没有微博 ID 时返回 `0`。无当前首页时不返回可见位置字段；`last_read_id=0` 表示没有已保存阅读 ID。`window_focused` 表示首页窗口是否有焦点；新的刷新/加载更多命令只使用该首页绑定的 presenter，并要求窗口有焦点，避免操作被详情页遮挡的旧首页。只读取当前视口，不遍历全部缓存。

`preload.status` 的 `state_present=false` 表示尚无该 presenter 的预加载状态；`stopped` 是内部停止标志，不等于成功完成。`gap.status` 的空页/错误计数是当前断层周期统计，状态重置后归零；`active=false` 不区分已完成、尚未启动或已停止，可结合持久化日志判断。状态查询不启动补齐或重置进度。

时间跳转新增 `yyyy-MM-dd_HH-mm` / `yyyy-MM-dd_HH-mm-ss`；缓存清理新增 `yyyy-MM-dd_HH-mm`，仍按开始分钟的 `00.000` 到结束分钟的 `59.999` 处理，日期仍按整天处理。旧输入继续兼容，格式转换不放宽日期校验。

## 功能覆盖盘点

| 模块能力 | ADB 对应方式 |
|---|---|
| 四项设置、恢复单项默认值 | `settings.list/get/set/reset`，运行中再 `weico.settings.reload` |
| 设置/日志弹窗中的业务操作 | 设置接口与 `logs.status/read/export`；无需为了操作再打开弹窗 |
| 时间线顶部/底部、日期跳转 | `timeline.top/bottom/jump` |
| 原生刷新、加载更多 | 新增 `timeline.refresh/load_more` |
| 当前阅读视口、保存的阅读 ID | 新增 `timeline.status` |
| 缓存范围与按范围清理 | `cache.stats/clear`，保留原有异步 operation ID |
| 预加载重启与进度 | `preload.restart` + 新增 `preload.status` |
| 自动断层补齐诊断 | 新增 `gap.status` + `logs.read/export`；不新增暂停策略 |
| 广告 Hook、热重载状态 | `weico.status` 现有 `timeline_ad_*`、`hot_reload_*` 字段 |
| 自动去广告、排序、阅读恢复、过期视频地址处理 | 仍由原有 Hook 自动运行；无需建立重复的手动执行接口 |

The added commands expose native refresh/load-more and structured viewport, preload and gap-fill state. Native actions report acceptance only, not network completion. Existing settings, cache operations, logs, ordering and automatic hooks keep their behavior. Raw ADB timestamps are additionally accepted for timeline jumps and minute-based cache clearing.
