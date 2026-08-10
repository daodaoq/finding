import type { ReactNode } from 'react';
import AppIcon from '../../../components/AppIcon';
import './ChatHeader.css';

interface Props {
  title: string;
  avatar?: string;
  onBack: () => void;
  /** 标题右侧扩展区（信息互换标签 / 群聊信息按钮等） */
  extra?: ReactNode;
  /** 最右侧图标区 */
  right?: ReactNode;
}

/** 聊天页顶部栏 —— 私聊 / 群聊共用 */
export default function ChatHeader({ title, avatar, onBack, extra, right }: Props) {
  return (
    <div className="chat-header">
      <button className="back-btn" onClick={onBack}>←</button>
      {avatar && (
        <div className="chat-avatar-sm">
          {avatar ? <img src={avatar} alt="" /> : <AppIcon name="user" size={18} />}
        </div>
      )}
      <span className="chat-header-name">{title}</span>
      {extra}
      {right}
    </div>
  );
}