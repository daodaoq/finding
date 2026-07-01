import type { ReactNode } from 'react';
import './Modal.css';

interface Props {
  visible: boolean;
  title?: string;
  children: ReactNode;
  onClose: () => void;
}

export default function Modal({ visible, title, children, onClose }: Props) {
  if (!visible) return null;
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {title && <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>}
        {children}
      </div>
    </div>
  );
}
