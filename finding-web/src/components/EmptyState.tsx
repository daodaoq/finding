import type { ReactNode } from 'react';
import AppIcon, { type AppIconName } from './AppIcon';
import './EmptyState.css';

interface Props { icon?: AppIconName; message?: string; action?: ReactNode; }

export default function EmptyState({ icon = 'inbox', message = '暂无内容', action }: Props) {
  return (
    <div className="empty-state">
      <AppIcon name={icon} size={30} />
      <p className="empty-message">{message}</p>
      {action && <div className="empty-action">{action}</div>}
    </div>
  );
}
