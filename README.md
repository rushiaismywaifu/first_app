# 🪝 Discord Webhook Sender

一個簡易的 Android App，用於透過 Discord Webhook 發送訊息。

## ✨ 功能

- 🔗 **Webhook URL** - 貼上你的 Discord Webhook URL
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

## 📱 截圖

> 使用 Discord Blurple 配色的深色主題界面

## 🔧 技術

- **語言**: Kotlin
- **最低 SDK**: Android 7.0 (API 24)
- **目標 SDK**: Android 14 (API 34)
- **HTTP 客戶端**: OkHttp
- **JSON 序列化**: Gson
- **UI**: Material Design Components

## 📦 下載

前往 [Releases](../../releases) 頁面下載最新版 APK。

## 🚀 使用方式

1. 在 Discord 頻道設定中建立 Webhook，複製 Webhook URL
2. 開啟 App，貼上 Webhook URL
3. （可選）設定使用者名稱和頭像 URL
4. 輸入訊息內容
5. 如需 Embed，開啟「啟用 Embed」並填寫相關欄位
6. 點擊「🚀 發送」按鈕

## 📝 注意事項

- 此 App 僅用於發送 Webhook 訊息，不會儲存任何個人資料
- Webhook URL 請妥善保管，避免洩漏
- 支援 `discord.com` 和 `discordapp.com` 的 Webhook URL
