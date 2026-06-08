# Android & Web 同時開発「MokuMetrics」実装計画

喫煙本数とタイミングを記録するアプリ「MokuMetrics」について、**Androidアプリ（ネイティブ版）**と**Webアプリ（ブラウザ版）**を同時に同じワークスペース内で開発するための実装計画です。

* **アプリ名**: MokuMetrics (モクメトリクス)
* **キャッチコピー**: 「また吸っちまった」のダッシュボード
* **パッケージ名 (Android)**: `com.example.mokumetrics`
* **構成**: 
  - `/android`: Android Studio プロジェクト（Kotlin + Jetpack Compose）
  - `/web`: Webフロントエンドプロジェクト（Vite + React + Vanilla CSS）

---

## ユーザーレビュー要求事項

> [!IMPORTANT]
> - **開発ツールとビルドについて**:
>   - Android側は、PCの **Android Studio** を用いてビルド・実行していただきます。
>   - Web側は、Node.js環境下で `npm run dev` を実行していただくことで、ローカルブラウザ（`http://localhost:5173` 等）で即座に動作確認が可能です。
> - **データストレージの仕様**:
>   - Web版にはサーバーを置かず、ブラウザの **LocalStorage** にデータを永続化します。これにより、Android版の **Room Database** と同様に、完全にローカルかつオフラインで安全に動作します。
> - **統一されたデザインシステム（全テーマ解放）**:
>   - 両プラットフォームで同様の「パルス記録ボタン」「曜日×時間帯ヒートマップ」を提供します。
>   - テーマ切り替え機能（Aurora Green、Dark Neon、Cyberpunk）は、**すべてのテーマを最初から自由に選択・切り替え可能**とします。

---

## プロジェクト構成（フォルダ構成）

```text
c:\Users\yusuke\antigravity
├── android/            # Androidプロジェクト一式
│   ├── app/
│   │   ├── src/main/java/com/example/mokumetrics/...
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── web/                # Webプロジェクト一式
    ├── src/
    │   ├── components/ # 記録ボタン、ヒートマップ等
    │   ├── theme/      # CSSテーマ設定
    │   ├── App.jsx     # メインUI構成
    │   └── main.jsx
    ├── index.html
    ├── package.json
    └── vite.config.js
```

---

## 提案する変更内容（追加・編集ファイル）

### 【Androidプロジェクト】 `/android`
以下のファイルを `/android` 配下に新規作成します。

* **ビルド設定**: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
* **マニフェスト**: `app/src/main/AndroidManifest.xml`, `strings.xml`
* **純粋ロジック層（ドメイン）**: `SmokeRecord.kt`, `SmokeRepository.kt`, `SmokeAnalytics.kt`
* **データ層 (Room)**: `RoomSmokeRecord.kt`, `SmokeDao.kt`, `SmokeDatabase.kt`, `RoomSmokeRepositoryImpl.kt`
* **UI/画面 (Compose)**: `MainActivity.kt`, `MainViewModel.kt`, `Theme.kt`, `HomeScreen.kt`, `StatsScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`
* **ウィジェット**: `SmokeWidgetReceiver.kt`

---

### 【Webプロジェクト】 `/web`
Vite + React で構成し、バニラCSSで美しいダークモード、ネオン、オーロラグリーンのテーマを作ります。

#### [NEW] [package.json](file:///c:/Users/yusuke/antigravity/web/package.json)
Vite、React、Lucide-React（アイコン用）の依存関係。

#### [NEW] [index.html](file:///c:/Users/yusuke/antigravity/web/index.html)
メタデータ、フォント（Google Font `Outfit`）の読み込み。

#### [NEW] [vite.config.js](file:///c:/Users/yusuke/antigravity/web/vite.config.js)
Viteビルド構成。

#### [NEW] [index.css](file:///c:/Users/yusuke/antigravity/web/src/index.css)
CSS変数を用いたテーマ定義（Aurora Green、Dark Neon、Cyberpunk）と、脈打つアニメーション、ガラスモーフィズム（Glassmorphism）のスタイル設計。

#### [NEW] [smokeAnalytics.js](file:///c:/Users/yusuke/antigravity/web/src/utils/smokeAnalytics.js)
Android版の `SmokeAnalytics.kt` と完全に一対一で対応する、純粋なJavaScriptによる統計集計・スマートインサイト生成ロジック。

#### [NEW] [App.jsx](file:///c:/Users/yusuke/antigravity/web/src/App.jsx)
State管理（LocalStorage同期、テーマ）、ナビゲーションバー（ホーム、統計、履歴、設定）、各画面コンポーネントの統合。

#### [NEW] [HomeScreen.jsx](file:///c:/Users/yusuke/antigravity/web/src/components/HomeScreen.jsx)
パルス記録ボタン、前回喫煙からの経過タイマー（毎秒更新）、今日の合計、スマートインサイト。

#### [NEW] [StatsScreen.jsx](file:///c:/Users/yusuke/antigravity/web/src/components/StatsScreen.jsx)
曜日別本数のCSS棒グラフ、時間帯別のカラー温度ヒートマップグリッド。

#### [NEW] [HistoryScreen.jsx](file:///c:/Users/yusuke/antigravity/web/src/components/HistoryScreen.jsx)
タイムライン表示。各記録の削除、メモの編集、手動追加（カレンダー/時間ピッカー）。

#### [NEW] [SettingsScreen.jsx](file:///c:/Users/yusuke/antigravity/web/src/components/SettingsScreen.jsx)
テーマ選択設定、およびデータのリセット機能。

---

## 検証計画

### Android版の検証
* Android Studioでプロジェクトを同期し、エミュレータまたは実機でデプロイして動作を確認します。

### Web版の検証
* コマンド `npm run dev` を実行し、ブラウザで起動します。
* レスポンシブデザイン（モバイルサイズで最も美しく見えるUI）であることを確認します。
* 喫煙の追加、削除、リロードしてもLocalStorageによりデータが保持されることを確認します。
