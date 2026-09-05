import type { ReactNode } from 'react';

interface Props {
  title: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}

/** 右侧栏通用卡片壳:布告记号 + 标题 + 发丝分隔 + 内容 */
export default function RailCard({ title, action, children, className }: Props) {
  return (
    <section className={`rail-card ${className ?? ''}`}>
      <header className="rail-card-head"><i className="rail-dot" /><h3>{title}</h3>{action && <span className="rail-card-action">{action}</span>}</header>
      {children}
    </section>
  );
}
