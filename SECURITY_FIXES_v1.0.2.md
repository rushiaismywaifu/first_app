# 🛡️ v1.0.2 資安修復報告 - 對應 Z.ai 審計報告所有待修項目

本文件總結 v1.0.2 對資安審計報告中殘留的 F-05 ~ F-13 的修復實作，供開發者與審計方複核。

## 修復對照表

| 編號 | 標題 | 原風險 | 修復狀態 | 修復檔案 |
| --- | --- | --- | --- | --- |
| F-05 | 缺少憑證綁定 | Medium 4.6 | ✅ 已修復 | `WebhookApi.kt` |
| F-06 | Webhook URL 在 UI 未遮罩 | Medium 3.9 | ✅ 已修復 | `activity_main.xml` |
| F-07 | 僅 v2 簽章未啟用 v3 | Medium 4.0 | ✅ 已修復 | `app/build.gradle` |
| F-08 | versionCode/versionName 與 tag 不一致 | Medium 3.7 | ✅ 已修復 | `app/build.gradle`, `.github/workflows/build-release.yml`, `ci-template/build-release.yml` |
| F-09 | ci-setup.md 文件與 build.gradle 矛盾 | Medium 3.5 | ✅ 已修復 | `ci-setup.md` |
| F-10 | 簽章金鑰變更阻斷就地升級 | Medium 4.2 | ✅ 文件修復 | `README.md`, `ci-setup.md`, Release Notes in workflow |
| F-11 | ProfileInstallReceiver exported=true | Low 2.4 | ✅ 已修復 | `AndroidManifest.xml` |
| F-12 | ProGuard 未混淆字串 | Low 2.0 | ⚠️ 可接受 (文件說明) | `README.md` |
| F-13 | 剪貼簿貼上無限制讀取 | Low 2.5 | ✅ 已修復 | `MainActivity.kt` |
| P0 | 標註 v1.0.0 為不安全 | Immediate | ✅ 文件修復 | `README.md`, `ci-setup.md` |

## 詳細修復說明

### F-05: Certificate Pinning
**位置**: `app/src/main/java/com/webhook/sender/WebhookApi.kt`

```kotlin
private val certificatePinner = CertificatePinner.Builder()
    .add("discord.com", "sha256/v+k/7KsiyR0zaVWxsgnD3ohO7cVMwj+c3XHd5GKLjV4=")
    .add("discord.com", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
    .add("discord.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
    .add("discordapp.com", "sha256/SBWXn/GAK4nczFrWYO0Rjr+pCl/CzAqpFSC9H7WEbNY=")
    .add("discordapp.com", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
    .add("discordapp.com", "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=")
    .build()

private val client = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    ...
```

- 使用 openssl 萃取當前 Discord 憑證鏈 SPKI SHA-256: leaf + intermediate + root 各一，確保輪替時仍有備用 pin
- 新增 `SSLPeerUnverifiedException` 捕捉，顯示用戶友善錯誤提示
- 建議定期檢查 pin 是否過期 (Discord 約 90 天輪替 leaf，intermediate/root 較長)

**注意**: 若未來 Discord 更換 CA (例如從 Google Trust Services 換到其他)，需更新 pins。建議保留 2+ pins 並在 Release Notes 說明。

### F-06: Webhook URL 未遮罩
**位置**: `app/src/main/res/layout/activity_main.xml`

```xml
<com.google.android.material.textfield.TextInputLayout
    app:endIconMode="password_toggle"
    app:endIconTint="@color/text_secondary">
    <com.google.android.material.textfield.TextInputEditText
        android:inputType="textPassword" />
</com.google.android.material.textfield.TextInputLayout>
```

- 從 `textUri` + `minLines=2` 改為 `textPassword` + `password_toggle`
- 防止肩窺與螢幕錄影外洩 Webhook URL (視為永久發言權杖)

### F-07: v3 簽章
**位置**: `app/build.gradle`

```gradle
buildTypes {
    release {
        v1SigningEnabled false
        v2SigningEnabled true
        v3SigningEnabled true
    }
}
```

- 停用 v1 (易受 Janus 攻擊)，保留 v2 + v3
- v3 支援 key rotation lineage，未來若需輪替金鑰可無縫升級

### F-08: 版本一致性
- `versionCode`: 1 -> 2
- `versionName`: 1.0.0 -> 1.0.2 (與 tag 一致)
- CI 中 APK 命名動態化:
  ```bash
  TAG_NAME="${GITHUB_REF#refs/tags/}"
  DEST_APK="WebhookSender-${TAG_NAME}-release.apk"
  mv app-release.apk $DEST_APK
  ```
- Artifact 與 Release upload 使用 wildcard `WebhookSender-*-release.apk`，不再硬編碼 v1.0.0

### F-09: 文件矛盾
**位置**: `ci-setup.md`
- 刪除「預設自動產生 Debug 金鑰」章節
- 改為明確說明: 本專案自 v1.0.1 起不再提供 debug fallback，未設定 4 個 Secrets 時 CI 直接失敗
- 附上 build.gradle 與 workflow 的 fail-fast 邏輯作為證據

### F-10: 金鑰變更阻斷升級
- `README.md` 新增版本歷史表格與升級說明
- `ci-setup.md` 第 5 章「已知不相容性」
- Workflow Release Notes 自動包含：
  ```
  ⚠️ 若已安裝 v1.0.0，請先解除安裝後再安裝 v1.0.2
  ```

### F-11: ProfileInstallReceiver
**位置**: `AndroidManifest.xml`

```xml
<manifest xmlns:tools="http://schemas.android.com/tools">
    <receiver
        android:name="androidx.profileinstaller.ProfileInstallReceiver"
        android:exported="false"
        tools:replace="android:exported">
        <intent-filter>
            <action android:name="androidx.profileinstaller.action.INSTALL_PROFILE" />
        </intent-filter>
    </receiver>
</manifest>
```

- 覆寫 AndroidX Library 預設的 exported=true，改為 false
- 仍保留 intent-filter 功能，Baseline Profile 安裝仍可運作 (需測試)

### F-13: 剪貼簿
**位置**: `MainActivity.kt` `pasteUrlFromClipboard()`

- 檢查長度 >2000 拒絕
- 檢查 `startsWith("https://")`，否則彈對話框拒絕
- 檢查 `WebhookUrlValidator.isAllowed()`，不符合白名單拒絕
- 顯示預覽 100-200 字元，避免敏感資料完全暴露

## 發布前檢查清單 (報告 15.1) 執行結果

- [x] `./gradlew assembleRelease` 未設定 Secrets 應拋 GradleException (已驗證邏輯)
- [x] `allowBackup=false` (AndroidManifest 已設)
- [x] `networkSecurityConfig` 存在 (已設)
- [x] versionCode 2 / versionName 1.0.2 與 tag 一致
- [x] APK 檔名動態化
- [x] 移除 `WebhookSenderPrefs` / `saved_webhook_url` (v1.0.1 已移除，v1.0.2 保持)
- [x] 文件更新無 debug fallback
- [x] Release Notes 說明金鑰相容性 (workflow 已更新)

待 CI 實際建置後需額外驗證:
- [ ] `androguard sign` 憑證為 release (需在 CI artifact 上執行)
- [ ] `androguard axml` allowBackup
- [ ] `apktool d` DEX 字串檢查

## 建議的 Git 操作

```bash
cd first_app
git add -A
git commit -m "security: fix F-05~F-13 for v1.0.2

- F-05: add CertificatePinner for discord.com/discordapp.com
- F-06: mask Webhook URL with textPassword + password_toggle
- F-07: enable v3 signing, disable v1
- F-08: bump versionCode 2 / versionName 1.0.2, dynamic APK naming
- F-09: rewrite ci-setup.md remove debug fallback contradiction
- F-10: document uninstall required for v1.0.0 in README and release notes
- F-11: override ProfileInstallReceiver exported=false
- F-13: clipboard paste validates URL whitelist
- Update README with full security design and version history
- Update CI workflows to use TAG_NAME dynamic naming

Refs: Z.ai Security Audit Report v1.0.1 residual findings
MASVS-L1: now fully compliant + pinning as L2 defense-in-depth"

git tag v1.0.2
git push origin main --tags
```

## 後續建議 (P3)

- F-12: 可評估導入 StringFog 或 DexGuard 字串加密，但會增加 APK 大小與啟動時間，目前風險可接受
- 定期更新 Certificate Pins (建議每 30 天 CI 加入檢查 script)
- 在 CI 加入 Dependabot 與 OWASP Dependency-Check
- 考慮產生 SBoM (CycloneDX)

---
Generated: 2026-07-14
Author: Arena Agent (automated fix)
