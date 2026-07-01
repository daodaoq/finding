import './EmptyState.css';

interface Props {
  icon?: string;
  message?: string;
}

export default function EmptyState({ icon = '📭', message = '暂无内容' }: Props) {
  return (
    <div className="empty-state">
      <span className="empty-icon">{icon}</span>
      <p className="empty-message">{message}</p>
    </div>
  );
}
