import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { userApi } from '../../api/user';
import { historyApi } from '../../api/history';
import { chatApi } from '../../api/chat';
import { resumeApi } from '../../api/resume';
import { postApi } from '../../api/post';
import { showToast } from '../../components/Toast';
import PostCard from '../../components/PostCard';
import type { Post } from '../../types/post';
import type { User } from '../../types/user';
import { useAuthStore } from '../../store/authStore';
import { useInfoShareStore } from '../../store/infoShareStore';
import ResumeView from '../../components/ResumeView';
import ReportDialog from '../../components/ReportDialog';
import AppIcon from '../../components/AppIcon';
import type { ResumeView as ResumeViewType } from '../../types/resume';
import { getErrorMessage } from '../../utils/appError';
import './index.css';

export default function UserProfilePage() {
  const { id } = useParams<{ id: string }>();
  const userId = Number(id);
  const [profile, setProfile] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [resumeView, setResumeView] = useState<ResumeViewType | null>(null);
  const [resumeLoading, setResumeLoading] = useState(true);
  const [showReport, setShowReport] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [blockedBy, setBlockedBy] = useState(false);
  // 陌生人消息状态:hasConversation=已有会话 sent=我已打招呼 received=对方打招呼
  const [stranger, setStranger] = useState<{ hasConversation: boolean; sent: boolean; received: boolean }>({ hasConversation: true, sent: false, received: false });
  const [userPosts, setUserPosts] = useState<Post[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(true);
  const navigate = useNavigate();
  const myId = useAuthStore((s) => s.user?.id);
  const shareVersion = useInfoShareStore((s) => s.version);

  useEffect(() => {
    userApi.getProfile(userId).then((res) => {
      setProfile(res.data.data);
      historyApi.record('user', userId).catch(() => {}); // 记录浏览
    }).catch(() => {}).finally(() => setLoading(false));
  }, [userId]);

  // 拉黑状态
  useEffect(() => {
    if (!myId || userId === myId) return;
    userApi.blockStatus(userId).then((res) => {
      setBlocked(res.data.data.blocked);
      setBlockedBy(res.data.data.blockedBy);
    }).catch(() => {});
  }, [userId, myId]);

  // 陌生人消息状态(决定按钮是 发消息/打招呼/已打招呼)
  useEffect(() => {
    if (!myId || userId === myId) return;
    chatApi.strangerStatus(userId).then((res) => setStranger(res.data.data)).catch(() => {});
  }, [userId, myId]);

  // 对方公开动态
  useEffect(() => {
    setLoadingPosts(true);
    postApi.userPosts(userId).then((res) => {
      setUserPosts(res.data.data.records || []);
    }).catch(() => setUserPosts([])).finally(() => setLoadingPosts(false));
  }, [userId]);

  const handleFollowToggle = async () => {
    if (!profile) return;
    try {
      if (profile.isFollowed) {
        await userApi.unfollow(userId);
        setProfile((prev: User | null) => prev ? { ...prev, isFollowed: false, followerCount: Math.max(0, (prev.followerCount || 0) - 1) } : prev);
      } else {
        await userApi.follow(userId);
        setProfile((prev: User | null) => prev ? { ...prev, isFollowed: true, followerCount: (prev.followerCount || 0) + 1 } : prev);
      }
    } catch { /* 拦截器统一提示 */ }
  };

  const handlePostLike = async (postId: number) => {
    try {
      await postApi.like(postId);
      setUserPosts((prev) => prev.map(p => p.id === postId ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
    } catch { /* 拦截器统一提示 */ }
  };

  const handleBlock = async () => {
    try {
      if (blocked) {
        await userApi.unblock(userId);
        setBlocked(false);
        showToast('已解除拉黑');
      } else {
        await userApi.block(userId);
        setBlocked(true);
        showToast('已拉黑，对方将无法再联系你');
      }
    } catch { /* 错误提示由拦截器统一弹出 */ }
  };

  // 拉取情感简历(已互换则展示内容,否则锁定占位);互换成功后 version 变化自动刷新
  useEffect(() => {
    setResumeLoading(true);
    resumeApi.getOther(userId)
      .then((res) => setResumeView(res.data.data))
      .catch(() => setResumeView(null))
      .finally(() => setResumeLoading(false));
  }, [userId, shareVersion]);

  if (loading) return <div className="up-page"><div className="up-loading">加载中...</div></div>;
  if (!profile) return <div className="up-page"><div className="up-loading">用户不存在</div></div>;

  const handleChat = () => {
    if (!myId) return;
    const name = encodeURIComponent(profile.nickname || '');
    const avatar = encodeURIComponent(profile.avatar || '');
    navigate(`/messages/chat?userId=${userId}&name=${name}&avatar=${avatar}`);
  };

  /** 发送陌生人打招呼消息(对方确认后即可正常聊天) */
  const handleStranger = async () => {
    const content = window.prompt('打个招呼吧（对方确认后即可聊天）', '你好，认识一下');
    if (content === null) return;
    if (!content.trim()) { showToast('消息不能为空'); return; }
    try {
      await chatApi.sendStrangerMessage(userId, content.trim());
      setStranger((prev) => ({ ...prev, sent: true }));
      showToast('已发送打招呼消息，等待对方确认');
    } catch (e) {
      showToast(getErrorMessage(e, '发送失败'));
    }
  };

  /** 主操作按钮:有会话→发消息;对方打招呼→去确认;已打招呼→等待;否则→打招呼 */
  const handleMainAction = () => {
    if (blockedBy) return;
    if (stranger.hasConversation) { handleChat(); return; }
    if (stranger.received) { navigate('/messages/strangers'); return; }
    handleStranger();
  };

  return (
    <div className="up-page">
      <div className="up-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>用户信息</span>
        {myId && userId !== myId && (
          <button className="up-report-btn" onClick={() => setShowReport(true)}>举报</button>
        )}
      </div>

      <div className="up-card">
        <div className="up-avatar">
          {profile.avatar ? <img src={profile.avatar} alt="" /> : <AppIcon name="user" size={34} />}
        </div>
        <div className="up-name">{profile.nickname}</div>
        {profile.signature && <div className="up-bio">{profile.signature}</div>}

        <div className="up-meta">
          {profile.school && (
            <div className="up-meta-item"><AppIcon name="grad" size={14} /> {profile.school}</div>
          )}
          {profile.city && (
            <div className="up-meta-item"><AppIcon name="location" size={14} /> {profile.city}</div>
          )}
          <div className="up-meta-item">
            {profile.gender === 1 ? <AppIcon name="mars" size={14} /> : profile.gender === 2 ? <AppIcon name="venus" size={14} /> : null}
            {profile.gender === 1 ? '男' : profile.gender === 2 ? '女' : '未设置'}
          </div>
        </div>
        <div className="up-stats" style={{ display: 'flex', gap: 24, marginTop: 12, color: '#666', fontSize: 14 }}>
          <span><b style={{ color: '#333' }}>{profile.followingCount || 0}</b> 关注</span>
          <span><b style={{ color: '#333' }}>{profile.followerCount || 0}</b> 粉丝</span>
          <span><b style={{ color: '#333' }}>{profile.postCount || 0}</b> 动态</span>
        </div>
      </div>

      {myId && userId !== myId && (
        <div className="up-actions">
          <button
            onClick={handleFollowToggle}
            style={{
              border: 'none', padding: '0 16px', height: 36, borderRadius: 18, fontSize: 14,
              background: profile.isFollowed ? '#f0f0f0' : '#29241f', color: profile.isFollowed ? '#666' : '#fff',
            }}
          >
            {profile.isFollowed ? '已关注' : '+ 关注'}
          </button>
          <button className="up-chat-btn" onClick={handleMainAction} disabled={blockedBy || stranger.sent}>
            {blockedBy ? '对方已拉黑你'
              : stranger.hasConversation ? '发消息'
              : stranger.sent ? '已打招呼，等待确认'
              : stranger.received ? '去确认打招呼'
              : '打招呼'}
          </button>
          <button
            className="up-block-btn"
            onClick={handleBlock}
            style={{
              border: 'none',
              background: blocked ? '#f0f0f0' : '#f5222d',
              color: blocked ? '#666' : '#fff',
              padding: '0 16px', height: 36, borderRadius: 18, fontSize: 14,
            }}
          >
            {blocked ? '解除拉黑' : '拉黑'}
          </button>
        </div>
      )}

      {/* 情感简历 */}
      <div className="up-resume-section">
        <div className="up-resume-title">情感简历</div>
        {resumeLoading ? (
          <div className="up-resume-loading">加载中...</div>
        ) : resumeView?.infoShared && resumeView.resume ? (
          <ResumeView resume={resumeView.resume} avatar={profile?.avatar} />
        ) : (
          <div className="up-resume-locked">
            <span className="up-lock-icon"><AppIcon name="lock" size={36} /></span>
            <p className="up-lock-main">情感简历未解锁</p>
            <p className="up-lock-hint">
              去聊天里和TA互换详细信息后，就能看到这份专属档案啦
            </p>
          </div>
        )}
      </div>

      {/* TA 的动态 */}
      <div className="up-resume-section">
        <div className="up-resume-title">TA 的动态</div>
        {loadingPosts ? (
          <div className="up-resume-loading">加载中...</div>
        ) : userPosts.length === 0 ? (
          <div style={{ textAlign: 'center', color: '#999', padding: 24 }}>TA 还没有发布动态</div>
        ) : (
          userPosts.map((p) => (
            <PostCard
              key={p.id}
              post={p}
              onLike={() => handlePostLike(p.id)}
              onClick={() => navigate(`/square/post/${p.id}`)}
              canManage={false}
            />
          ))
        )}
      </div>

      {showReport && (
        <ReportDialog
          targetType="resume"
          targetId={userId}
          title="该用户（情感简历/个人介绍）"
          onClose={() => setShowReport(false)}
        />
      )}
    </div>
  );
}
