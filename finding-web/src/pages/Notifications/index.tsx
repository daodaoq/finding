import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messageApi } from '../../api/message';
import MessageItem from '../../components/MessageItem';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import { showToast } from '../../components/Toast';
import { useMessageStore } from '../../store/messageStore';
import type { Message } from '../../types/message';
import './index.css';

type TabKey = 'all' | 'like' | 'comment' | 'mate' | 'follow' | 'info_share' | 'system';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'like', label: '点赞' },
  { key: 'comment', label: '评论' },
  { key: 'mate', label: '搭子' },
  { key: 'follow', label: '关注' },
  { key: 'info_share', label: '互换' },
  { key: 'system', label: '系统' },
];

const matchTab = (type: string, tab: TabKey) => {
  if (tab === 'all') return true;
  if (tab === 'mate') return type.startsWith('mate');
  if (tab === 'info_share') return type.startsWith('info_share');
  return type === tab;
};

export default function NotificationsPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [activeTab, setActiveTab] = useState<TabKey>('all');
  const navigate = useNavigate();
  const setUnreadCount = useMessageStore((s) => s.setUnreadCount);

  useEffect(() => {
    setLoading(true);
    setPage(1);
    setMessages([]);
    setHasMore(true);
    messageApi.list({ page: 1, size: 20 })
      .then((res) => {
        setMessages(res.data.data.records || []);
        setHasMore((res.data.data.records?.length || 0) === 20);
      })
      .catch(() => showToast('加载通知失败'))
      .finally(() => setLoading(false));
    // 进入通知页即标记已读
    messageApi.markAllRead().then(() => setUnreadCount(0)).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  const loadMore = async () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    try {
      const next = page + 1;
      const res = await messageApi.list({ page: next, size: 20 });
      setMessages((prev) => [...prev, ...(res.data.data.records || [])]);
      setPage(next);
      setHasMore((res.data.data.records?.length || 0) === 20);
    } catch { /* 忽略 */ }
    finally { setLoadingMore(false); }
  };

  const handleClick = async (msg: Message) => {
    if (msg.type === 'like' || msg.type === 'comment') {
      navigate(`/square/post/${msg.relatedId}`);
    } else if (msg.type === 'mate_request' || msg.type === 'mate_accepted' || msg.type === 'mate_rejected') {
      navigate(`/mate/${msg.relatedId}`);
    } else if (msg.type === 'follow') {
      // 关注通知 → 对方主页
      if (msg.fromUserId) navigate(`/user/${msg.fromUserId}`);
    } else if (msg.type.startsWith('info_share')) {
      if (msg.fromUserId) navigate(`/user/${msg.fromUserId}`);
    }
    // system 类型不做跳转
  };

  const filtered = messages.filter((m) => matchTab(m.type, activeTab));

  return (
    <div className="notifications-page">
      <div className="notif-header">
        <button className="back-btn" onClick={() => navigate('/messages')}>←</button>
        <h2 className="notif-title">互动通知</h2>
        <div style={{ width: 40 }} />
      </div>

      {/* 类型筛选 Tab */}
      <div className="notif-tabs" style={{ display: 'flex', gap: 4, padding: '8px 12px', overflowX: 'auto' }}>
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key)}
            style={{
              flexShrink: 0, padding: '6px 14px', borderRadius: 14, border: 'none', fontSize: 13,
              background: activeTab === t.key ? '#ff6b81' : '#f0f0f0',
              color: activeTab === t.key ? '#fff' : '#666',
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="notif-list">
        {loading && <><LoadingSkeleton /><LoadingSkeleton /></>}
        {!loading && filtered.map((msg) => (
          <MessageItem key={msg.id} message={msg} onClick={handleClick} />
        ))}
        {!loading && filtered.length === 0 && (
          <EmptyState icon="🔔" message="暂无通知" />
        )}
        {!loading && hasMore && (
          <button
            onClick={loadMore}
            style={{ display: 'block', margin: '16px auto', border: 'none', background: '#f0f0f0', color: '#666', padding: '8px 24px', borderRadius: 16, fontSize: 13 }}
          >
            {loadingMore ? '加载中...' : '加载更多'}
          </button>
        )}
      </div>
    </div>
  );
}
