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
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.top
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.bottom
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.jump --extra "value:s:7-11 18:30"
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.stats
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra day:s:2026-07-07
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra "start:s:2026-07-01 00:00" --extra "end:s:2026-07-31 23:59"
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.preload.restart
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.settings.reload
```

`cache.stats` 和 `cache.clear` 返回 `status=accepted` 后，通过重复调用 `weico.status` 读取 `last_operation_state`，直到变为 `completed` 或 `error`。缓存清理必须提供单日 `day`，或同时提供 `start` 与 `end`；它复用应用内相同的日期解析、缓存改写、失败回滚与防旧任务写回逻辑。

## 命令

| 命令 | 作用 | 运行时要求 |
|---|---|---|
| `weico.status` | 查询首页、缓存任务和最近命令状态 | 轻享版已启动 |
| `weico.timeline.top`, `weico.timeline.bottom` | 复用顶部左右双击的绝对边界跳转 | 当前首页时间线已捕获 |
| `weico.timeline.jump` | 复用首页“跳转”日期/时间定位 | 当前首页和缓存已就绪 |
| `weico.cache.stats` | 后台读取缓存范围 | 轻享版 Hook 已加载 |
| `weico.cache.clear` | 后台清除精确发布日期范围 | 当前首页 presenter/action 已就绪 |
| `weico.preload.restart` | 重置并重新调度现有预加载 | 当前首页 presenter 已就绪 |
| `weico.settings.reload` | 重新读取模块设置并刷新快捷按钮/预加载 | 轻享版已启动 |

设置键仍为 `weico_profile_entry`、`weico_timeline_jump_button`、`weico_timeline_cache_clear_button` 和 `weico_timeline_cache_days`。旧版目标应用本地设置会作为迁移回退保留；Provider 中存在显式值后，以 Provider 为准。
