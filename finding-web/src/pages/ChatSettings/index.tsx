import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { uploadApi } from '../../api/upload';
import { userApi } from '../../api/user';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ConfirmDialog from '../../components/ConfirmDialog';
import { CHAT_BG_PRESETS, resolveChatBg } from '../../utils/chatBackgrounds';
import type { ChatSettings } from '../../types/message';
import './index.css';

const REPORT_REASONS = ['骚扰 / 不文明用语', '诈骗嫌疑', '色情低俗', '冒充身份', '其他'];

/** 聊天信息页 —— 从聊天 header 设置键进入 */
export default function ChatSettingsPage() {
  const [searchParams] = useSearchParams();
  const userId = Number(searchParams.get('userId'));
  const roomId = Number(searchParams.get('roomId'));
  const nickname = searchParams.get('name') || '对方';
  const avatar = searchParams.get('avatar') || '';
  const navigate = useNavigate();
  const myId = useAuthStore((s) => s.user?.id);

  const [settings, setSettings] = useState<ChatSettings | null>(null);
  const [profile, setProfile] = useState<any>(null);
  const [view, setView] = useState<'main' | 'search' | 'background'>('main');

  // 查找聊天记录
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<any[] | null>(null);
  const [searching, setSearching] = useState(false);

  // 清空 / 投诉
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [showReport, setShowReport] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reportCustom, setReportCustom] = useState('');
  const bgFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (roomId) {
      chatApi.getSettings(roomId).then((res) => setSettings(res.data.data)).catch(() => {});
    }
    if (userId) {
      userApi.getProfile(userId).then((res) => setProfile(res.data.data)).catch(() => {});
    }
  }, [roomId, userId]);

  const updateSetting = async (data: { pinned?: boolean; muted?: boolean; background?: string }) => {
    if (!roomId) return;
    try {
      await chatApi.updateSettings(roomId, data);
      setSettings((prev) => (prev ? { ...prev, ...data } : prev));
    } catch (e: any) {
      showToast(e?.message || '设置保存失败');
    }
  };

  // ── 查找聊天记录 ──
  const handleSearch = async () => {
    if (!keyword.trim()) return;
    setSearching(true);
    try {
      const res = await chatApi.searchMessages(roomId, keyword.trim());
      setResults(res.data.data.records || []);
    } catch {
      showToast('搜索失败，请重试');
    } finally {
      setSearching(false);
    }
  };

  // ── 清空聊天记录 ──
  const handleClear = async () => {
    setShowClearConfirm(false);
    try {
      await chatApi.clearMessages(roomId);
      showToast('聊天记录已清空');
    } catch (e: any) {
      showToast(e?.message || '清空失败');
    }
  };

  // ── 投诉 ──
  const handleReport = async () => {
    const reason = reportCustom.trim() || reportReason;
    if (!reason) {
      showToast('请选择或填写投诉原因');
      return;
    }
    try {
      await chatApi.reportUser({ targetUserId: userId, roomId, reason });
      setShowReport(false);
      setReportCustom('');
      setReportReason('');
      showToast('投诉已提交，我们会尽快处理');
    } catch (e: any) {
      showToast(e?.message || '投诉提交失败');
    }
  };

  // ── 聊天背景 ──
  const handleUploadBg = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await uploadApi.uploadImage(file);
      await updateSetting({ background: res.data.data });
      showToast('背景已更新');
    } catch {
      showToast('背景设置失败');
    } finally {
      e.target.value = '';
    }
  };

  const openProfile = () => navigate(`/user/${userId}`);

  // ── 查找聊天记录视图 ──
  if (view === 'search') {
    return (
      <div className="cs-page">
        <div className="cs-header">
          <button className="back-btn" onClick={() => setView('main')}>←</button>
          <span>查找聊天记录</span>
        </div>
        <div className="cs-search-bar">
          <input
            className="cs-search-input"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="输入关键词搜索"
            autoFocus
          />
          <button className="cs-search-btn" onClick={handleSearch} disabled={searching || !keyword.trim()}>
            {searching ? '搜索中' : '搜索'}
          </button>
        </div>
        <div className="cs-search-results">
          {results === null && <p className="cs-empty">输入关键词搜索聊天记录</p>}
          {results !== null && results.length === 0 && <p className="cs-empty">没有找到相关聊天记录</p>}
          {results?.map((m) => (
            <div key={m.id} className="cs-result-item">
              <span className="cs-result-name">
                {m.fromUserId === myId ? '我' : nickname}
              </span>
              <span className="cs-result-text">{m.messageType === 'image' ? '[图片]' : m.content}</span>
              <span className="cs-result-time">{formatTime(m.createdAt)}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // ── 聊天背景视图 ──
  if (view === 'background') {
    const currentBg = settings?.background || null;
    return (
      <div className="cs-page">
        <div className="cs-header">
          <button className="back-btn" onClick={() => setView('main')}>←</button>
          <span>设置聊天背景</span>
        </div>
        <div className="cs-bg-body">
          <div className="cs-bg-current" style={resolveChatBg(currentBg) || { background: '#f0f0f0' }}>
            <span>当前背景</span>
          </div>
          <div className="cs-bg-grid">
            {Object.entries(CHAT_BG_PRESETS).map(([key, style]) => (
              <button
                key={key}
                className={`cs-bg-swatch ${currentBg === key ? 'active' : ''}`}
                style={style}
                onClick={() => updateSetting({ background: key })}
              />
            ))}
          </div>
          <div className="cs-bg-actions">
            <button className="cs-bg-action" onClick={() => updateSetting({ background: '' })}>
              恢复默认
            </button>
            <button className="cs-bg-action" onClick={() => bgFileRef.current?.click()}>
              上传图片
            </button>
            <input ref={bgFileRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleUploadBg} />
          </div>
        </div>
      </div>
    );
  }

  // ── 主视图 ──
  return (
    <div className="cs-page">
      <div className="cs-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>聊天信息</span>
      </div>

      {/* 对方名片 → 跳转情感简历 */}
      <div className="cs-contact-card" onClick={openProfile}>
        <div className="cs-contact-avatar">
          {avatar ? <img src={avatar} alt="" /> : <span>👤</span>}
        </div>
        <div className="cs-contact-info">
          <span className="cs-contact-name">{nickname}</span>
          <span className="cs-contact-sub">查看TA的情感简历 ›</span>
        </div>
      </div>

      {/* 设置项 */}
      <div className="cs-list">
        <button className="cs-item" onClick={() => { setResults(null); setKeyword(''); setView('search'); }}>
          <span className="cs-item-icon">🔍</span>
          <span className="cs-item-label">查找聊天记录</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <div className="cs-item">
          <span className="cs-item-icon">📌</span>
          <span className="cs-item-label">置顶聊天</span>
          <span
            className={`cs-switch ${settings?.pinned ? 'on' : ''}`}
            onClick={() => updateSetting({ pinned: !settings?.pinned })}
          >
            <span className="cs-switch-dot" />
          </span>
        </div>

        <div className="cs-item">
          <span className="cs-item-icon">🔕</span>
          <span className="cs-item-label">消息免打扰</span>
          <span
            className={`cs-switch ${settings?.muted ? 'on' : ''}`}
            onClick={() => updateSetting({ muted: !settings?.muted })}
          >
            <span className="cs-switch-dot" />
          </span>
        </div>

        <button className="cs-item" onClick={() => setView('background')}>
          <span className="cs-item-icon">🎨</span>
          <span className="cs-item-label">设置当前聊天背景</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <button className="cs-item" onClick={() => setShowClearConfirm(true)}>
          <span className="cs-item-icon">🗑️</span>
          <span className="cs-item-label">清空聊天记录</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <button className="cs-item" onClick={() => setShowReport(true)}>
          <span className="cs-item-icon">⚠️</span>
          <span className="cs-item-label">投诉</span>
          <span className="cs-item-arrow">›</span>
        </button>
      </div>

      {/* 清空确认 */}
      <ConfirmDialog
        visible={showClearConfirm}
        title="清空聊天记录"
        message="确定要清空与对方的全部聊天记录吗？此操作不可恢复。"
        confirmText="清空"
        danger
        onConfirm={handleClear}
        onCancel={() => setShowClearConfirm(false)}
      />

      {/* 投诉弹窗 */}
      {showReport && (
        <div className="report-overlay" onClick={() => setShowReport(false)}>
          <div className="report-card" onClick={(e) => e.stopPropagation()}>
            <h4 className="report-title">投诉 {nickname}</h4>
            <div className="report-reasons">
              {REPORT_REASONS.map((r) => (
                <button
                  key={r}
                  className={`report-reason ${reportReason === r ? 'active' : ''}`}
                  onClick={() => { setReportReason(r); setReportCustom(''); }}
                >
                  {r}
                </button>
              ))}
            </div>
            <textarea
              className="report-input"
              rows={2}
              placeholder="补充说明（选填）"
              value={reportCustom}
              onChange={(e) => { setReportCustom(e.target.value); setReportReason(''); }}
            />
            <div className="report-buttons">
              <button className="report-btn cancel" onClick={() => setShowReport(false)}>取消</button>
              <button className="report-btn submit" onClick={handleReport}>提交投诉</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  if (diff < 172800000) return '昨天';
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
