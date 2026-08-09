import './ConfirmDialog.css';
import './AnnouncementModal.css';

interface Props {
  /** 封禁提示文案(含原因/到期时间);null 时不显示 */
  message: string | null;
  onClose: () => void;
}

/**
 * 账号被封禁提示框 —— 收到服务端 ban 推送后弹出,关闭时由外层强制退出登录。
 */
export default function BanModal({ message, onClose }: Props) {
  if (!message) return null;

  return (
    <div className="confirm-overlay" onClick={onClose}>
      <div className="confirm-card ann-card" onClick={(e) => e.stopPropagation()}>
        <h4 className="confirm-title ann-title">🚫 账号已被封禁</h4>
        <div className="ban-body">{message}</div>
        <div className="confirm-buttons">
          <button className="confirm-btn primary" onClick={onClose}>知道了</button>
        </div>
      </div>
    </div>
  );
}
