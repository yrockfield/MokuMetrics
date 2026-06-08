import React from 'react';
import { Settings, RefreshCw, Eye } from 'lucide-react';

export default function SettingsScreen({ theme, onThemeChange, onClearData }) {
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
      description: '近未来を漂わせる紫とシアンのサイバーダークテーマ',
      color: '#8b5cf6'
    },
    {
      id: 'cyberpunk',
      name: 'Cyberpunk (サイバーパンク)',
      description: 'ビビッドな黄色とマゼンタピンクが映える漆黒テーマ',
      color: '#facc15'
    }
  ];

  const handleReset = () => {
    const confirmReset = window.confirm("これまでのすべての喫煙記録が消去されます。よろしいですか？");
    if (confirmReset) {
      onClearData();
    }
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

      {/* データ管理 */}
      <div className="glass-card" style={{ borderLeft: '4px solid var(--color-danger)' }}>
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-danger)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <RefreshCw size={16} /> データの管理
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
