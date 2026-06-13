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
>   - 両プラットフォームで同様の「パルス記録ボタン」「喫煙間隔のばらつき分布グラフ（スタックバー方式）」「曜日×時間帯ヒートマップ（深夜・朝・昼・夜の4区分）」を提供します。
>   - テーマ切り替え機能（Aurora Green、Dark Neon、Cyberpunk）は、**すべてのテーマを最初から自由に選択・切り替え可能**とします。
> - **データのインポート・エクスポートおよび一括削除**:
>   - 機種変更やブラウザ移行に対応するため、全喫煙記録データをJSON形式の文字列として**エクスポート（コピー）**、およびその文字列を貼り付けて**インポート（復元）**するバックアップ機能を備えます。
>   - 誤入力を想定し、履歴画面からの個別データ削除、および設定画面から確認ダイアログを経ての**全データ一括削除（リセット）**機能を両環境に実装します。
> - **Gemini APIによるインサイト・分析・一言メッセージの動的生成**:
>   - 設定画面からユーザーが自身の「Gemini API キー」を入力・保存できるフォームを提供します。キーはローカル（Web版は LocalStorage、Android版は SharedPreferences）に保存され、外部送信されません。
>   - 「吸っちまった（記録）」ボタン押下時に、バックグラウンド（非同期スレッド）でGemini APIを呼び出します。記録完了などのUI更新は一切待たせず即座に行います。
>   - APIアクセスを抑制するため、最後の成功した更新から **4時間以上** 経過している場合のみバックグラウンドAPI更新を実行します。
>   - APIキーが設定されていない場合やエラー時には、アプリに組み込まれた静的ロジック（静的インサイト、静的一言リスト）へ安全にフォールバックします。
>   - **アドバイザーキャラクターの選択機能**:
>     - 設定画面から「フランクなおっちゃん (uncle)」「ツンデレ秘書 (tsundere)」「明るくフランクなギャル (gal)」の3つのキャラクターからアドバイザーを選択可能です。
>     - キャラクターの切り替え時は、即座に次の記録時に新しいキャラクターからのアドバイスが受け取れるよう、最終更新日時キャッシュをクリアします。

---

## プロジェクト構成（フォルダ構成）

```text
.
├── android/            # Androidプロジェクト一式
│   ├── app/
│   │   ├── src/main/java/com/example/mokumetrics/...
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── web/                # Webプロジェクト一式
    ├── src/
    │   ├── components/ # 各種画面UIコンポーネント
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
  - AndroidアプリおよびRoom Databaseの依存関係を追加します。
* **マニフェストとリソース**: `app/src/main/AndroidManifest.xml`, `strings.xml`
  - アプリの起動構成およびテーマ・文言リソースを設定します。
* **純粋ロジック層（ドメイン）**:
  - `SmokeRecord.kt`: 個別の喫煙履歴（ID、タイムスタンプ、メモ）のモデル定義。
  - `SmokeRepository.kt`: データアクセスのインターフェース定義。
  - `SmokeAnalytics.kt`: 曜日別トレンド、喫煙間隔の統計集計ロジック、およびGemini APIの非同期リクエストと保存処理。
* **データ層 (Room)**:
  - `RoomSmokeRecord.kt`: RoomのEntity定義。
  - `SmokeDao.kt`: クエリの定義（取得・挿入・更新・一件削除・全件削除）。
  - `SmokeDatabase.kt`: データベース定義。
  - `RoomSmokeRepositoryImpl.kt`: リポジトリインターフェースの実装。JSONのインポート・エクスポートデータ処理も含みます。
* **UI/画面 (Compose)**:
  - `MainActivity.kt`: アプリの土台となるScaffold構成（ボトムナビゲーション管理）、JSONデータ共有用のダイアログのステート管理、ViewModelとUI画面の接続。
  - `MainViewModel.kt`: アプリ全体のステート管理とRoomに対する入出力処理、およびGemini APIキー・LLM生成テキスト（インサイト・分析・一言）のState保持とレコード追加時の非同期更新指示。
  - `Theme.kt`: カスタム配色定義（Aurora, Neon, Cyberpunk）。
  - `HomeScreen.kt`: パルス記録ボタン、経過時間タイマー、スマートインサイト（Gemini API優先表示、未設定時はローカルフォールバック）、および記録完了時の一言（Gemini生成一言のランダム表示）。
  - `StatsScreen.kt`: 曜日別棒グラフ、喫煙間隔のばらつき分布、曜日×時間帯ヒートマップ、およびパターン分析（Gemini API優先表示、未設定時はローカルフォールバック）。
  - `HistoryScreen.kt`: カレンダー日付選択、手動追加フォーム、履歴一覧（メモ編集、1件削除機能付き）。
  - `SettingsScreen.kt`: テーマの切り替え、JSONデータインポート・エクスポート、確認アラート付き全データ削除ボタンに加え、サブタブ方式で「設定・APIキー（更新日時表示付き）」と「生成された一言（更新日時・メッセージ一覧表示）」を切り替え表示する画面。
* **ウィジェット**:
  - `SmokeWidgetReceiver.kt`: ホーム画面から即座にワンタップで記録するためのウィジェット機能の実装。

---

### 【Webプロジェクト】 `/web`
Vite + React で構成し、バニラCSSで美しいダークモード、ネオン、オーロラグリーンのテーマを作ります。

#### [NEW] [package.json](web/package.json)
Vite、React、Lucide-React（アイコン用）の依存関係。

#### [NEW] [index.html](web/index.html)
メタデータ、フォント（Google Font `Outfit`）の読み込み。

#### [NEW] [vite.config.js](web/vite.config.js)
Viteビルド構成。

#### [NEW] [index.css](web/src/index.css)
CSS変数を用いたテーマ定義（Aurora Green、Dark Neon、Cyberpunk）と、脈打つアニメーション、ガラスモーフィズム（Glassmorphism）のスタイル設計。

#### [NEW] [smokeAnalytics.js](web/src/utils/smokeAnalytics.js)
Android版の `SmokeAnalytics.kt` と一対一で対応する、純粋なJavaScriptによる統計集計・スマートインサイト生成、喫煙間隔の分布集計、および曜日×4時間帯ヒートマップ集計ロジック。また、Gemini APIの非同期呼出・JSON結果のLocalStorage保存ロジックを含みます。

#### [NEW] [App.jsx](web/src/App.jsx)
State管理（LocalStorage同期、テーマ、APIキー）、ナビゲーションバー（ホーム、統計、履歴、設定）、各画面コンポーネントの統合、インポート・エクスポートデータのやり取り管理、および記録追加時のGemini API非同期呼出（非ブロッキング）。

#### [NEW] [HomeScreen.jsx](web/src/components/HomeScreen.jsx)
パルス記録ボタン、前回喫煙からの経過タイマー（毎秒更新）、今日の合計、スマートインサイト（Gemini API優先表示、未設定時はローカルフォールバック）、および記録完了時の一言（Gemini生成一言のランダム表示）。

#### [NEW] [StatsScreen.jsx](web/src/components/StatsScreen.jsx)
曜日別本数のCSS棒グラフ、喫煙間隔のばらつき分布（スタックバーによる割合グラフ）、および曜日×時間帯ヒートマップ（深夜・朝・昼・夜の4区分）、パターン分析（Gemini API優先表示、未設定時はローカルフォールバック）。

#### [NEW] [HistoryScreen.jsx](web/src/components/HistoryScreen.jsx)
タイムライン表示。各記録の個別削除（ゴミ箱ボタン）、メモのインライン編集・保存、手動追加（カレンダー/時間ピッカー）。

#### [NEW] [SettingsScreen.jsx](web/src/components/SettingsScreen.jsx)
テーマ選択設定、テキストエリアを使ったJSONのインポート/エクスポートUI、警告アラートを挟むLocalStorageの一括リセット機能、およびサブタブ方式で「設定・APIキー（更新日時表示付き）」と「生成された一言（更新日時・メッセージ一覧表示）」を切り替え表示する画面。

---

## 検証計画

### Android版の検証
* Android Studioでプロジェクトを同期し、エミュレータまたは実機でデプロイして動作を確認します。
* または、Gradleビルドコマンド `./gradlew assembleDebug` でビルドを行います。
* 喫煙の追加・編集・個別削除・全削除が Room データベースに永続化されることを確認します。
* エクスポートしたJSONデータをコピーし、一度データをリセットした後にインポートして完全にデータが復元できることを確認します。

### Web版の検証
* コマンド `npm run dev` を実行し、ブラウザで起動します。
* レスポンシブデザイン（モバイルサイズで最も美しく見えるUI）であることを確認します。
* 喫煙の追加・編集・個別削除・一括リセットした際にLocalStorageへデータが反映されることを確認します。
* テキストエリアによるJSONのコピー＆ペーストでのインポート・エクスポートが正しく動作することを確認します。
