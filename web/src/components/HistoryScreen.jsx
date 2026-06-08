import React, { useState, useEffect } from 'react';
import { Calendar, Trash2, Edit2, Check, X, Plus, ChevronLeft, ChevronRight } from 'lucide-react';

export default function HistoryScreen({ records, onAddRecord, onDeleteRecord, onUpdateRecord }) {
  const [editingId, setEditingId] = useState(null);
  const [editMemo, setEditMemo] = useState('');
  
  // 表示対象の日付 (初期値は今日)
  const [displayDate, setDisplayDate] = useState(new Date());

  // 手動追加フォーム用
  const [showAddForm, setShowAddForm] = useState(false);
  const [manualDate, setManualDate] = useState(displayDate.toISOString().split('T')[0]);
  const [manualTime, setManualTime] = useState(new Date().toTimeString().slice(0, 5));
  const [manualMemo, setManualMemo] = useState('');

  // 表示日付が変更されたら、手動追加のデフォルト日付も連動させる
  useEffect(() => {
    // タイムゾーンによる日付のズレを防ぐため、displayDate のローカル日付文字列を抽出
    const year = displayDate.getFullYear();
    const month = String(displayDate.getMonth() + 1).padStart(2, '0');
    const day = String(displayDate.getDate()).padStart(2, '0');
    setManualDate(`${year}-${month}-${day}`);
  }, [displayDate]);

  // 前日・翌日への移動ハンドラ
  const handlePrevDay = () => {
    const d = new Date(displayDate);
    d.setDate(d.getDate() - 1);
    setDisplayDate(d);
  };

  const handleNextDay = () => {
    const d = new Date(displayDate);
    d.setDate(d.getDate() + 1);
    setDisplayDate(d);
  };

  // タイムスタンプ順（降順）にソート
  const sortedRecords = [...records].sort((a, b) => b.timestamp - a.timestamp);

  // 表示中日付に一致するレコードを抽出
  const targetDateStr = displayDate.toDateString();
  const filteredRecords = sortedRecords.filter(
    r => new Date(r.timestamp).toDateString() === targetDateStr
  );

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

  const formatDate = (dateObj) => {
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, '0');
    const date = String(dateObj.getDate()).padStart(2, '0');
    const day = ["日", "月", "火", "水", "木", "金", "土"][dateObj.getDay()];
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
      {/* 1. 日付選択ナビゲーションヘッダー */}
      <div 
        className="glass-card" 
        style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          padding: '12px 16px', 
          marginBottom: '16px' 
        }}
      >
        <button className="icon-btn" onClick={handlePrevDay} title="前日へ" style={{ fontSize: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          ◀
        </button>

        {/* 日付表示と透明な日付インプットを重ねる */}
        <div 
          style={{ 
            position: 'relative', 
            display: 'inline-flex', 
            alignItems: 'center', 
            gap: '8px', 
            fontWeight: 'bold',
            fontSize: '15px',
            color: 'var(--color-accent)',
            cursor: 'pointer'
          }}
        >
          <span>{formatDate(displayDate)}</span>
          <Calendar size={16} />
          
          <input
            type="date"
            value={manualDate}
            onChange={(e) => {
              if (e.target.value) {
                // timezone ズレ回避のため、入力された yyyy-mm-dd をもとに Date を生成
                const [y, m, d] = e.target.value.split('-').map(Number);
                setDisplayDate(new Date(y, m - 1, d));
              }
            }}
            style={{ 
              position: 'absolute', 
              top: 0, 
              left: 0, 
              right: 0, 
              bottom: 0, 
              opacity: 0, 
              cursor: 'pointer', 
              width: '100%' 
            }}
          />
        </div>

        <button className="icon-btn" onClick={handleNextDay} title="翌日へ" style={{ fontSize: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          ▶
        </button>
      </div>

      {/* 2. 手動追加の切り替えボタン */}
      <button 
        className="btn btn-secondary" 
        style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
        onClick={() => setShowAddForm(!showAddForm)}
      >
        <Plus size={16} /> {showAddForm ? '閉じる' : '過去の喫煙を手動で追加'}
      </button>

      {/* 3. 手動追加フォーム */}
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

      {/* 4. 履歴リスト */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Calendar size={16} /> 喫煙履歴 ({filteredRecords.length}件)
        </h4>

        {filteredRecords.length === 0 ? (
          <p style={{ textAlign: 'center', fontSize: '13px', color: 'var(--text-secondary)', padding: '20px 0' }}>
            この日の喫煙データはありません。
          </p>
        ) : (
          <div className="history-list">
            {filteredRecords.map((record) => (
              <div key={record.id} className="history-item">
                <div style={{ flex: 1, paddingRight: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                    <span className="history-time">{formatTime(record.timestamp)}</span>
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
