import React, { useState } from 'react';
import { Calendar, Trash2, Edit2, Check, X, Plus } from 'lucide-react';

export default function HistoryScreen({ records, onAddRecord, onDeleteRecord, onUpdateRecord }) {
  const [editingId, setEditingId] = useState(null);
  const [editMemo, setEditMemo] = useState('');
  
  // 手動追加フォーム用
  const [showAddForm, setShowAddForm] = useState(false);
  const [manualDate, setManualDate] = useState(new Date().toISOString().split('T')[0]);
  const [manualTime, setManualTime] = useState(new Date().toTimeString().slice(0, 5));
  const [manualMemo, setManualMemo] = useState('');

  // タイムスタンプ順（降順）にソートしてコピー
  const sortedRecords = [...records].sort((a, b) => b.timestamp - a.timestamp);

  const startEdit = (record) => {
    setEditingId(record.id);
    setEditMemo(record.memo || '');
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditMemo('');
  };

  const saveEdit = (id) => {
    onUpdateRecord(id, editMemo.trim());
    setEditingId(null);
    setEditMemo('');
  };

  const handleManualSubmit = (e) => {
    e.preventDefault();
    if (!manualDate || !manualTime) return;

    // 日付と時間から Date オブジェクトを作成
    const datetimeStr = `${manualDate}T${manualTime}`;
    const timestamp = new Date(datetimeStr).getTime();

    if (isNaN(timestamp)) {
      alert("無効な日時が指定されました。");
      return;
    }

    onAddRecord(timestamp, manualMemo.trim());
    
    // フォームをリセット
    setManualMemo('');
    setShowAddForm(false);
  };

  const formatDate = (timestamp) => {
    const d = new Date(timestamp);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const date = String(d.getDate()).padStart(2, '0');
    const day = ["日", "月", "火", "水", "木", "金", "土"][d.getDay()];
    return `${y}/${m}/${date} (${day})`;
  };

  const formatTime = (timestamp) => {
    const d = new Date(timestamp);
    const h = String(d.getHours()).padStart(2, '0');
    const m = String(d.getMinutes()).padStart(2, '0');
    return `${h}:${m}`;
  };

  return (
    <div className="history-screen">
      {/* 手動追加の切り替えボタン */}
      <button 
        className="btn btn-secondary" 
        style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
        onClick={() => setShowAddForm(!showAddForm)}
      >
        <Plus size={16} /> {showAddForm ? '閉じる' : '過去の喫煙を手動で追加'}
      </button>

      {/* 手動追加フォーム */}
      {showAddForm && (
        <form className="glass-card" onSubmit={handleManualSubmit}>
          <h4 style={{ fontSize: '14px', fontWeight: 'bold', marginBottom: '12px' }}>過去データを追加</h4>
          
          <div className="form-group">
            <label className="form-label">日付</label>
            <input 
              type="date" 
              className="form-input" 
              value={manualDate} 
              onChange={(e) => setManualDate(e.target.value)} 
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">時間</label>
            <input 
              type="time" 
              className="form-input" 
              value={manualTime} 
              onChange={(e) => setManualTime(e.target.value)} 
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">メモ (状況・理由など)</label>
            <input 
              type="text" 
              className="form-input" 
              placeholder="例: 会食中、イライラしたため"
              value={manualMemo} 
              onChange={(e) => setManualMemo(e.target.value)}
            />
          </div>

          <button type="submit" className="btn">追加する</button>
        </form>
      )}

      {/* 履歴リスト */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Calendar size={16} /> タイムライン履歴 ({records.length}件)
        </h4>

        {sortedRecords.length === 0 ? (
          <p style={{ textAlign: 'center', fontSize: '13px', color: 'var(--text-secondary)', padding: '20px 0' }}>
            喫煙データがまだありません。
          </p>
        ) : (
          <div className="history-list">
            {sortedRecords.map((record) => (
              <div key={record.id} className="history-item">
                <div style={{ flex: 1, paddingRight: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                    <span className="history-time">{formatTime(record.timestamp)}</span>
                    <span className="history-date">{formatDate(record.timestamp)}</span>
                  </div>

                  {editingId === record.id ? (
                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px', alignItems: 'center' }}>
                      <input
                        type="text"
                        className="form-input"
                        value={editMemo}
                        onChange={(e) => setEditMemo(e.target.value)}
                        style={{ padding: '6px 10px', fontSize: '12px' }}
                      />
                      <button className="icon-btn" onClick={() => saveEdit(record.id)} title="保存">
                        <Check size={16} style={{ color: 'var(--color-accent)' }} />
                      </button>
                      <button className="icon-btn" onClick={cancelEdit} title="キャンセル">
                        <X size={16} />
                      </button>
                    </div>
                  ) : (
                    record.memo && <div className="history-memo">「{record.memo}」</div>
                  )}
                </div>

                {/* 操作アクション */}
                {editingId !== record.id && (
                  <div className="history-actions">
                    <button className="icon-btn" onClick={() => startEdit(record)} title="メモの編集">
                      <Edit2 size={14} />
                    </button>
                    <button className="icon-btn delete" onClick={() => onDeleteRecord(record.id)} title="削除">
                      <Trash2 size={14} />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
