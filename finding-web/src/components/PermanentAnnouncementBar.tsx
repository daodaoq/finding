import { useEffect, useState } from 'react';
import { homeApi, type Announcement } from '../api/home';
import './PermanentAnnouncementBar.css';

const DISMISS_KEY = 'dismissedPermanentAnnouncementId';

interface Props {
  /** 管理员增删改永久公告(WS)时由外部递增,触发重新拉取 */
  refreshKey: number;
}

/**
 * 永久展示公告 —— 页面顶部的小悬浮横条。
 * 关闭后记住该公告 id,不再展示,直到管理员重新发布/变更。
 */
export default function PermanentAnnouncementBar({ refreshKey }: Props) {
  const [announcement, setAnnouncement] = useState<Announcement | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    homeApi.permanentAnnouncement()
      .then((res) => {
        if (cancelled) return;
        setAnnouncement(res.data.data || null);
      })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [refreshKey]);

  const dismiss = () => {
    if (!announcement) return;
    localStorage.setItem(DISMISS_KEY, String(announcement.id));
    setDismissed(true);
  };

  if (!announcement) return null;
  const seenId = Number(localStorage.getItem(DISMISS_KEY) || 0);
  if (dismissed || announcement.id === seenId) return null;

  return (
    <div className="permanent-announcement-bar">
      <span className="pab-title">{announcement.title}</span>
      <span className="pab-content">{announcement.content}</span>
      <button className="pab-close" onClick={dismiss} aria-label="关闭">✕</button>
    </div>
  );
}
