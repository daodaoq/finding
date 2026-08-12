import type { ReactNode } from 'react';
import './Modal.css';

interface Props {
  visible: boolean;
  title?: string;
  children: ReactNode;
  onClose: () => void;
  /** 悬浮于屏幕中央(默认底部弹窗) */
  centered?: boolean;
}

export default function Modal({ visible, title, children, onClose, centered = false }: Props) {
  if (!visible) return null;
  return (
    <div className={`modal-overlay ${centered ? 'modal-overlay--centered' : ''}`} onClick={onClose}>
      <div className={`modal-content ${centered ? 'modal-content--centered' : ''}`} onClick={(e) => e.stopPropagation()}>
        {title && <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>}
        {children}
      </div>
    </div>
  );
}
