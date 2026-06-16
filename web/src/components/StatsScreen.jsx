import React, { useState, useEffect } from 'react';
import { BarChart2, Calendar, Clock, Sparkles } from 'lucide-react';
import { getDayOfWeekStats, getSmokingIntervalStats, getPeriodHeatmapStats, getPeriodHeatmapLevel, getLlmPatternAnalysis } from '../utils/smokeAnalytics';

export default function StatsScreen({ records }) {
  const [llmPattern, setLlmPattern] = useState(getLlmPatternAnalysis());
  const [llmUpdateTime, setLlmUpdateTime] = useState(() => localStorage.getItem("llm_last_insight_update_time"));

  useEffect(() => {
    const handleLlmUpdate = () => {
      setLlmPattern(getLlmPatternAnalysis());
      setLlmUpdateTime(localStorage.getItem("llm_last_insight_update_time"));
    };
    window.addEventListener("llm_data_updated", handleLlmUpdate);
    return () => window.removeEventListener("llm_data_updated", handleLlmUpdate);
  }, []);
  // 曜日別統計
  const dayStats = getDayOfWeekStats(records);
  const maxDayCount = Math.max(...dayStats.map(d => d.count), 1);

  // 喫煙間隔統計
  const intervalStats = getSmokingIntervalStats(records);

  // 期間別ヒートマップ統計
  const heatmapData = getPeriodHeatmapStats(records);
  const days = ["日", "月", "火", "水", "木", "金", "土"];
  const periods = [
    { label: "深夜 (0-8)", range: "0-8" },
    { label: "朝 (8-12)", range: "8-12" },
    { label: "昼 (12-18)", range: "12-18" },
    { label: "夜 (18-24)", range: "18-24" }
  ];

  // 基本メトリクス
  const totalCount = records.length;
  const uniqueDays = new Set(records.map(r => new Date(r.timestamp).toDateString())).size || 1;
  const dailyAverage = (totalCount / uniqueDays).toFixed(1);

  // 最多時間帯の算出
  const hourCounts = Array(24).fill(0);
  records.forEach(r => {
    const hr = new Date(r.timestamp).getHours();
    hourCounts[hr]++;
  });
  const maxHourVal = Math.max(...hourCounts, 0);
  const peakHour = maxHourVal > 0 ? hourCounts.indexOf(maxHourVal) : null;

  return (
    <div className="stats-screen">
      {/* 概要カード */}
      <div className="stats-grid">
        <div className="glass-card stat-box">
          <div className="stat-value">{totalCount}</div>
          <div className="stat-label">累計喫煙本数</div>
        </div>
        <div className="glass-card stat-box">
          <div className="stat-value">{dailyAverage}</div>
          <div className="stat-label">1日平均本数</div>
        </div>
      </div>

      {/* 曜日別棒グラフ */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChart2 size={16} /> 曜日別喫煙トレンド
        </h4>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>曜日ごとの平均本数</p>
        
        <div className="chart-bar-container">
          {dayStats.map((day, index) => {
            const heightPercent = `${(day.count / maxDayCount) * 100}%`;
            return (
              <div key={index} className="chart-bar-wrapper">
                <div style={{ fontSize: '10px', marginBottom: '4px', fontWeight: 'bold' }}>
                  {day.count > 0 ? day.count : ''}
                </div>
                <div 
                  className="chart-bar" 
                  style={{ height: heightPercent }}
                  title={`${day.label}曜日: ${day.count}本`}
                ></div>
                <div className="chart-label">{day.label}</div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 喫煙間隔のばらつき分布 */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Clock size={16} /> 喫煙間隔のばらつき分布
        </h4>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          同じ日の中での喫煙間隔（前回の喫煙から何分空いたか）の分布
        </p>

        {intervalStats.total === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-secondary)', fontSize: '13px' }}>
            間隔を算出するためのデータが不足しています（1日2回以上の喫煙記録が必要です）。
          </div>
        ) : (
          <div>
            {/* スタックバー */}
            <div style={{ 
              display: 'flex', 
              height: '20px', 
              borderRadius: '10px', 
              overflow: 'hidden', 
              backgroundColor: 'rgba(255,255,255,0.05)',
              marginBottom: '20px'
            }}>
              {intervalStats.under30 > 0 && (
                <div style={{ 
                  width: `${(intervalStats.under30 / intervalStats.total) * 100}%`, 
                  backgroundColor: '#ef4444'
                }} title={`30分未満: ${intervalStats.under30}件`} />
              )}
              {intervalStats.between30And60 > 0 && (
                <div style={{ 
                  width: `${(intervalStats.between30And60 / intervalStats.total) * 100}%`, 
                  backgroundColor: '#f97316'
                }} title={`30分〜60分: ${intervalStats.between30And60}件`} />
              )}
              {intervalStats.between60And120 > 0 && (
                <div style={{ 
                  width: `${(intervalStats.between60And120 / intervalStats.total) * 100}%`, 
                  backgroundColor: '#10b981'
                }} title={`60分〜120分: ${intervalStats.between60And120}件`} />
              )}
              {intervalStats.over120 > 0 && (
                <div style={{ 
                  width: `${(intervalStats.over120 / intervalStats.total) * 100}%`, 
                  backgroundColor: '#3b82f6'
                }} title={`120分以上: ${intervalStats.over120}件`} />
              )}
            </div>

            {/* 凡例・詳細 */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', fontSize: '11px' }}>
                <span style={{ display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', backgroundColor: '#ef4444', marginTop: '3px' }}></span>
                <div>
                  <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>30分未満</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{intervalStats.under30}件 ({((intervalStats.under30 / intervalStats.total) * 100).toFixed(1)}%)</div>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', fontSize: '11px' }}>
                <span style={{ display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', backgroundColor: '#f97316', marginTop: '3px' }}></span>
                <div>
                  <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>30分〜60分</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{intervalStats.between30And60}件 ({((intervalStats.between30And60 / intervalStats.total) * 100).toFixed(1)}%)</div>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', fontSize: '11px' }}>
                <span style={{ display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', backgroundColor: '#10b981', marginTop: '3px' }}></span>
                <div>
                  <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>60分〜120分</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{intervalStats.between60And120}件 ({((intervalStats.between60And120 / intervalStats.total) * 100).toFixed(1)}%)</div>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', fontSize: '11px' }}>
                <span style={{ display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', backgroundColor: '#3b82f6', marginTop: '3px' }}></span>
                <div>
                  <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>120分以上</div>
                  <div style={{ color: 'var(--text-secondary)' }}>{intervalStats.over120}件 ({((intervalStats.over120 / intervalStats.total) * 100).toFixed(1)}%)</div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 曜日×時間帯ヒートマップ (大区分) */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Clock size={16} /> 曜日×時間帯ヒートマップ
        </h4>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          どの曜日・どの時間帯に多く吸っているかの分布 (時間帯大区分)
        </p>

        {/* グリッド領域 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {/* ヘッダー */}
          <div style={{ display: 'flex', paddingLeft: '24px', fontSize: '10px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
            {periods.map((p, idx) => (
              <div key={idx} style={{ flex: 1, textAlign: 'center', fontWeight: 'bold' }}>
                {p.label}
              </div>
            ))}
          </div>

          {/* 各曜日の行 */}
          {days.map((day, dayIdx) => (
            <div key={dayIdx} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {/* 曜日ラベル */}
              <div style={{ width: '16px', fontSize: '11px', color: 'var(--text-secondary)', fontWeight: 'bold' }}>
                {day}
              </div>
              {/* 4つの時間帯セル */}
              <div style={{ display: 'flex', flex: 1, gap: '4px' }}>
                {periods.map((_, pIdx) => {
                  const count = heatmapData[dayIdx][pIdx];
                  const level = getPeriodHeatmapLevel(count);
                  return (
                    <div
                      key={pIdx}
                      className={`heatmap-cell heatmap-level-${level}`}
                      style={{ 
                        flex: 1, 
                        height: '28px', 
                        borderRadius: '4px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '10px',
                        fontWeight: 'bold',
                        color: level > 2 ? '#fff' : 'var(--text-primary)',
                        opacity: level === 0 ? 0.3 : 1
                      }}
                      title={`${day}曜日: ${count}本`}
                    >
                      {count > 0 ? `${count}本` : '0'}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* 凡例 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '6px', marginTop: '12px', fontSize: '10px', color: 'var(--text-secondary)' }}>
          <span>少</span>
          <div className="heatmap-level-0" style={{ width: '10px', height: '10px', borderRadius: '2px' }}></div>
          <div className="heatmap-level-1" style={{ width: '10px', height: '10px', borderRadius: '2px' }}></div>
          <div className="heatmap-level-2" style={{ width: '10px', height: '10px', borderRadius: '2px' }}></div>
          <div className="heatmap-level-3" style={{ width: '10px', height: '10px', borderRadius: '2px' }}></div>
          <div className="heatmap-level-4" style={{ width: '10px', height: '10px', borderRadius: '2px' }}></div>
          <span>多</span>
        </div>
      </div>

      {/* 分析インサイト概要 */}
      {(llmPattern || peakHour !== null) && (
        <div className="glass-card" style={{ borderLeft: '4px solid var(--color-warning)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
            <h5 style={{ fontSize: '13px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-primary)', margin: 0 }}>
              <Sparkles size={14} style={{ color: 'var(--color-warning)' }} /> パターン分析
            </h5>
            {llmPattern && llmUpdateTime && (
              <span style={{ fontSize: '10px', color: 'var(--text-secondary)' }}>
                更新: {(() => {
                  const d = new Date(parseInt(llmUpdateTime, 10));
                  return `${d.getFullYear()}/${String(d.getMonth()+1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                })()}
              </span>
            )}
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '6px', lineHeight: '1.4' }}>
            {llmPattern ? llmPattern : (
              <>あなたの喫煙ピーク時間帯は <strong>{peakHour}時台</strong> です。この時間帯に行動パターンを変える（例：散歩する、お茶を飲む）ことで、喫煙本数の削減が期待できます。</>
            )}
          </p>
        </div>
      )}
    </div>
  );
}
