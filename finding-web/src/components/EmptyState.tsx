import AppIcon, { type AppIconName } from './AppIcon';
import './EmptyState.css';

interface Props { icon?: AppIconName; message?: string; }

export default function EmptyState({ icon = 'inbox', message = '暂无内容' }: Props) {
  return <div className="empty-state"><AppIcon name={icon} size={30} /><p className="empty-message">{message}</p></div>;
}
