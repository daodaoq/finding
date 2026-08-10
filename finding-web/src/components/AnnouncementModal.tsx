import './ConfirmDialog.css';
import './AnnouncementModal.css';
import { renderMarkdown } from '../utils/markdown';
import { formatDateTime } from '../utils/format';
import AppIcon from './AppIcon';

export interface AnnouncementData {
  id: number;
  title: string;
  content: string;
  /** 发布时间(ISO 字符串) */
  createdAt?: string;
}

interface Props {
  /** 全部未读公告,在一个面板内滚动展示 */
  announcements: AnnouncementData[];
  onClose: () => void;
}

/** 本地记录"已读"的最新公告 ID,避免下次启动重复弹出 */
const SEEN_KEY = 'lastAnnouncementId';

export function getLastSeenAnnouncementId(): number {
  return Number(localStorage.getItem(SEEN_KEY) || 0);
}

export function saveSeenAnnouncementId(id: number) {
  localStorage.setItem(SEEN_KEY, String(id));
}

/**
 * 全局系统公告面板 —— 管理员发布公告后实时 WS 推送 / 启动补拉,
 * 多条未读一次滚动展示,支持 Markdown,点「全部知道了」统一标记已读。
 */
export default function AnnouncementModal({ announcements, onClose }: Props) {
  if (announcements.length === 0) return null;

  const maxId = Math.max(...announcements.map((a) => a.id));

  const handleClose = () => {
    saveSeenAnnouncementId(maxId);
    onClose();
  };

  return (
    <div className="confirm-overlay" onClick={handleClose}>
      <div className="confirm-card ann-card" onClick={(e) => e.stopPropagation()}>
        <h4 className="confirm-title ann-title"><AppIcon name="megaphone" size={20} /> 系统公告</h4>
        <div className="ann-list">
          {announcements.map((a) => (
            <div key={a.id} className="ann-item">
              <div className="ann-item-head">
                <span className="ann-item-title">{a.title}</span>
                {a.createdAt && <span className="ann-item-time">{formatDateTime(a.createdAt)}</span>}
              </div>
              <div
                className="ann-item-body"
                dangerouslySetInnerHTML={{ __html: renderMarkdown(a.content) }}
              />
            </div>
          ))}
        </div>
        <div className="confirm-buttons">
          <button className="confirm-btn primary" onClick={handleClose}>全部知道了</button>
        </div>
      </div>
    </div>
  );
}
