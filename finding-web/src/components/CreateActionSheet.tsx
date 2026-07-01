import './CreateActionSheet.css';

interface Props {
  visible: boolean;
  onClose: () => void;
  onCreatePost: () => void;
  onCreateMate: () => void;
}

const OPTIONS = [
  { key: 'post', icon: '📝', label: '发帖子', desc: '分享校园生活动态' },
  { key: 'mate', icon: '🤝', label: '找搭子', desc: '发布搭子邀约' },
] as const;

export default function CreateActionSheet({ visible, onClose, onCreatePost, onCreateMate }: Props) {
  if (!visible) return null;

  const handleSelect = (key: string) => {
    onClose();
    if (key === 'post') onCreatePost();
    else onCreateMate();
  };

  return (
    <div className="action-sheet-overlay" onClick={onClose}>
      <div className="action-sheet" onClick={e => e.stopPropagation()}>
        <div className="action-sheet-header">选择发布类型</div>
        <div className="action-sheet-options">
          {OPTIONS.map(opt => (
            <button key={opt.key} className="as-option" onClick={() => handleSelect(opt.key)}>
              <span className="as-option-icon">{opt.icon}</span>
              <div className="as-option-info">
                <span className="as-option-label">{opt.label}</span>
                <span className="as-option-desc">{opt.desc}</span>
              </div>
              <span className="as-option-arrow">›</span>
            </button>
          ))}
        </div>
        <button className="as-cancel" onClick={onClose}>取消</button>
      </div>
    </div>
  );
}
