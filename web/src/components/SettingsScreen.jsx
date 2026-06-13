import React, { useState, useEffect, useRef } from 'react';
import { RefreshCw, Eye, Database, Download, Upload, Key, MessageSquare, Settings, User } from 'lucide-react';

export default function SettingsScreen({ theme, onThemeChange, records, onImportData, onClearData, apiKey, onApiKeyChange, character, onCharacterChange }) {
  const [activeSubTab, setActiveSubTab] = useState('general');
  const [lastUpdateTime, setLastUpdateTime] = useState(() => localStorage.getItem("llm_last_insight_update_time"));
  const [oneLiners, setOneLiners] = useState(() => {
    const raw = localStorage.getItem("llm_oneliners");
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });

  const themesList = [
    {
      id: 'aurora',
      name: 'Aurora Green (オーロラ)',
      description: '神秘的なグリーンとダークネオンの癒やし系テーマ',
      color: '#10b981'
    },
    {
      id: 'neon',
      name: 'Dark Neon (ダークネオン)',
      description: '近未来を漂わせる紫とシアン of the year的サイバーダークテーマ',
      color: '#8b5cf6'
    },
    {
      id: 'cyberpunk',
      name: 'Cyberpunk (サイバーパンク)',
      description: 'ビビッドな黄色とマゼンタピンクが映える漆黒テーマ',
      color: '#facc15'
    }
  ];

  const charactersList = [
    {
      id: 'uncle',
      name: 'フランクなおっちゃん (おっちゃん)',
      description: 'ぶっきらぼうだけど人情味あふれる焼き鳥屋のオヤジ。関西弁混じりで時に厳しく、時に優しく励ましてくれる。',
      emoji: '🍢'
    },
    {
      id: 'tsundere',
      name: 'ツンデレ秘書 (秘書)',
      description: 'クールで丁寧な敬語を使う優秀なアシスタント。冷静に分析しつつ、心の中ではあなたの体を本気で心配している。',
      emoji: '💼'
    },
    {
      id: 'gal',
      name: '明るくフランクなギャル (ギャル)',
      description: '超ポジティブで明るいフランクな女性キャラ。フランクな口調であなたの喫煙記録や禁煙の努力を全力で肯定・応援してくれる。',
      emoji: '💅'
    }
  ];

  const fileInputRef = useRef(null);

  useEffect(() => {
    const handleLlmUpdate = () => {
      setLastUpdateTime(localStorage.getItem("llm_last_insight_update_time"));
      const raw = localStorage.getItem("llm_oneliners");
      try {
        setOneLiners(raw ? JSON.parse(raw) : null);
      } catch {
        setOneLiners(null);
      }
    };
    window.addEventListener("llm_data_updated", handleLlmUpdate);
    return () => window.removeEventListener("llm_data_updated", handleLlmUpdate);
  }, []);

  const formatDatetime = (ts) => {
    if (!ts) return "未更新";
    try {
      const date = new Date(parseInt(ts, 10));
      return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
    } catch {
      return "エラー";
    }
  };

  const handleReset = () => {
    const confirmReset = window.confirm("これまでのすべての喫煙記録が消去されます。よろしいですか？");
    if (confirmReset) {
      onClearData();
    }
  };

  const handleExport = () => {
    try {
      const blob = new Blob([JSON.stringify(records, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const downloadAnchor = document.createElement('a');
      downloadAnchor.href = url;
      downloadAnchor.download = `mokumetrics_data_${Date.now()}.json`;
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert("エクスポート中にエラーが発生しました: " + err.message);
    }
  };

  const handleImportClick = () => {
    fileInputRef.current.click();
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const confirmImport = window.confirm("データをインポートすると、現在のすべての記録が上書きされます。よろしいですか？");
    if (!confirmImport) {
      e.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const imported = JSON.parse(event.target.result);
        if (!Array.isArray(imported)) {
          throw new Error("データが配列形式ではありません。");
        }
        
        const isValid = imported.every(r => r && typeof r.timestamp === 'number');
        if (!isValid) {
          throw new Error("一部のデータに必要な情報（タイムスタンプなど）が含まれていません。");
        }

        onImportData(imported);
        alert("インポートが完了しました！");
      } catch (err) {
        alert("エラー: 正しい形式のバックアップファイルではありません。\n" + err.message);
      }
      e.target.value = '';
    };
    reader.readAsText(file);
  };

  return (
    <div className="settings-screen">
      {/* サブタブ */}
      <div className="sub-tab-container">
        <button 
          className={`sub-tab-button ${activeSubTab === 'general' ? 'active' : ''}`} 
          onClick={() => setActiveSubTab('general')}
        >
          <Settings size={14} /> 設定・APIキー
        </button>
        <button 
          className={`sub-tab-button ${activeSubTab === 'oneliners' ? 'active' : ''}`} 
          onClick={() => setActiveSubTab('oneliners')}
        >
          <MessageSquare size={14} /> 生成された一言
        </button>
      </div>

      {activeSubTab === 'general' ? (
        <>
          {/* テーマ選択 */}
          <div className="glass-card">
            <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Eye size={16} /> テーマの選択
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              アプリの視覚デザインを変更します。すべてのテーマが最初から選択可能です。
            </p>

            <div className="theme-selector">
              {themesList.map((t) => (
                <div
                  key={t.id}
                  className={`theme-option ${theme === t.id ? 'selected' : ''}`}
                  onClick={() => onThemeChange(t.id)}
                >
                  <div>
                    <span className="theme-name" style={{ color: theme === t.id ? 'var(--color-accent)' : 'var(--text-primary)' }}>
                      {t.name}
                    </span>
                    <p style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                      {t.description}
                    </p>
                  </div>
                  <div
                    className="theme-color-preview"
                    style={{ backgroundColor: t.color, boxShadow: theme === t.id ? `0 0 10px ${t.color}` : 'none' }}
                  ></div>
                </div>
              ))}
            </div>
          </div>

          {/* データの管理 */}
          <div className="glass-card">
            <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Database size={16} /> データの管理
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              喫煙履歴データのバックアップ（エクスポート）や、過去のバックアップデータの復元（インポート）が行えます。
            </p>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button className="btn btn-secondary" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }} onClick={handleExport}>
                <Download size={16} /> エクスポート
              </button>
              <button className="btn btn-secondary" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px' }} onClick={handleImportClick}>
                <Upload size={16} /> インポート
              </button>
            </div>
            
            <input 
              type="file" 
              ref={fileInputRef} 
              style={{ display: 'none' }} 
              accept=".json" 
              onChange={handleFileChange} 
            />
          </div>
          {/* アドバイザーキャラクターの選択 */}
          <div className="glass-card">
            <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <User size={16} /> アドバイザーキャラクターの選択
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              分析アドバイスや一言メッセージのキャラクターを変更します。変更すると次回の喫煙記録時に即座に新しい内容が反映されます。
            </p>

            <div className="theme-selector" style={{ marginTop: '16px' }}>
              {charactersList.map((c) => (
                <div
                  key={c.id}
                  className={`theme-option ${character === c.id ? 'selected' : ''}`}
                  onClick={() => onCharacterChange(c.id)}
                >
                  <div>
                    <span className="theme-name" style={{ color: character === c.id ? 'var(--color-accent)' : 'var(--text-primary)' }}>
                      {c.name}
                    </span>
                    <p style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                      {c.description}
                    </p>
                  </div>
                  <div
                    style={{ fontSize: '24px', opacity: character === c.id ? 1 : 0.5, transition: 'opacity 0.2s' }}
                  >
                    {c.emoji}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Gemini API 設定 */}
          <div className="glass-card">
            <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Key size={16} /> Gemini API の設定
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              Gemini API キーを設定すると、喫煙履歴に基づいた高度なスマートインサイト、パターン分析、一言メッセージを自動生成できます。
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                <input 
                  type="password" 
                  placeholder="Gemini API キーを入力してください"
                  value={apiKey || ''}
                  onChange={(e) => onApiKeyChange(e.target.value)}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    backgroundColor: 'rgba(255, 255, 255, 0.05)',
                    color: 'var(--text-primary)',
                    fontSize: '13px',
                    outline: 'none'
                  }}
                />
              </div>
              <p style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
                ※ APIキーはブラウザの LocalStorage に安全にローカル保存されます。外部のサーバーへ送信されることはありません。
              </p>
            </div>
          </div>

          {/* データの削除 */}
          <div className="glass-card" style={{ borderLeft: '4px solid var(--color-danger)' }}>
            <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-danger)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <RefreshCw size={16} /> データの削除
            </h4>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              LocalStorage に保存されているすべての喫煙履歴をリセットして初期状態に戻します。この操作は取り消せません。
            </p>

            <button className="btn btn-danger" onClick={handleReset}>
              全データを消去してリセット
            </button>
          </div>
        </>
      ) : (
        /* 生成された一言メッセージリスト */
        <div className="glass-card">
          <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MessageSquare size={16} /> 生成された一言メッセージリスト
          </h4>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            Gemini APIによって自動生成され、喫煙記録時の一言メッセージ（トースト）として使用されるカスタムメッセージのリストです。
          </p>

          <div style={{ background: 'rgba(255,255,255,0.03)', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--border-card)', marginBottom: '20px', fontSize: '11px', color: 'var(--text-secondary)' }}>
            一言リスト最終更新: <strong style={{ color: 'var(--color-accent)' }}>{formatDatetime(lastUpdateTime)}</strong>
          </div>

          {oneLiners && oneLiners.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {oneLiners.map((msg, idx) => (
                <div 
                  key={idx} 
                  style={{ 
                    padding: '12px 16px', 
                    borderRadius: '10px', 
                    background: 'rgba(255, 255, 255, 0.02)', 
                    border: '1px solid rgba(255, 255, 255, 0.05)',
                    fontSize: '13px',
                    lineHeight: '1.4',
                    display: 'flex',
                    gap: '12px',
                    alignItems: 'center'
                  }}
                >
                  <span style={{ color: 'var(--color-accent)', fontWeight: 'bold' }}>#{idx + 1}</span>
                  <span style={{ color: 'var(--text-primary)' }}>{msg}</span>
                </div>
              ))}
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: '32px 16px', color: 'var(--text-secondary)', fontSize: '13px' }}>
              まだ一言リストは生成されていません。Gemini APIキーを設定した状態で「吸っちまった」ボタンをタップすると、バックグラウンドで自動生成されます。
            </div>
          )}
        </div>
      )}

      {/* バージョン情報 */}
      <div style={{ textAlign: 'center', marginTop: '32px', fontSize: '11px', color: 'var(--text-secondary)' }}>
        <p>MokuMetrics v1.0.0 (Web Client)</p>
        <p style={{ marginTop: '4px' }}>「また吸っちまった」を価値あるデータに変えるダッシュボード</p>
      </div>
    </div>
  );
}
