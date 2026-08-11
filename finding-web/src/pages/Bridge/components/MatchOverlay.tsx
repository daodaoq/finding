import type { ChatApply } from '../../../types/bridge';
import AppIcon from '../../../components/AppIcon';
import './MatchOverlay.css';

interface Props {
  apply: ChatApply;
  myAvatar: string;
  myNickname: string;
  onGoChat: () => void;
  onClose: () => void;
}

/** 相亲「匹配成功」时刻:聊天申请通过后展示双方名片与去聊天入口 */
export default function MatchOverlay({ apply, myAvatar, myNickname, onGoChat, onClose }: Props) {
  return (
    <div className="match-overlay" onClick={onClose}>
      <div className="match-card" onClick={(e) => e.stopPropagation()}>
        <div className="match-heart"><AppIcon name="heart" size={32} /></div>
        <h3>匹配成功！</h3>
        <p className="match-sub">你们已经开始聊天了</p>
        <div className="match-avatars">
          <div className="match-avatar">
            {myAvatar ? <img src={myAvatar} alt="" /> : <span className="match-avatar-fallback">我</span>}
          </div>
          <span className="match-plus">+</span>
          <div className="match-avatar">
            {apply.fromUserAvatar ? <img src={apply.fromUserAvatar} alt="" /> : <AppIcon name="user" size={26} />}
          </div>
        </div>
        <p className="match-names">{myNickname || '我'} × {apply.fromUserNickname || '对方'}</p>
        <button className="match-go" onClick={onGoChat}>去聊天</button>
        <button className="match-later" onClick={onClose}>继续浏览</button>
      </div>
    </div>
  );
}
