import React, { useState, useEffect, useRef } from 'react';
import { Flame, MessageSquare, AlertCircle } from 'lucide-react';
import { getTodayCount, getSecondsSinceLastSmoke, formatDuration, generateSmartInsight, getLlmSmartInsight, getLlmOneLiners } from '../utils/smokeAnalytics';
import { smokingMessages } from '../utils/smokingMessages';

export default function HomeScreen({ records, onAddRecord }) {
  const [memo, setMemo] = useState('');
  const [timeNow, setTimeNow] = useState(Date.now());
  const [showPulse, setShowPulse] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);
  const toastTimerRef = useRef(null);

  const [llmInsight, setLlmInsight] = useState(getLlmSmartInsight());
  const [llmUpdateTime, setLlmUpdateTime] = useState(() => localStorage.getItem("llm_last_insight_update_time"));

  // 1秒ごとに現在時刻を更新し、タイマーを進める
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeNow(Date.now());
    }, 1000);

    const handleLlmUpdate = () => {
      setLlmInsight(getLlmSmartInsight());
      setLlmUpdateTime(localStorage.getItem("llm_last_insight_update_time"));
    };
    window.addEventListener("llm_data_updated", handleLlmUpdate);

    return () => {
      clearInterval(timer);
      window.removeEventListener("llm_data_updated", handleLlmUpdate);
      if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    };
  }, []);

  const handleRecord = () => {
    // パルスエフェクトの制御
    setShowPulse(true);
    setTimeout(() => setShowPulse(false), 1000);
    
    onAddRecord(Date.now(), memo.trim());
    setMemo(''); // 記録後にメモをクリア

    // ランダムメッセージの表示
    const customOneLiners = getLlmOneLiners();
    let msg = "";
    if (customOneLiners && customOneLiners.length > 0) {
      const randomIndex = Math.floor(Math.random() * customOneLiners.length);
      msg = customOneLiners[randomIndex];
    } else {
      const randomIndex = Math.floor(Math.random() * smokingMessages.length);
      msg = smokingMessages[randomIndex];
    }
    setToastMessage(msg);

    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current);
    }
    toastTimerRef.current = setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  const todayCount = getTodayCount(records);
  const secondsSinceLast = getSecondsSinceLastSmoke(records, timeNow);
  const formattedDuration = formatDuration(secondsSinceLast);
  const insight = llmInsight || generateSmartInsight(records);

  return (
    <div className="home-screen">
      {/* 経過時間表示 */}
      <div className="glass-card text-center" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '14px', color: 'var(--text-secondary)', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '1px' }}>
          前回吸ってからの経過時間
        </h3>
        <div className="timer-text" style={{ textShadow: secondsSinceLast > 14400 ? '0 0 15px rgba(var(--color-accent-rgb), 0.4)' : 'none' }}>
          {formattedDuration}
        </div>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '8px' }}>
          {secondsSinceLast === null ? 'まだ記録はありません。最初の1本を記録しましょう！' : '冷静な時間が流れています'}
        </p>
      </div>

      {/* 今日の総本数 */}
      <div className="glass-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px' }}>
        <div>
          <span style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>今日の総喫煙本数</span>
          <h2 style={{ fontSize: '28px', fontWeight: '800', marginTop: '4px', color: 'var(--color-accent)' }}>
            {todayCount} <span style={{ fontSize: '14px', fontWeight: 'normal', color: 'var(--text-secondary)' }}>本</span>
          </h2>
        </div>
        <div style={{ background: 'rgba(var(--color-accent-rgb), 0.1)', padding: '12px', borderRadius: '50%' }}>
          <Flame size={24} style={{ color: 'var(--color-accent)' }} />
        </div>
      </div>

      {/* 記録ボタンセクション */}
      <div className="glass-card text-center" style={{ position: 'relative', overflow: 'hidden' }}>
        <div className="form-group" style={{ textAlign: 'left', marginBottom: '20px' }}>
          <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <MessageSquare size={14} /> 吸っちまった状況のメモ (任意)
          </label>
          <input
            type="text"
            className="form-input"
            placeholder="例：仕事のストレス、食後、休憩など"
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
          />
        </div>

        <div className="pulse-button-container">
          <button className="pulse-button" onClick={handleRecord}>
            <Flame size={40} style={{ animation: 'bounce 2s infinite' }} />
            <span>吸っちまった</span>
          </button>
          {/* 常に、あるいはアニメーション中に動くリング */}
          <div className="pulse-ring"></div>
          {showPulse && <div className="pulse-ring" style={{ animationDelay: '0.5s' }}></div>}
        </div>
        <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
          ボタンをタップすると、現在時刻で即座に記録されます。
        </span>
      </div>

      {/* スマートインサイト */}
      <div className="glass-card insight-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} /> スマートインサイト
          </h4>
          {llmInsight && llmUpdateTime && (
            <span style={{ fontSize: '10px', color: 'var(--text-secondary)' }}>
              更新: {(() => {
                const d = new Date(parseInt(llmUpdateTime, 10));
                return `${d.getFullYear()}/${String(d.getMonth()+1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
              })()}
            </span>
          )}
        </div>
        <p className="insight-text">{insight}</p>
      </div>

      {/* ランダムメッセージトースト表示 */}
      {toastMessage && (
        <div className="toast-overlay" onClick={() => setToastMessage(null)}>
          <div className="toast-card">
            <div className="toast-emoji">🚬</div>
            <div className="toast-content">{toastMessage}</div>
          </div>
        </div>
      )}
    </div>
  );
}
