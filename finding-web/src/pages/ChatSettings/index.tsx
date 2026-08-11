import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { chatApi } from '../../api/chat';
import { uploadApi } from '../../api/upload';
import { userApi } from '../../api/user';
import { reportApi } from '../../api/report';
import { useAuthStore } from '../../store/authStore';
import { showToast } from '../../components/Toast';
import ConfirmDialog from '../../components/ConfirmDialog';
import AppIcon from '../../components/AppIcon';
import type { ChatSettings } from '../../types/message';
import SearchView from './components/SearchView';
import BackgroundView from './components/BackgroundView';
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

  // ── 隐藏会话 ──
  const handleHide = async () => {
    if (!roomId) return;
    try {
      await chatApi.hideConversation(roomId, true);
      showToast('已隐藏会话，对方发来新消息时会自动恢复');
      navigate(-1);
    } catch (e: any) {
      showToast(e?.message || '隐藏失败');
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
      await reportApi.report({ targetType: 'user', targetId: userId, roomId, reason });
      setShowReport(false);
      setReportCustom('');
      setReportReason('');
      showToast('投诉已提交，我们会尽快处理');
    } catch (e: any) {
      showToast(e?.message || '投诉提交失败');
    }
  };

  // ── 上传聊天背景 ──
  const handleUploadBg = async (file: File) => {
    try {
      const res = await uploadApi.uploadImage(file);
      await updateSetting({ background: res.data.data });
      showToast('背景已更新');
    } catch {
      showToast('背景设置失败');
    }
  };

  const openProfile = () => navigate(`/user/${userId}`);

  // ── 查找聊天记录视图 ──
  if (view === 'search') {
    return (
      <SearchView
        nickname={nickname}
        myId={myId}
        keyword={keyword}
        onKeywordChange={setKeyword}
        searching={searching}
        results={results}
        onSearch={handleSearch}
        onBack={() => setView('main')}
      />
    );
  }

  // ── 聊天背景视图 ──
  if (view === 'background') {
    return (
      <BackgroundView
        background={settings?.background ?? null}
        onUpdate={(bg) => updateSetting({ background: bg })}
        onUpload={handleUploadBg}
        onBack={() => setView('main')}
      />
    );
  }

  // ── 主视图 ──
  return (
    <div className="cs-page">
      <div className="cs-header">
        <button className="back-btn" onClick={() => navigate(-1)} aria-label="返回"><AppIcon name="left" size={20} /></button>
        <span>聊天信息</span>
      </div>

      {/* 对方名片 → 跳转情感简介 */}
      <div className="cs-contact-card" onClick={openProfile}>
        <div className="cs-contact-avatar">
          {avatar ? <img src={avatar} alt="" /> : <AppIcon name="user" size={24} />}
        </div>
        <div className="cs-contact-info">
          <span className="cs-contact-name">{nickname}</span>
          <span className="cs-contact-sub">查看TA的情感简介 ›</span>
        </div>
      </div>

      {/* 设置项 */}
      <div className="cs-list">
        <button className="cs-item" onClick={() => { setResults(null); setKeyword(''); setView('search'); }}>
          <AppIcon name="search" className="cs-item-icon" size={19} />
          <span className="cs-item-label">查找聊天记录</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <div className="cs-item">
          <AppIcon name="pin" className="cs-item-icon" size={19} />
          <span className="cs-item-label">置顶聊天</span>
          <span
            className={`cs-switch ${settings?.pinned ? 'on' : ''}`}
            onClick={() => updateSetting({ pinned: !settings?.pinned })}
          >
            <span className="cs-switch-dot" />
          </span>
        </div>

        <div className="cs-item">
          <AppIcon name="mute" className="cs-item-icon" size={19} />
          <span className="cs-item-label">消息免打扰</span>
          <span
            className={`cs-switch ${settings?.muted ? 'on' : ''}`}
            onClick={() => updateSetting({ muted: !settings?.muted })}
          >
            <span className="cs-switch-dot" />
          </span>
        </div>

        <button className="cs-item" onClick={() => setView('background')}>
          <AppIcon name="palette" className="cs-item-icon" size={19} />
          <span className="cs-item-label">设置当前聊天背景</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <button className="cs-item" onClick={() => setShowClearConfirm(true)}>
          <AppIcon name="trash" className="cs-item-icon" size={19} />
          <span className="cs-item-label">清空聊天记录</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <button className="cs-item" onClick={handleHide}>
          <AppIcon name="eye" className="cs-item-icon" size={19} />
          <span className="cs-item-label">隐藏会话</span>
          <span className="cs-item-arrow">›</span>
        </button>

        <button className="cs-item" onClick={() => setShowReport(true)}>
          <AppIcon name="flag" className="cs-item-icon" size={19} />
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
