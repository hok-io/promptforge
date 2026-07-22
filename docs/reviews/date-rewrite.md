# Git 日期重新編排

日期：2026-09-26。依使用者要求，由 2026-09-01 起分批排列。這些日期是重新編排的展示時間，不是實際開發時間；不能作為持續開發的證據。

author 與 committer 日期一同更新。所有原始 commit 的 tree 與次序保留；review 文件的 hash 在後續 patch 更新。原歷史保存在 `refs/archive/before-date-rewrite` 和 `.git/before-date-rewrite.bundle`。

| 原 hash | 新 hash | 原 author 時間 | 新時間（+08:00） | 用途 |
|---|---|---|---|---|
| 8b0238f | b7f984e | 2026-09-22T13:31:33+08:00 | 2026-09-01T19:10:00+08:00 | build: establish local Java test and CI gate |
| 8739dac | 0897e52 | 2026-09-22T13:32:10+08:00 | 2026-09-01T19:47:00+08:00 | test: specify rule file loading before implementation |
| 65c5532 | 8936649 | 2026-09-22T13:33:51+08:00 | 2026-09-01T19:59:00+08:00 | feat: load traceable project rule files |
| dd8ef4b | 7e39216 | 2026-09-22T13:34:42+08:00 | 2026-09-02T19:10:00+08:00 | test: specify scoped keyword selection before implementation |
| 7afabf3 | af4706a | 2026-09-22T13:35:22+08:00 | 2026-09-02T19:47:00+08:00 | feat: select relevant rules deterministically |
| 0b5cfd4 | 93ea34a | 2026-09-22T13:36:01+08:00 | 2026-09-03T19:10:00+08:00 | test: specify concise task frame before implementation |
| 36d57ea | e6abf2b | 2026-09-22T13:36:47+08:00 | 2026-09-03T19:35:00+08:00 | feat: compile short traceable task frames |
| 37dd21a | 195e2ac | 2026-09-22T13:37:24+08:00 | 2026-09-04T19:10:00+08:00 | test: specify usable CLI before implementation |
| c2116b5 | 947011f | 2026-09-22T13:39:26+08:00 | 2026-09-04T19:59:00+08:00 | feat: run prompt frames from a local Java CLI |
| b107ad2 | 07217f4 | 2026-09-22T13:40:38+08:00 | 2026-09-06T19:10:00+08:00 | test: specify rule safety boundaries before implementation |
| 371020d | 89b5fa4 | 2026-09-22T13:44:43+08:00 | 2026-09-06T19:47:00+08:00 | feat: bound rule inputs and frame size |
| 9f2904e | 28c83ae | 2026-09-22T13:45:41+08:00 | 2026-09-07T19:10:00+08:00 | test: specify Codex skill entrypoint before implementation |
| 60fce31 | 98f36bf | 2026-09-22T13:47:42+08:00 | 2026-09-07T19:35:00+08:00 | feat: provide an on-demand Codex prompt-frame skill |
| 449f96d | a396f43 | 2026-09-22T13:48:20+08:00 | 2026-09-09T19:10:00+08:00 | test: specify conflict metadata before implementation |
| 7ba70cb | 4a346fe | 2026-09-22T13:49:27+08:00 | 2026-09-09T19:59:00+08:00 | feat: load immutable conflict relationships |
| ef65bdf | b1fdf8f | 2026-09-22T13:50:05+08:00 | 2026-09-10T19:10:00+08:00 | test: require explicit conflict diagnostics before implementation |
| aebdbec | 1f05744 | 2026-09-22T13:51:22+08:00 | 2026-09-10T19:47:00+08:00 | feat: reject conflicting selected rules |
| 2b5c1fe | 890ceff | 2026-09-22T13:53:40+08:00 | 2026-09-11T19:10:00+08:00 | docs: record phase two edge and Java review evidence |
| 9fee286 | d15c4ce | 2026-09-22T13:54:40+08:00 | 2026-09-13T19:10:00+08:00 | test: specify local knowledge cards before implementation |
| 36e259d | ba8d9d5 | 2026-09-22T13:56:16+08:00 | 2026-09-13T19:47:00+08:00 | feat: load traceable ADR and policy cards |
| dd6f4b8 | 37ddf56 | 2026-09-22T13:56:49+08:00 | 2026-09-14T19:10:00+08:00 | test: specify local BM25 retrieval before implementation |
| 6af404d | 98598d8 | 2026-09-22T13:58:12+08:00 | 2026-09-14T19:35:00+08:00 | feat: rank local knowledge cards with BM25 |
| 7d913f3 | 5a8ef67 | 2026-09-22T13:59:11+08:00 | 2026-09-16T19:10:00+08:00 | test: specify local knowledge query before implementation |
| 7af24c2 | a20ddfd | 2026-09-22T14:01:20+08:00 | 2026-09-16T19:59:00+08:00 | feat: query source-linked local knowledge |
| 4843915 | d45a3aa | 2026-09-22T14:02:05+08:00 | 2026-09-18T19:10:00+08:00 | test: require executable JAR before packaging |
| ecaa97c | 1cb81bf | 2026-09-22T14:03:26+08:00 | 2026-09-18T19:47:00+08:00 | build: package and smoke-test executable JAR |
| b1cd80c | 14f5c22 | 2026-09-22T14:06:14+08:00 | 2026-09-20T19:10:00+08:00 | test: specify project validation and cross-platform CI |
| 1631e4c | dbe67d9 | 2026-09-22T14:08:14+08:00 | 2026-09-20T19:35:00+08:00 | feat: validate project rules and knowledge before review |
| 7eb8b3c | f0fc927 | 2026-09-22T14:10:47+08:00 | 2026-09-22T19:10:00+08:00 | test: expose direct JAR Chinese output corruption |
| 01c738a | 3d2dd83 | 2026-09-22T14:11:25+08:00 | 2026-09-22T19:59:00+08:00 | fix: emit UTF-8 from direct Java JAR entrypoint |
| a89db4b | 0891eb9 | 2026-09-22T14:12:09+08:00 | 2026-09-23T19:10:00+08:00 | test: cover knowledge edges and record release review |
| 2001a8e | 1d4e8d2 | 2026-09-26T11:50:00+08:00 | 2026-09-25T19:10:00+08:00 | docs: organize human review of all 31 Java commits |
| 4ce7901 | daf5ae5 | 2026-09-26T11:50:00+08:00 | 2026-09-25T19:59:00+08:00 | docs: define core readiness steps before web work |
| 829fbc5 | 10394f4 | 2026-09-26T11:50:01+08:00 | 2026-09-26T19:10:00+08:00 | docs: plan local Spring prompt and skill workbench |
| 6ee7c14 | b0a86bf | 2026-09-26T11:50:16+08:00 | 2026-09-26T19:47:00+08:00 | docs: tidy review card spacing |
