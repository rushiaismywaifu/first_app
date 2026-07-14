# 🪝 Discord Webhook Sender

一個簡易的 Android App，用於透過 Discord Webhook 發送訊息。已通過深度資安審計，符合 OWASP MASVS-L1 基準，並完成 P0-P2 安全強化。

## ✨ 功能

- 🔗 **Webhook URL** - 貼上你的 Discord Webhook URL (密碼遮罩 + 可見性切換)
- 👤 **自訂名稱** - 設定發送者的顯示名稱
- 🖼️ **自訂頭像** - 透過 URL 設定發送者頭像
- 💬 **訊息內容** - 發送文字訊息
- 📋 **Embed 嵌入訊息** - 支援完整的 Discord Embed 格式
  - 標題 (Title) 與描述 (Description)
  - 顏色選擇器 (Color Picker)
  - 圖片 (Image) 與縮圖 (Thumbnail)
  - 頁尾 (Footer) 含圖示
  - 作者 (Author) 含圖示與連結
  - 自訂欄位 (Fields) 支援行內 (Inline) 模式
- 📋 **JSON 預覽** - 預覽發送的 JSON 內容並複製
- 🎨 **Discord 深色主題** UI
- 🛡️ **安全強化** - 憑證綁定、嚴格 URL 白名單、無持久化儲存

## 📱 截圖

> 使用 Discord Blurple 配色的深色主題界面

## 🔧 技術

- **語言**: Kotlin
- **最低 SDK**: Android 7.0 (API 24)
- **目標 SDK**: Android 14 (API 34)
- **HTTP 客戶端**: OkHttp 4.12.0 + Certificate Pinning (F-05)
- **JSON 序列化**: Gson 2.10.1
- **UI**: Material Design Components (textPassword + password_toggle for Webhook URL)
- **簽章**: v2 + v3 (支援金鑰輪替, F-07)
- **建置**: AGP 8.2.2, R8/ProGuard, JDK 17

## 📦 下載

前往 [Releases](../../releases) 頁面下載最新版 APK。

- **最新版**: `v1.0.2` (versionCode 2, versionName 1.0.2) - 已修復全部 Medium 風險
- **檔名格式**: `WebhookSender-v{tag}-release.apk` (動態命名, 已修復 F-08)
- 切勿使用 `v1.0.0`：該版本以 Android Debug 金鑰簽署 (F-01, CVSS 9.1)，屬於極高風險，已標記為不安全

### 版本歷史與簽章相容性

| 版本 | versionCode | 簽章金鑰 | 狀態 | 備註 |
| --- | --- | --- | --- | --- |
| v1.0.0 | 1 | Android Debug (CN=Android Debug) | ❌ 不安全，請勿使用 | F-01/F-02 極高風險，已棄用 |
| v1.0.1 | 1 (不一致) | Release (CN=Developer) | ⚠️ 已修復 Critical/High，但殘留 Medium | 需先解除安裝 v1.0.0 才能安裝 (F-10) |
| v1.0.2 | 2 | Release (CN=Developer) v2+v3 | ✅ 建議使用 | 完成全部 P0-P2 修補，檔名與版本一致 |

#### ⚠️ 重要升級說明 (F-10)
若您已安裝 **v1.0.0** (debug 簽章)，由於 v1.0.1+ 改用正式 release keystore，金鑰不同，Android 系統會阻止就地升級 (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)。請先 **解除安裝 v1.0.0**，再安裝 v1.0.2。 v1.0.1 與 v1.0.2 使用相同 release keystore，可正常就地升級。未來若需再次變更簽章金鑰，將透過 v3 簽章的 lineage 機制進行金鑰輪替 (F-07)。

## 🚀 使用方式

1. 在 Discord 頻道設定中建立 Webhook，複製 Webhook URL
2. 開啟 App，貼上 Webhook URL (可使用剪貼簿按鈕，App 會驗證白名單 F-13)
3. （可選）設定使用者名稱和頭像 URL
4. 輸入訊息內容
5. 如需 Embed，開啟「啟用 Embed」並填寫相關欄位
6. 點擊「🚀 發送」按鈕

## 🛡️ 安全設計

### 已修復的 Critical / High 漏洞 (v1.0.1)
- **F-01 (CVSS 9.1)**: 移除 debug keystore fallback，CI 未設定 Secrets 時直接失敗，僅允許正式 release keystore 簽署
- **F-02 (CVSS 8.2)**: `allowBackup=false`，移除 SharedPreferences 明文儲存 Webhook URL
- **F-03 (CVSS 7.5)**: 新增 `WebhookUrlValidator` 嚴格正則白名單，無繞過對話框
- **F-04**: 錯誤訊息不再洩漏伺服器回應內文

### v1.0.2 新增修復 (Medium / Low)
- **F-05**: 實作 OkHttp `CertificatePinner` 綁定 `discord.com` / `discordapp.com` 的 SPKI SHA-256 (leaf + intermediate + root)，防止中間人攻擊。Pin 失敗時顯示明確錯誤。
- **F-06**: Webhook URL 輸入框改為 `textPassword` + `password_toggle` 眼睛圖示，防肩窺
- **F-07**: 啟用 `v3SigningEnabled true`, `v1SigningEnabled false`, `v2SigningEnabled true`，支援金鑰輪替
- **F-08**: 修正 `versionCode=2` / `versionName=1.0.2` 與 tag 一致，CI APK 檔名動態化為 `WebhookSender-${TAG_NAME}-release.apk`
- **F-09**: 更新 `ci-setup.md` 移除「預設自動產生 Debug 金鑰」的矛盾描述，改為「必須設定 Secrets，否則建置失敗」
- **F-10**: 於 README 與 Release Notes 明確標示需先解除安裝 v1.0.0
- **F-11**: `AndroidManifest.xml` 覆寫 `ProfileInstallReceiver` 為 `exported=false` (使用 `tools:replace`)
- **F-13**: 剪貼簿貼上增加白名單驗證，僅允許 `https://` 且通過 `isAllowed` 的 URL

### 仍為可接受的 Low 風險
- **F-12**: ProGuard 未混淆字串字面量 - 為可選的增加逆向難度，已評估保留現狀 (可選用 StringFog/DexGuard，若需要)

### 網路安全
- `network_security_config.xml`: 僅信任系統 CA，不信任使用者安裝 CA
- `usesCleartextTraffic=false`
- OkHttp timeout 15s，僅允許 HTTPS white-listed Webhook URL

## 📝 注意事項

- 此 App 僅用於發送 Webhook 訊息，不會儲存任何個人資料 (自 v1.0.1 起移除持久化)
- Webhook URL 請妥善保管，避免洩漏，UI 已預設遮罩
- 支援 `discord.com` 和 `discordapp.com` 的 Webhook URL，含 `/api/webhooks/{id}/{token}` 與 `/api/vX/webhooks/...` 格式
- 若需「記住 URL」功能，請未來改用 `EncryptedSharedPreferences` (AndroidX Security) + Android Keystore，勿再使用明文 SharedPreferences

## 🔍 審計報告

本專案已接受 Z.ai 資安團隊深度審計，報告涵蓋 OWASP MASVS、CWE、CVSS v3.1。完整修補路線圖請見本倉庫 `ci-setup.md` 第 4-6 章。

### 發布前檢查清單 (報告 15.1)
- `./gradlew assembleRelease` 未設定 Secrets 應失敗
- `androguard sign` 憑證為 release (非 Android Debug)
- `allowBackup=false` + `networkSecurityConfig` 存在
- versionCode/versionName 與 tag 一致
- APK 檔名含正確版本號
- DEX 中無 `WebhookSenderPrefs` / `saved_webhook_url`
- 文件已更新，無 debug fallback 描述
- Release Notes 說明金鑰相容性

## 🔐 簽名設定

請見 [ci-setup.md](./ci-setup.md) 第三章，必須設定 `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` 四個 GitHub Secrets，否則建置失敗 (安全設計)。
