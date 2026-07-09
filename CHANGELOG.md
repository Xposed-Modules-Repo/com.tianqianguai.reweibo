# Changelog

## 1.0

- First official LSPosed/Xposed repository release for package `com.tianqianguai.reweibo`.
- Default recommended scope is Weibo Lite (`com.weico.international`).
- Uses the legacy `de.robv.android.xposed` API with `xposedminversion=82`.
- Removes Weibo Lite splash ads and timeline advertising paths.
- Forces Weibo Lite timeline reverse/newest-first behavior for the home feed.
- Filters ad-like and contentless timeline status entries from loaded data.
- Persists and restores an expanded timeline cache to reduce position loss after refresh or cache eviction.
- Saves and restores the last-read timeline position when the restored cache is trusted.
- Adds timeline top-bar double-tap shortcuts for jumping to timeline edges.
- Refreshes or replaces stale cached video URLs when opening timeline videos where possible.
- Hydrates cached timeline text/media fields so restored entries remain renderable.
- Keeps legacy hooks for Sina Weibo and Share available for manual scopes, but does not advertise them as default recommended scopes.
