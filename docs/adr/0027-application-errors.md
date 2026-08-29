# ADR 0027：用应用错误类型表达失败

Workbench 服務以 `FailureKind` 表達失敗原因，不在應用層攜帶 HTTP 數字。類型包括輸入無效、資源不存在、版本衝突及儲存失敗。Web adapter 負責把這些類型映射成 HTTP 狀態，讓工作台服務與傳輸協定保持分離。

既有 HTTP 合約維持一致：`INVALID_INPUT` 對應 422、`NOT_FOUND` 對應 404、`CONFLICT` 對應 409、`STORAGE_FAILURE` 對應 500。HTTP adapter 的測試直接驗證每種錯誤類型與回應狀態的對應。

日期：2026-09-24
