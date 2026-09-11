---
name: PromptForge
description: "Turn a user's task into an executable prompt using the selected local PromptForge outline."
---

# PromptForge

Project root: `D:/PromptForge`. Resolve dependency paths against this root.

Input: the user's original task. Preserve goal, scope, constraints, expected result, and acceptance checks.

## 1. 理解與界定任務

- purpose: 保留使用者原始目標、範圍、限制與預期結果。
- input: 使用者的任務要求與目前對話。
- decision: 辨認已知事項、必要缺口和可自行處理的選擇。
- actions: 查閱相關本地資料；只有關鍵決策無法自行確認時才追問。
- observation: 具體來源、使用者回覆或仍缺少的資訊。
- adjustment: 根據觀察縮小問題或調整假設。
- output: 可執行的任務界定。
- stop: 目標與必要限制足以寫入 prompt，或列出受阻原因。

## 2. 組成可執行 Prompt

- purpose: 寫出可直接交給 AI 執行的指示。
- input: 原始要求、已核實資料與未解缺口。
- decision: 保留目標、範圍、限制、預期結果及驗收方法。
- actions: 明列依證據決定下一步、採取行動、觀察結果、調整或重試、判斷完成或受阻的 ReAct 流程。
- observation: Prompt 是否有可操作的行動與驗收標準。
- adjustment: 發現缺漏時補入來源或標記待確認事項。
- output: 可直接供 PromptForge-context 檢查的完整 prompt。
- stop: Prompt 可執行且缺口明確，或說明受阻。

## Required result

Write a prompt with the original goal, scope, constraints, expected output and acceptance method. Include an executable ReAct loop: judge the next step from current evidence; act; observe actual results; adjust, gather more context or retry; decide completion or blockage. Run only steps needed for this task. Ask or research when essential information is missing. Show a brief decision summary, action results and verification evidence; never request hidden chain of thought. The prompt must be usable directly as input to $PromptForge-context.
