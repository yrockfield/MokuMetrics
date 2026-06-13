// smokeAnalytics.js
import { GoogleGenAI } from "@google/genai";

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
 * 同じ日のレコード同士で喫煙間隔を計算し、ばらつき分布を集計する
 */
export function getSmokingIntervalStats(records) {
  const stats = { under30: 0, between30And60: 0, between60And120: 0, over120: 0, total: 0 };
  if (!records || records.length < 2) return stats;

  // 日付文字列ごとにグループ化
  const groups = {};
  records.forEach(r => {
    const dateStr = new Date(r.timestamp).toDateString();
    if (!groups[dateStr]) groups[dateStr] = [];
    groups[dateStr].push(r);
  });

  Object.values(groups).forEach(dayRecords => {
    if (dayRecords.length < 2) return;
    // timestamp昇順でソート
    const sorted = [...dayRecords].sort((a, b) => a.timestamp - b.timestamp);
    for (let i = 0; i < sorted.length - 1; i++) {
      const diffMs = sorted[i + 1].timestamp - sorted[i].timestamp;
      const diffMins = diffMs / (60 * 1000);
      if (diffMins < 30) {
        stats.under30++;
      } else if (diffMins <= 60) {
        stats.between30And60++;
      } else if (diffMins <= 120) {
        stats.between60And120++;
      } else {
        stats.over120++;
      }
      stats.total++;
    }
  });

  return stats;
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

/**
 * 曜日(7) × 時間帯大区分(4: 深夜, 朝, 昼, 夜) のヒートマップデータを集計
 */
export function getPeriodHeatmapStats(records) {
  const matrix = Array.from({ length: 7 }, () => Array(4).fill(0));
  if (!records) return matrix;

  records.forEach(r => {
    const date = new Date(r.timestamp);
    const day = date.getDay(); // 0:日, 1:月, ...
    const hour = date.getHours(); // 0-23
    
    let periodIdx = 0;
    if (hour >= 8 && hour < 12) {
      periodIdx = 1; // 朝 (8-12)
    } else if (hour >= 12 && hour < 18) {
      periodIdx = 2; // 昼 (12-18)
    } else if (hour >= 18 && hour < 24) {
      periodIdx = 3; // 夜 (18-24)
    } else {
      periodIdx = 0; // 深夜・早朝 (0-8)
    }
    
    matrix[day][periodIdx] += 1;
  });

  return matrix;
}

/**
 * 期間ヒートマップ用のカラーレベル変換
 */
export function getPeriodHeatmapLevel(count) {
  if (count === 0) return 0;
  if (count <= 2) return 1;
  if (count <= 5) return 2;
  if (count <= 9) return 3;
  return 4;
}

/**
 * 必要に応じてGemini APIを叩き、インサイト、パターン分析、一言を更新する
 */
export async function updateLlmDataIfNeeded(records, apiKey, characterId = 'uncle') {
  if (!apiKey || !records || records.length === 0) return;

  const now = Date.now();
  const lastUpdateStr = localStorage.getItem("llm_last_insight_update_time");
  const lastUpdateTime = lastUpdateStr ? parseInt(lastUpdateStr, 10) : 0;
  
  // 更新頻度: 4時間 (4 * 60 * 60 * 1000)
  const UPDATE_INTERVAL = 4 * 60 * 60 * 1000;
  if (now - lastUpdateTime < UPDATE_INTERVAL) {
    // 4時間未満の場合は更新をスキップ
    return;
  }

  const CHARACTER_PROMPTS = {
    uncle: "あなたは「フランクなおっちゃん」としてふるまってください。ぶっきらぼうだけど人情味のある焼き鳥屋のオヤジのような口調（関西弁混じりのフランクな口調）で、ユーザーを優しく受け流して励ましてください。分析やアドバイスもおっちゃんのキャラクターらしく、温かみがありつつぶっきらぼうな言い回しにしてください。",
    tsundere: "あなたは「ツンデレ秘書」としてふるまってください。クールで丁寧な敬語を使う優秀なアシスタントで、基本は冷徹にデータを分析し、ややトゲのある言い方をしますが、最後にはユーザーの体のことや健康を本気で心配している強い優しさ（デレ）をはっきりと見せてください。最初はツンツンしていても、後半や最後のアドバイス部分では、心配するあまり感情がどうしても漏れ出てしまうような、強めのデレ感を意識してください。",
    gal: "あなたは「明るくフランクな女性キャラ（マイルドなギャル）」としてふるまってください。明るく超ポジティブでフランクな話し言葉（「〜じゃん」「〜だし！」「ヤバい」など）を使い、絵文字や感嘆符を多く交えながら、ユーザーの記録行為自体を褒めちぎり、モチベーションを爆上げする全肯定の応援をしてください。ただし、コテコテすぎる表現（「ウチ」という一人称や、過剰なギャル特有の略語など）は避け、あくまで「親しみやすくてノリが良い、ポジティブな女友達」のような自然なフランクさを意識してください。"
  };

  try {
    // 直近50件の喫煙記録を時系列順にフォーマット
    const recentRecords = [...records]
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 50)
      .reverse();

    const formattedRecords = recentRecords.map((r, idx, arr) => {
      const d = new Date(r.timestamp);
      const dateStr = `${d.getFullYear()}/${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
      const intervalStr = idx > 0 ? `${Math.floor((r.timestamp - arr[idx-1].timestamp) / (60 * 1000))}分` : "なし";
      return `${idx+1}. 日時: ${dateStr}, メモ: ${r.memo || "なし"}, 直前の喫煙からの経過時間: ${intervalStr}`;
    }).join("\n");

    const charPrompt = CHARACTER_PROMPTS[characterId] || CHARACTER_PROMPTS.uncle;

    const prompt = `${charPrompt}
ユーザーの直近の喫煙記録を分析し、以下の3つの要素を生成してください。

1. スマートインサイト (smartInsight):
   直近の喫煙本数の変化や、最近のペースなどに対する、キャラクターの個性を強く反映した簡潔で的確な禁煙・減煙のアドバイス（日本語で2〜3文）。
2. パターン分析 (patternAnalysis):
   曜日別や喫煙間隔の傾向に基づき、ユーザーの行動傾向（例：「ついつい30分未満で吸っている回数が多い」「特定の時間帯に集中している」など）をキャラクターらしく指摘し、具体的な対策を提案する文章（日本語で2〜3文）。
3. 一言メッセージ (oneLiners):
   「吸っちまった」ボタンを押した直後にユーザーに提示する、合計10個のメッセージ。キャラクターの口調を完璧に維持し、それぞれ20〜40文字程度で、以下の構成にしてください。
   - 喫煙したことに対するコメント（励まし、ツッコミ、アドバイスなど）: 2件
   - 喫煙とは全く関係のない、脈絡のない雑談や日常のどうでもいいコメント（例：キャラクターの好きな食べ物の話、今考えていること、どうでもいい豆知識など、喫煙とは完全に無関係な内容）: 8件

【ユーザーの喫煙記録】
${formattedRecords}

必ず以下のJSONスキーマに従ってJSONを出力してください。他の余計な説明文やマークダウンタグ（\`\`\`json等）は一切含めず、純粋なJSONオブジェクトのみを返してください。

{
  "smartInsight": "文字列",
  "patternAnalysis": "文字列",
  "oneLiners": ["一言1", "一言2", "一言3", "一言4", "一言5", "一言6", "一言7", "一言8", "一言9", "一言10"]
}
`;

    const ai = new GoogleGenAI({ apiKey: apiKey });
    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        responseMimeType: "application/json"
      }
    });

    const text = response.text;
    const data = JSON.parse(text);

    if (data.smartInsight && data.patternAnalysis && Array.isArray(data.oneLiners)) {
      localStorage.setItem("llm_smart_insight", data.smartInsight);
      localStorage.setItem("llm_pattern_analysis", data.patternAnalysis);
      localStorage.setItem("llm_oneliners", JSON.stringify(data.oneLiners));
      localStorage.setItem("llm_last_insight_update_time", String(now));
      
      // カスタム一言の適用を即時反映するためにカスタムイベントなどを発火することも可能
      window.dispatchEvent(new CustomEvent("llm_data_updated"));
    }
  } catch (err) {
    console.error("Gemini API error:", err);
  }
}

/**
 * 保存されたLLMインサイトを取得
 */
export function getLlmSmartInsight() {
  return localStorage.getItem("llm_smart_insight");
}

/**
 * 保存されたLLMパターン分析を取得
 */
export function getLlmPatternAnalysis() {
  return localStorage.getItem("llm_pattern_analysis");
}

/**
 * 保存されたLLM一言リストを取得
 */
export function getLlmOneLiners() {
  const custom = localStorage.getItem("llm_oneliners");
  if (custom) {
    try {
      return JSON.parse(custom);
    } catch (e) {
      return null;
    }
  }
  return null;
}
