import type { ReactNode } from 'react';
import LoadingSkeleton from './LoadingSkeleton';
import EmptyState from './EmptyState';
import AppIcon from './AppIcon';
import './PageState.css';

interface Props {
  loading: boolean;
  error?: string | null;
  empty?: boolean;
  emptyMessage?: string;
  onRetry?: () => void;
  children?: ReactNode;
}

/** 统一页面加载态:loading / error+重试 / empty / 内容 */
export default function PageState({ loading, error, empty, emptyMessage = '暂无数据', onRetry, children }: Props) {
  if (loading) {
    return <div className="page-state"><LoadingSkeleton /><LoadingSkeleton /></div>;
  }
  if (error) {
    return (
      <div className="page-state page-state--error">
        <AppIcon name="info" size={32} />
        <p>{error}</p>
        {onRetry && <button className="page-state-retry" onClick={onRetry}>重试</button>}
      </div>
    );
  }
  if (empty) {
    return <EmptyState message={emptyMessage} />;
  }
  return <>{children}</>;
}
