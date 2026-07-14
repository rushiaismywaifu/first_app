# 🚀 Android Release APK 自動化建置與簽名指南 (CI/CD Setup) - v1.0.2 更新版

本指南詳細說明如何透過 GitHub Actions 自動編譯、最佳化 (ProGuard/R8) 及發布簽名版的 `WebhookSender-v{tag}-release.apk` 至 GitHub Releases。本文件已於 v1.0.2 根據資安審計報告 F-09 進行修正，移除所有關於 Debug 金鑰 fallback 的描述。

---

## ⚠️ 1. 權限問題與手動設定步驟 (必須手動操作)

基於 GitHub 官方安全與權限規範，機器人 / 第三方 GitHub App 授權 Token 預設無法直接對 `.github/workflows/` 目錄下的 Workflow YAML 腳本進行推播修改（會出現 `refusing to allow a GitHub App to create or update workflow without workflows permission`）。同時，GitHub Actions 預設不具備自動發布 Release 的寫入權限。

請您依照以下 **2 個必要的手動步驟** 完成設定，即可完全啟用自動打包發布功能：

### 步驟 A：將 CI/CD 腳本放入 `.github/workflows/` 目錄
我已為您撰寫好完整並且支援自動數位簽名的 GitHub Actions 腳本，並保存在本專案的 `ci-template/build-release.yml` 檔案中。
請您透過 GitHub 網頁介面或 Git 命令列，將該檔案複製到 `.github/workflows/` 目錄：
- **Git 命令列操作** (在專案根目錄下執行)：
  ```bash
  mkdir -p .github/workflows
  cp ci-template/build-release.yml .github/workflows/build-release.yml
  git add .github/workflows/build-release.yml
  git commit -m "ci: add automated APK build and release workflow"
  git push origin main
  ```
- **GitHub 網頁操作**：
  1. 在 GitHub 倉庫首頁點擊 `Add file` -> `Create new file`。
  2. 檔案名稱輸入：`.github/workflows/build-release.yml`。
  3. 將 `ci-template/build-release.yml` 檔案裡的所有內容複製貼上。
  4. 點擊 `Commit changes` 儲存。

### 步驟 B：開啟 GitHub Actions 的讀寫權限 (Workflow Permissions)
為了讓 Action 在編譯完 APK 後，能夠自動幫您建立 Release 並上傳 APK 檔案，需要開啟寫入權限：
1. 進入本 GitHub Repository 頁面，點擊上方頁籤的 **`Settings`** (設定)。
2. 在左側選單中，展開 **`Actions`** 並點選 **`General`**。
3. 頁面往下滑動到 **`Workflow permissions`** 區塊。
4. 選擇 **`Read and write permissions`** (預設通常僅為 `Read repository contents and packages permission`)。
5. 確認勾選下方 **`Allow GitHub Actions to create and approve pull requests`** (選填)。
6. 點擊 **`Save`** 儲存變更。

---

## 📦 2. 如何發布與下載 Release APK

完成上述手動設定後，本專案的 CI/CD Pipeline 支援以下發布與下載方式：

### 方式 A：推播或打標籤自動發布 (推薦)
1. 當有新的 Commit 合併或推播至 `main` 分支時，CI 會自動編譯 Release APK。
2. 若已存在對應 tag 的 Release，系統會自動將最新編譯出的 APK 覆蓋或附加到該 Release 頁面中。
3. 若您推播了新的版本標籤 (例如：`git tag v1.0.2 && git push origin v1.0.2`)，系統會立刻自動建立 `v1.0.2` 的 GitHub Release 並把 APK 附在上頭，APK 檔名為 `WebhookSender-v1.0.2-release.apk` (動態命名，已修復 F-08)。

### 方式 B：手動觸發打包發布 (Workflow Dispatch)
1. 點擊 GitHub 頁面上方的 **`Actions`** 頁籤。
2. 點選左側的 **`Build & Release APK`** 工作流程。
3. 點擊右側的 **`Run workflow`** 按鈕：
   - 分支選擇 `main`。
   - `立即建立或更新 GitHub Release 並上傳 APK？` 下拉選單選擇 **`true`**。
   - `Release 標籤名稱` 輸入您想發布的版本號 (例如 `v1.0.2`)。
4. 點擊綠色的 **`Run workflow`**，等待約 1~2 分鐘編譯完成後，即可至 `Releases` 下載最新打包好的 APK。

### 方式 C：從 Artifacts 下載 APK (無需發布 Release)
每次 Workflow 執行完成後，不論是否建立 Release，都會將打包好的 APK 封裝成 Artifacts。
- 進入該次 Action 的執行明細頁面，滑到最下方的 **`Artifacts`** 區塊。
- 點擊下載 **`WebhookSender-Release-APK`** zip 檔，解壓縮即可直接取得 APK。

### 版本一致性說明 (F-08 已修復)
- `app/build.gradle` 中的 `versionCode` 與 `versionName` 已改為與 Git tag 一致 (v1.0.2 => versionCode 2, versionName 1.0.2)
- CI 中 APK 檔名動態依 tag 命名，不再硬編碼為 `v1.0.0`
- 發布前請執行檢查清單：
  - `./gradlew assembleRelease` 在未設定 Secrets 時應失敗 (安全機制)
  - `androguard sign` 驗證為正式 release keystore
  - APK 內部 versionName 與 tag 一致

---

## 🔐 3. APK 數位簽名設定 (必要)

Android 系統強制要求所有安裝的 APK 都必須有數位簽名。自 **v1.0.1 起，本專案不再提供 Debug 金鑰 fallback** (已修復 F-01)。所有 Release 建置皆需正式生產金鑰，否則 CI 會立即失敗。

### 正式生產金鑰（Keystore）簽名步驟（必要）
本專案不再提供 Debug 金鑰 fallback。發布 Release APK 前必須完成以下 GitHub Secrets 設定，否則 CI 建置將失敗並拋出「Release signing secrets are required; debug signing is not permitted.」。

`app/build.gradle` 中的邏輯：
```gradle
if (keystorePath && file(keystorePath).exists() && storePass && alias && keyPass) {
    storeFile file(keystorePath)
    storePassword storePass
    keyAlias alias
    keyPassword keyPass
} else if (releaseRequested) {
    throw new GradleException("Release signing credentials are required; debug-key fallback is disabled.")
}
```

#### CI 簽名驗證流程 (`.github/workflows/build-release.yml`)
```bash
if [ -z "$KEYSTORE_BASE64" ] || [ -z "$STORE_PASSWORD" ] || [ -z "$KEY_ALIAS" ] || [ -z "$KEY_PASSWORD" ]; then
  echo "::error::Release signing secrets are required. Debug signing is not permitted."
  exit 1
fi
```

此設計確保：
1. 不會意外發布以 Android Debug 金鑰簽署的 Release APK (F-01)
2. 供應鏈安全：任何人無法以公開的 debug key 重製相同簽章的惡意 APK 進行升級攻擊

#### 步驟 (1)：在本地端建立 Keystore 金鑰檔案
```bash
keytool -genkey -v -keystore release.jks -storepass YOUR_STORE_PASSWORD -alias YOUR_KEY_ALIAS -keypass YOUR_KEY_PASSWORD -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Developer,O=Dev,L=Taipei,ST=Taiwan,C=TW"
```

#### 步驟 (2)：將金鑰轉換為 Base64 字串
- **Linux / macOS**：
  ```bash
  base64 -w 0 release.jks > keystore_base64.txt
  ```
- **Windows PowerShell**：
  ```powershell
  [Convert]::ToBase64String([System.IO.File]::ReadAllBytes("release.jks")) | Out-File -Encoding ASCII keystore_base64.txt
  ```

#### 步驟 (3)：新增至 GitHub Secrets
1. 進入 Repository -> **`Settings`** -> **`Secrets and variables`** -> **`Actions`**。
2. 點擊 **`New repository secret`**，依序新增以下 4 個 Secret：
   | Secret 名稱 | 填入內容 |
   | :--- | :--- |
   | **`KEYSTORE_BASE64`** | 步驟 (2) 產生的檔案內所有文字 |
   | **`STORE_PASSWORD`** | storepass 密碼 |
   | **`KEY_ALIAS`** | key alias |
   | **`KEY_PASSWORD`** | keypass 密碼 |

#### 簽章版本 (F-07 已修復)
`app/build.gradle` 已啟用：
- `v1SigningEnabled false` (停用易受攻擊的 v1)
- `v2SigningEnabled true`
- `v3SigningEnabled true` (支援金鑰輪替 lineage)

---

## 🛡️ 4. 安全設定總覽 (v1.0.2)

| 項目 | 設定 | 對應漏洞 |
| --- | --- | --- |
| allowBackup | `false` | F-02 |
| SharedPreferences 儲存 Webhook URL | 已移除 (不持久化) | F-02 |
| networkSecurityConfig | 僅信任系統 CA | F-05 前置 |
| CertificatePinner | 綁定 discord.com / discordapp.com SPKI | F-05 |
| Webhook URL 驗證 | `WebhookUrlValidator` 嚴格正則 + 無繞過 | F-03 |
| 錯誤訊息 | 固定字串，不洩漏 response body | F-04 |
| InputType | `textPassword` + `password_toggle` | F-06 |
| 剪貼簿 | 驗證白名單後才貼上 | F-13 |
| ProfileInstallReceiver | exported=false override | F-11 |

---

## ⚠️ 5. 已知不相容性 (F-10)

- **v1.0.0 為 Android Debug 金鑰簽署**，屬於極高風險版本，已不應使用
- v1.0.1 起改用正式 release keystore，金鑰不同故無法就地升級
- 若使用者已安裝 v1.0.0，需先解除安裝再安裝 v1.0.1+，並在 Release Notes 明確標示
- 未來金鑰輪替請使用 v3 簽章的 lineage 機制，避免再次發生

建議在 GitHub Release 頁面將 v1.0.0 標記為「⚠️ 不安全，請勿使用」或直接刪除。

---

## 📋 6. 發布前檢查清單 (來自報告 15.1)

- [ ] `./gradlew assembleRelease` 在未設定 Secrets 時應失敗
- [ ] `androguard sign` 驗證憑證為 release keystore (非 Android Debug)
- [ ] `androguard axml` 驗證 `allowBackup=false`、有 `networkSecurityConfig`
- [ ] APK 內部 `versionCode`/`versionName` 與 Git tag 一致 (例如 tag v1.0.2 => versionCode 2 / versionName 1.0.2)
- [ ] APK 檔名包含正確版本號 (如 `WebhookSender-v1.0.2-release.apk`)
- [ ] `apktool` 反編譯確認 DEX 中不存在 `WebhookSenderPrefs`、`saved_webhook_url`
- [ ] 已更新 README 與此文件，移除所有提及 debug fallback 的內容
- [ ] Release Notes 說明與前版本的金鑰相容性 (F-10)
