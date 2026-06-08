# MokuMetrics 🚬

> **「また吸っちまった」のダッシュボード**

MokuMetrics（モクメトリクス）は、喫煙の本数とタイミングを「恐れず、しかし客観的に」記録・分析し、自然な喫煙コントロールを支援するアプリケーションです。
本リポジトリには、 **Webアプリ（ブラウザ版）** と **Androidアプリ（ネイティブ版）** のソースコードが同梱されています。

---

## 🌟 主な機能

* **パルス記録ボタン**: 脈打つネオンエフェクト付きのボタンで、吸った瞬間にワンタップで記録。状況メモ（任意）も追加可能。
* **経過時間タイマー**: 前回吸ってからの経過時間をリアルタイムにカウント。
* **曜日×時間帯ヒートマップ (GitHub風)**: どの曜日のどの時間帯に多く吸ってしまっているかをカラー温度で可視化。
* **曜日別トレンドグラフ**: 曜日ごとの合計喫煙本数を比較する棒グラフ。
* **スマートインサイト**: 蓄積されたデータから「魔の時間帯」や前日との比較など、AIライクな行動アドバイスを自動生成。
* **統一されたデザインシステム**: 3つのネオンテーマを最初から自由に切り替え可能。
  1. **Aurora Green (オーロラ)**: 深いフォレストグリーンとネオングリーンの癒やし系テーマ
  2. **Dark Neon (ダークネオン)**: 近未来感のある紫とシアンのサイバーダークテーマ
  3. **Cyberpunk (サイバーパンク)**: イエローとマゼンタピンクが映える漆黒テーマ
* **完全ローカル永続化**: 
  - Web版：ブラウザの `LocalStorage` を使用。
  - Android版：ローカルの `Room Database` を使用。
  - 外部サーバーへのデータ送信はなく、完全にオフラインで動作します。
* **Android ホーム画面ウィジェット**: アプリを開かずに、ホーム画面のウィジェットから今日の喫煙本数を確認し、ワンタップで「吸っちまった」記録が可能。

---

## 📁 プロジェクト構造

```text
MokuMetrics/
├── android/            # Android Studio プロジェクト（Kotlin + Jetpack Compose）
│   ├── app/
│   │   ├── src/main/java/com/example/mokumetrics/... # アプリロジック・UI
│   │   └── src/main/res/...                          # リソース・ウィジェットレイアウト
│   └── build.gradle.kts
├── web/                # Webフロントエンドプロジェクト（Vite + React）
│   ├── src/
│   │   ├── components/ # 各画面コンポーネント (Home, Stats, History, Settings)
│   │   ├── utils/      # 統計・インサイトロジック (smokeAnalytics.js)
│   │   └── App.jsx
│   ├── index.html
│   └── package.json
├── LICENSE             # Apache License 2.0
└── README.md           # 本ファイル
```

---

## 🚀 動作方法

### 1. Webアプリ (ブラウザ版)

Vite 開発サーバーを使用して起動します。

```bash
# web ディレクトリへ移動
cd web

# 依存関係のインストール
npm install

# 開発用ローカルサーバーの起動 (http://localhost:5173 で開きます)
npm run dev

# プロダクションビルドの実行
npm run build
```

### 2. Androidアプリ (ネイティブ版)

Android Studio で `android` フォルダをプロジェクトとしてインポートして実行するか、Gradle ラッパーを使用してコマンドラインからビルドします。

```bash
# android ディレクトリへ移動
cd android

# デバッグ用APKのビルド (app/build/outputs/apk/debug/app-debug.apk が生成されます)
./gradlew assembleDebug
```

---

## 📄 ライセンス

本プロジェクトは **[Apache License 2.0](LICENSE)** のもとでオープンソースとして公開されています。
