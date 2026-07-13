# CI/CD 設定說明

由於 GitHub App 權限限制，無法自動建立 GitHub Actions 工作流程檔案。
請按照以下步驟手動設定：

## 步驟

1. 前往 GitHub 儲存庫頁面
2. 點擊 **Add file** → **Create new file**
3. 檔案路徑輸入：`.github/workflows/build-and-release.yml`
4. 將下方的 YAML 內容貼入
5. 點擊 **Commit changes**

設定完成後，每次推送到 `main` 分支時，GitHub Actions 會自動建置 APK 並建立 Release。

## 工作流程內容

```yaml
name: Build and Release APK

on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android SDK components
        run: |
          sdkmanager --install "platforms;android-34"
          sdkmanager --install "build-tools;34.0.0"
          sdkmanager --install "platform-tools"
          yes | sdkmanager --licenses || true

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Generate Gradle Wrapper
        run: |
          if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
            curl -sL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle.zip
            unzip -q /tmp/gradle.zip -d /tmp/
            /tmp/gradle-8.5/bin/gradle wrapper --gradle-version 8.5
          fi

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Release APK
        run: ./gradlew assembleRelease --no-daemon

      - name: Get version info
        id: version
        run: |
          VERSION=$(grep 'versionName' app/build.gradle | awk -F'"' '{print $2}')
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "build_number=$GITHUB_RUN_NUMBER" >> $GITHUB_OUTPUT

      - name: Upload APK as artifact
        uses: actions/upload-artifact@v4
        with:
          name: webhook-sender-apk
          path: app/build/outputs/apk/release/*.apk

      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ steps.version.outputs.version }}-${{ steps.version.outputs.build_number }}
          name: Webhook Sender v${{ steps.version.outputs.version }} (Build ${{ steps.version.outputs.build_number }})
          body: |
            ## 🪝 Discord Webhook Sender

            ### 功能
            - 📋 貼上 Webhook URL
            - 👤 自訂使用者名稱與頭像
            - 💬 發送訊息
            - 📎 支援 Embed（嵌入訊息）
              - 標題、描述、顏色
              - 圖片、縮圖
              - 頁尾 (Footer)
              - 作者 (Author)
              - 自訂欄位 (Fields)

            ### 安裝方式
            1. 下載 APK 檔案
            2. 在 Android 手機上開啟並安裝
            3. 如提示「未知來源」，請在設定中允許安裝
          files: app/build/outputs/apk/release/*.apk
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## 手動觸發建置

工作流程也支援手動觸發：
1. 前往 **Actions** 頁面
2. 選擇 **Build and Release APK** 工作流程
3. 點擊 **Run workflow**
