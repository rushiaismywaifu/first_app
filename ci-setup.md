# 🚀 Android Release APK 自動化建置與簽名指南 (CI/CD Setup)

本指南詳細說明如何透過 GitHub Actions 自動編譯、最佳化 (ProGuard/R8) 及發布簽名版的 `WebhookSender-v1.0.0-release.apk` 至 GitHub Releases。同時詳細列出**所有如有權限問題或需手動操作的部分**。

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
為了讓 Action 在編譯完 APK 後，能夠自動幫您建立 Release (`v1.0.0`) 並上傳 APK 檔案，需要開啟寫入權限：
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
2. 若已存在 `v1.0.0` Release（例如草稿 Release），系統會自動將最新編譯出的 APK 覆蓋或附加到該 Release 頁面中。
3. 若您推播了新的版本標籤 (例如：`git tag v1.0.1 && git push origin v1.0.1`)，系統會立刻自動建立 `v1.0.1` 的 GitHub Release 並把 APK 附在上頭。

### 方式 B：手動觸發打包發布 (Workflow Dispatch)
1. 點擊 GitHub 頁面上方的 **`Actions`** 頁籤。
2. 點選左側的 **`Build & Release APK`** 工作流程。
3. 點擊右側的 **`Run workflow`** 按鈕：
   - 分支選擇 `main`。
   - `立即建立或更新 GitHub Release 並上傳 APK？` 下拉選單選擇 **`true`**。
   - `Release 標籤名稱` 輸入您想發布的版本號 (例如 `v1.0.0`)。
4. 點擊綠色的 **`Run workflow`**，等待約 1~2 分鐘編譯完成後，即可至 `Releases` 下載最新打包好的 `WebhookSender-v1.0.0-release.apk`。

### 方式 C：從 Artifacts 下載 APK (無需發布 Release)
每次 Workflow 執行完成後，不論是否建立 Release，都會將打包好的 APK 封裝成 Artifacts。
- 進入該次 Action 的執行明細頁面，滑到最下方的 **`Artifacts`** 區塊。
- 點擊下載 **`WebhookSender-Release-APK`** zip 檔，解壓縮即可直接取得 `WebhookSender-v1.0.0-release.apk`。

---

## 🔐 3. APK 數位簽名設定 (進階/自訂金鑰)

Android 系統強制要求所有安裝的 APK 都必須有數位簽名。我們的 `build.gradle` 與 CI 腳本已設計為雙軌制：

### 預設自動簽名機制 (免設定直接可用)
如果您**尚未**設定自己的正式生產金鑰 (Keystore)，本專案的 CI 腳本與 `app/build.gradle` 預設會自動產生標準 Debug 金鑰為 Release APK 進行簽名 (`assembleRelease`)。
這保證了產生的 `WebhookSender-v1.0.0-release.apk` 可以直接在您的 Android 手機或模擬器點擊下載並順利安裝測試，不會出現「未簽署應用程式 / 無法安裝」的錯誤提示。

### 自訂正式生產金鑰 (Keystore) 簽名步驟
如果您打算發布正式的私鑰簽名 APK 或準備上架 Android 商店，請依循以下步驟設定 GitHub Repository Secrets：

#### 步驟 (1)：在本地端建立 Keystore 金鑰檔案
打開命令提示字元 (終端機)，執行以下指令建立 `release.jks` 金鑰：
```bash
keytool -genkey -v -keystore release.jks -storepass YOUR_STORE_PASSWORD -alias YOUR_KEY_ALIAS -keypass YOUR_KEY_PASSWORD -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Your Name,O=Your Org,C=TW"
```
> *(請將 `YOUR_STORE_PASSWORD`、`YOUR_KEY_ALIAS`、`YOUR_KEY_PASSWORD` 替換為您的密碼與名稱並妥善保存)*

#### 步驟 (2)：將金鑰轉換為 Base64 字串
為了在 CI/CD 中安全存放金鑰檔案，需將其轉為 Base64 字串：
- **Linux / macOS 終端機**：
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
   | **`KEYSTORE_BASE64`** | 步驟 (2) 產生的 `keystore_base64.txt` 檔案內所有文字 |
   | **`STORE_PASSWORD`** | 步驟 (1) 設定的 `storepass` 密碼 |
   | **`KEY_ALIAS`** | 步驟 (1) 設定的 `alias` 金鑰別名 |
   | **`KEY_PASSWORD`** | 步驟 (1) 設定的 `keypass` 密碼 |

> 完成上述 Actions Secrets 設定後，下次 CI 執行時會自動偵測並解密 `KEYSTORE_BASE64`，使用您的專屬生產金鑰為 Release APK 進行數位簽名！
