import { formatSessionTime } from '../../../utils/format';
import './SearchView.css';

import type { ChatMessageDTO } from '../../../types/message';

interface Props {
  nickname: string;
  myId?: number;
  keyword: string;
  onKeywordChange: (v: string) => void;
  searching: boolean;
  results: ChatMessageDTO[] | null;
  onSearch: () => void;
  onBack: () => void;
}

/** 聊天信息页 - 查找聊天记录视图 */
export default function SearchView({
  nickname,
  myId,
  keyword,
  onKeywordChange,
  searching,
  results,
  onSearch,
  onBack,
}: Props) {
  return (
    <div className="cs-page">
      <div className="cs-header">
        <button className="back-btn" onClick={onBack}>←</button>
        <span>查找聊天记录</span>
      </div>
      <div className="cs-search-bar">
        <input
          className="cs-search-input"
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onSearch()}
          placeholder="输入关键词搜索"
          autoFocus
        />
        <button className="cs-search-btn" onClick={onSearch} disabled={searching || !keyword.trim()}>
          {searching ? '搜索中' : '搜索'}
        </button>
      </div>
      <div className="cs-search-results">
        {results === null && <p className="cs-empty">输入关键词搜索聊天记录</p>}
        {results !== null && results.length === 0 && <p className="cs-empty">没有找到相关聊天记录</p>}
        {results?.map((m) => (
          <div key={m.id} className="cs-result-item">
            <span className="cs-result-name">{m.fromUserId === myId ? '我' : nickname}</span>
            <span className="cs-result-text">{m.messageType === 'image' ? '[图片]' : m.messageType === 'video' ? '[视频]' : m.content}</span>
            <span className="cs-result-time">{formatSessionTime(m.createdAt)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}