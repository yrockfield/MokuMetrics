// smokeAnalytics.js

/**
 * 今日（ローカル日付）の喫煙件数を取得
 */
export function getTodayCount(records) {
  const todayStr = new Date().toDateString();
  return records.filter(r => new Date(r.timestamp).toDateString() === todayStr).length;
}

/**
 * 前回の喫煙から現在までの経過時間を秒数で取得
 * 記録がない場合は null
 */
export function getSecondsSinceLastSmoke(records, now = Date.now()) {
  if (!records || records.length === 0) return null;
  // 最新のレコードを取得 (timestampの降順を想定、または最大値を検索)
  const timestamps = records.map(r => r.timestamp);
  const lastTimestamp = Math.max(...timestamps);
  if (lastTimestamp > now) return 0;
  return Math.floor((now - lastTimestamp) / 1000);
}

/**
 * 経過秒数を "HH:MM:SS" または "DD日 HH:MM:SS" のフォーマットに変換
 */
export function formatDuration(seconds) {
  if (seconds === null) return "--:--:--";
  const d = Math.floor(seconds / (3600 * 24));
  const h = Math.floor((seconds % (3600 * 24)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;

  const pad = (n) => String(n).padStart(2, '0');
  
  if (d > 0) {
    return `${d}日 ${pad(h)}:${pad(m)}:${pad(s)}`;
  }
  return `${pad(h)}:${pad(m)}:${pad(s)}`;
}

/**
 * 曜日別の喫煙本数を集計
 * 戻り値: { "月": 3, "火": 5, ... } 形式の配列
 */
export function getDayOfWeekStats(records) {
  const dayNames = ["日", "月", "火", "水", "木", "金", "土"];
  const stats = dayNames.map(name => ({ label: name, count: 0 }));
  
  records.forEach(r => {
    const dayIndex = new Date(r.timestamp).getDay();
    stats[dayIndex].count += 1;
  });
  
  return stats;
}

/**
 * 曜日(7) × 時間帯(24時間) のヒートマップデータを集計
 * 戻り値: 2次元配列 stats[dayIndex][hour] = 件数
 */
export function getHourlyHeatmapStats(records) {
  // 7曜日 × 24時間のマトリックス
  const matrix = Array.from({ length: 7 }, () => Array(24).fill(0));
  
  records.forEach(r => {
    const date = new Date(r.timestamp);
    const day = date.getDay(); // 0:日, 1:月, ...
    const hour = date.getHours(); // 0-23
    matrix[day][hour] += 1;
  });
  
  return matrix;
}

/**
 * ヒートマップの件数を 0〜4 のレベルに変換
 */
export function getHeatmapLevel(count) {
  if (count === 0) return 0;
  if (count <= 1) return 1;
  if (count <= 3) return 2;
  if (count <= 5) return 3;
  return 4;
}

/**
 * 喫煙データに基づきスマートインサイトを生成
 */
export function generateSmartInsight(records) {
  if (!records || records.length === 0) {
    return "まずは記録を始めましょう！「また吸っちまった」その瞬間を恐れずタップしてください。";
  }

  // 1. 今日と昨日の本数比較
  const todayStr = new Date().toDateString();
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const yesterdayStr = yesterday.toDateString();

  let todayCount = 0;
  let yesterdayCount = 0;

  records.forEach(r => {
    const dStr = new Date(r.timestamp).toDateString();
    if (dStr === todayStr) todayCount++;
    else if (dStr === yesterdayStr) yesterdayCount++;
  });

  if (todayCount > 0 && yesterdayCount > 0) {
    if (todayCount < yesterdayCount) {
      return `昨日の ${yesterdayCount} 本に比べ、今日は ${todayCount} 本と抑えられています！素晴らしい調子です。この調子でいきましょう！`;
    } else if (todayCount > yesterdayCount) {
      return `昨日の ${yesterdayCount} 本を超えて、今日は既に ${todayCount} 本吸っています。少し深呼吸して、次の1本を5分だけ遅らせてみませんか？`;
    }
  }

  // 2. 最多時間帯のインサイト
  const hourCounts = Array(24).fill(0);
  records.forEach(r => {
    const hr = new Date(r.timestamp).getHours();
    hourCounts[hr]++;
  });

  let maxHour = 0;
  let maxCount = 0;
  hourCounts.forEach((count, hr) => {
    if (count > maxCount) {
      maxCount = count;
      maxHour = hr;
    }
  });

  if (maxCount >= 3) {
    let timeframe = "";
    if (maxHour >= 5 && maxHour < 10) timeframe = "朝（5時〜10時）";
    else if (maxHour >= 10 && maxHour < 16) timeframe = "昼（10時〜16時）";
    else if (maxHour >= 16 && maxHour < 21) timeframe = "夕方・夜（16時〜21時）";
    else timeframe = "深夜（21時〜5時）";

    return `データによると、あなたは【${timeframe}】の喫煙が最も多い傾向にあります。この時間帯は「口寂しさ」への代替手段（ガムやミントなど）を用意しておくと効果的です。`;
  }

  // 3. 直近の間隔のトレンド
  if (records.length >= 3) {
    // 降順にソートして直近3件の差分をとる
    const sorted = [...records].sort((a, b) => b.timestamp - a.timestamp);
    const diff1 = sorted[0].timestamp - sorted[1].timestamp;
    const diff2 = sorted[1].timestamp - sorted[2].timestamp;

    if (diff1 < diff2 && diff1 < 30 * 60 * 1000) {
      // 直近の間隔が30分未満で、前回より縮まっている場合
      return "直近の喫煙ペースがやや早くなっています。「チェーンスモーク」になりそうな時は、冷たい水を一杯飲んでリフレッシュしてみてください。";
    }
  }

  return "記録が順調に蓄積されています！ダッシュボードから自分の「喫煙パターン」を把握して、自然なコントロールを目指しましょう。";
}
