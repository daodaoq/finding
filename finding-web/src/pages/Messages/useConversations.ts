import { useCallback, useEffect, useRef, useState } from 'react';
import { messageApi } from '../../api/message';
import { chatApi } from '../../api/chat';
import { groupChatApi } from '../../api/groupChat';
import { showToast } from '../../components/Toast';
import { useMessageStore } from '../../store/messageStore';
import { useWebSocket, useWsReconnect } from '../../hooks/useWebSocket';
import { getErrorMessage } from '../../utils/appError';
import { useAuthStore } from '../../store/authStore';
import type { Conversation } from '../../types/message';
import type { GroupChat } from '../../types/groupChat';

/**
 * 消息中心数据层:会话/群聊/隐藏会话/陌生人消息计数 + 未读角标。
 * 自 MessagesPage 原样抽出(逻辑未改),供移动端整页与桌面左栏共用。
 */
export function useConversations() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [groups, setGroups] = useState<GroupChat[]>([]);
  const [hiddenConversations, setHiddenConversations] = useState<Conversation[]>([]);
  const [strangerCount, setStrangerCount] = useState(0);
  const [convError, setConvError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true); const [refreshing, setRefreshing] = useState(false);
  const setUnreadCount = useMessageStore((state) => state.setUnreadCount);
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const loadConversations = async (silent = false) => {
    silent ? setRefreshing(true) : setLoading(true);
    try {
      const response = await chatApi.listConversations();
      setConversations(response.data.data || []);
      setConvError(null);
    } catch (e) {
      // 首次加载失败→错误态+重试;后台刷新失败保留旧数据并提示
      if (conversations.length === 0) setConvError(getErrorMessage(e, '加载会话列表失败'));
      else showToast(getErrorMessage(e, '加载会话列表失败'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };
  const loadGroups = async () => { try { const response = await groupChatApi.listMyGroups(); setGroups(response.data.data || []); } catch { showToast('加载群聊列表失败'); } };
  const loadHiddenConversations = async () => { try { const response = await chatApi.listHiddenConversations(); setHiddenConversations(response.data.data || []); } catch {} };
  const loadStrangerCount = async () => { try { const response = await chatApi.listStrangerMessages(); setStrangerCount((response.data.data || []).length); } catch {} };
  const loadUnreadCount = async () => { try { const response = await messageApi.unreadCount(); setUnreadCount(response.data.data.count); } catch {} };
  useWebSocket(useCallback((message) => {
    if (message.type === 'chat') {
      loadConversations(true);
      // 未读总角标由 MainLayout 的 refreshTotal() 统一从服务端拉取,这里不再本地 +1(避免双重计数与闭包过期)
    }
  }, [loadConversations]));
  useEffect(() => { if (isLoggedIn) { loadConversations(); loadGroups(); loadHiddenConversations(); loadStrangerCount(); loadUnreadCount(); } else setLoading(false); }, [isLoggedIn]);
  const refresh = async () => { await loadConversations(true); await loadUnreadCount(); };
  // 断线补偿:WS 重连成功后刷新会话列表与未读
  const refreshRef = useRef(refresh); refreshRef.current = refresh;
  useWsReconnect(() => refreshRef.current());
  // 回前台时刷新会话列表(补拉断线/后台期间的消息)
  useEffect(() => {
    const onFocus = () => { if (isLoggedIn) refreshRef.current(); };
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [isLoggedIn]);
  return { conversations, groups, hiddenConversations, strangerCount, convError, loading, refreshing, isLoggedIn, loadConversations, refresh };
}
