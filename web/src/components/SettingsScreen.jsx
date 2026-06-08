import React, { useRef } from 'react';
import { RefreshCw, Eye, Database, Download, Upload } from 'lucide-react';

export default function SettingsScreen({ theme, onThemeChange, records, onImportData, onClearData }) {
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

  const fileInputRef = useRef(null);

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
        
        // 簡単な構造チェック
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
        
        {/* 隠しインプット */}
        <input 
          type="file" 
          ref={fileInputRef} 
          style={{ display: 'none' }} 
          accept=".json" 
          onChange={handleFileChange} 
        />
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

      {/* バージョン情報 */}
      <div style={{ textAlign: 'center', marginTop: '32px', fontSize: '11px', color: 'var(--text-secondary)' }}>
        <p>MokuMetrics v1.0.0 (Web Client)</p>
        <p style={{ marginTop: '4px' }}>「また吸っちまった」を価値あるデータに変えるダッシュボード</p>
      </div>
    </div>
  );
}

