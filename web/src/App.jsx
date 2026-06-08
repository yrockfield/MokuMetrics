import React, { useState, useEffect } from 'react';
import { Home, BarChart2, Calendar, Settings as SettingsIcon } from 'lucide-react';

// コンポーネントインポート
import HomeScreen from './components/HomeScreen';
import StatsScreen from './components/StatsScreen';
import HistoryScreen from './components/HistoryScreen';
import SettingsScreen from './components/SettingsScreen';

export default function App() {
  // 1. 記録データ (LocalStorage 同期)
  const [records, setRecords] = useState(() => {
    const saved = localStorage.getItem('mokumetrics_records');
    return saved ? JSON.parse(saved) : [];
  });

  // 2. アクティブなタブ ('home' | 'stats' | 'history' | 'settings')
  const [activeTab, setActiveTab] = useState('home');

  // 3. テーマ管理 (LocalStorage 同期)
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('mokumetrics_theme');
    return saved || 'aurora'; // デフォルトは aurora green
  });

  // レコード更新時の LocalStorage 反映
  useEffect(() => {
    localStorage.setItem('mokumetrics_records', JSON.stringify(records));
  }, [records]);

  // テーマ更新時の DOM 反映および LocalStorage 保存
  useEffect(() => {
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('mokumetrics_theme', theme);
  }, [theme]);

  // レコード追加
  const handleAddRecord = (timestamp, memo) => {
    const newRecord = {
      id: Date.now() + Math.random().toString(36).substr(2, 9), // 重複しづらいID
      timestamp,
      memo
    };
    setRecords(prev => [...prev, newRecord]);
  };

  // レコード削除
  const handleDeleteRecord = (id) => {
    setRecords(prev => prev.filter(r => r.id !== id));
  };

  // レコードのメモ更新
  const handleUpdateRecord = (id, newMemo) => {
    setRecords(prev => prev.map(r => r.id === id ? { ...r, memo: newMemo } : r));
  };

  // データ全削除
  const handleClearData = () => {
    setRecords([]);
  };

  // 画面のレンダリング振り分け
  const renderScreen = () => {
    switch (activeTab) {
      case 'home':
        return <HomeScreen records={records} onAddRecord={handleAddRecord} />;
      case 'stats':
        return <StatsScreen records={records} />;
      case 'history':
        return (
          <HistoryScreen 
            records={records} 
            onAddRecord={handleAddRecord}
            onDeleteRecord={handleDeleteRecord} 
            onUpdateRecord={handleUpdateRecord}
          />
        );
      case 'settings':
        return (
          <SettingsScreen 
            theme={theme} 
            onThemeChange={setTheme} 
            records={records}
            onImportData={setRecords}
            onClearData={handleClearData} 
          />
        );
      default:
        return <HomeScreen records={records} onAddRecord={handleAddRecord} />;
    }
  };

  return (
    <div className="app-container">
      {/* 上部ヘッダー */}
      <header className="app-header" style={{ alignItems: 'center' }}>
        <h1 className="app-title" style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '28px' }}>
          <img src="/favicon.png" alt="MokuMetrics Logo" style={{ width: '48px', height: '48px', borderRadius: '10px', boxShadow: '0 0 10px rgba(16, 185, 129, 0.2)' }} /> MokuMetrics
        </h1>
        <span style={{ fontSize: '11px', background: 'rgba(255,255,255,0.08)', padding: '4px 8px', borderRadius: '12px', color: 'var(--text-secondary)' }}>
          {activeTab === 'home' && 'ダッシュボード'}
          {activeTab === 'stats' && '分析グラフ'}
          {activeTab === 'history' && '喫煙履歴'}
          {activeTab === 'settings' && 'アプリ設定'}
        </span>
      </header>

      {/* メイン画面コンテンツ */}
      <main style={{ flex: 1, width: '100%' }}>
        {renderScreen()}
      </main>

      {/* ボトムナビゲーションバー */}
      <nav className="bottom-nav">
        <button 
          className={`nav-item ${activeTab === 'home' ? 'active' : ''}`}
          onClick={() => setActiveTab('home')}
        >
          <Home size={20} />
          <span>ホーム</span>
        </button>

        <button 
          className={`nav-item ${activeTab === 'stats' ? 'active' : ''}`}
          onClick={() => setActiveTab('stats')}
        >
          <BarChart2 size={20} />
          <span>統計</span>
        </button>

        <button 
          className={`nav-item ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          <Calendar size={20} />
          <span>履歴</span>
        </button>

        <button 
          className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          <SettingsIcon size={20} />
          <span>設定</span>
        </button>
      </nav>
    </div>
  );
}
