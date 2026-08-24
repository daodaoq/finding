import { Outlet, useLocation } from 'react-router-dom';
import EmptyState from '../../components/EmptyState';
import { useIsDesktop } from '../../hooks/useIsDesktop';
import ConversationPane from './ConversationPane';
import './index.css';

/**
 * 消息区布局:
 * - 移动端(<768px):直接透传子路由,行为与改造前完全一致(列表页→整页聊天)。
 * - 桌面端(≥768px):双栏——左侧会话栏常驻,右侧渲染当前子路由
 *   (私聊/群聊/通知/设置等);未选中任何会话时显示空态占位。
 *   列表仅存在于左栏一份,切换会话不卸载,WS 推送持续刷新。
 */
export default function MessagesLayout() {
  const isDesktop = useIsDesktop();
  const { pathname } = useLocation();
  if (!isDesktop) return <Outlet />;
  const isIndex = pathname === '/messages';
  return (
    <div className="messages-split">
      <aside className="msg-side-pane"><ConversationPane /></aside>
      <section className="msg-main-pane">
        {isIndex
          ? <div className="chat-empty-placeholder"><EmptyState icon="message" message="选择一个会话开始聊天" /></div>
          : <Outlet />}
      </section>
    </div>
  );
}
