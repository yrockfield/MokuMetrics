import React from 'react';
import { BarChart2, Calendar, Clock, Sparkles } from 'lucide-react';
import { getDayOfWeekStats, getHourlyHeatmapStats, getHeatmapLevel } from '../utils/smokeAnalytics';

export default function StatsScreen({ records }) {
  // 曜日別統計
  const dayStats = getDayOfWeekStats(records);
  const maxDayCount = Math.max(...dayStats.map(d => d.count), 1);

  // ヒートマップ統計
  const heatmapData = getHourlyHeatmapStats(records);
  const days = ["日", "月", "火", "水", "木", "金", "土"];
  const hours = Array.from({ length: 24 }, (_, i) => i);

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
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>曜日ごとの合計本数</p>
        
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

      {/* 時間帯別ヒートマップ */}
      <div className="glass-card">
        <h4 style={{ fontSize: '14px', fontWeight: '700', color: 'var(--color-accent)', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Clock size={16} /> 曜日×時間帯ヒートマップ
        </h4>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          どの曜日・どの時間帯に多く吸っているかの分布 (GitHub風)
        </p>

        {/* スクロール可能なヒートマップ領域 */}
        <div style={{ overflowX: 'auto', paddingBottom: '8px' }}>
          <div style={{ minWidth: '420px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {/* 時間ヘッダー */}
            <div style={{ display: 'flex', paddingLeft: '28px', fontSize: '9px', color: 'var(--text-secondary)' }}>
              {hours.map(hr => (
                <div key={hr} style={{ width: '15px', textAlign: 'center', margin: '0 1px' }}>
                  {hr % 4 === 0 ? hr : ''}
                </div>
              ))}
            </div>

            {/* 曜日ごとの行 */}
            {days.map((day, dayIdx) => (
              <div key={dayIdx} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                {/* 曜日ラベル */}
                <div style={{ width: '24px', fontSize: '10px', color: 'var(--text-secondary)', fontWeight: 'bold' }}>
                  {day}
                </div>
                {/* 24時間分のドット */}
                <div style={{ display: 'flex', gap: '2px' }}>
                  {hours.map(hr => {
                    const count = heatmapData[dayIdx][hr];
                    const level = getHeatmapLevel(count);
                    return (
                      <div
                        key={hr}
                        className={`heatmap-cell heatmap-level-${level}`}
                        style={{ width: '15px', height: '15px', borderRadius: '3px' }}
                        title={`${day}曜日 ${hr}時: ${count}本`}
                      >
                        {/* ホバー時に情報をブラウザの標準titleで出す */}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
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
      {peakHour !== null && (
        <div className="glass-card" style={{ borderLeft: '4px solid var(--color-warning)' }}>
          <h5 style={{ fontSize: '13px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-primary)' }}>
            <Sparkles size={14} style={{ color: 'var(--color-warning)' }} /> パターン分析
          </h5>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '6px', lineHeight: '1.4' }}>
            あなたの喫煙ピーク時間帯は <strong>{peakHour}時台</strong> です。この時間帯に行動パターンを変える（例：散歩する、お茶を飲む）ことで、喫煙本数の削減が期待できます。
          </p>
        </div>
      )}
    </div>
  );
}
