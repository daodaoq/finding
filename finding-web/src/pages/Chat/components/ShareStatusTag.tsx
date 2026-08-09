import { useNavigate } from 'react-router-dom';
import type { InfoShareStatus } from '../../../types/resume';
import './ShareStatusTag.css';

interface Props {
  status: InfoShareStatus['status'];
  targetUserId: number;
  onRequest: () => void;
  onShowConfirm: () => void;
}

/** 私聊页「信息互换」状态标签：已互换 / 已发送申请 / 待处理 / 可互换 */
export default function ShareStatusTag({ status, targetUserId, onRequest, onShowConfirm }: Props) {
  const navigate = useNavigate();

  if (status === 'approved') {
    return (
      <button
        className="share-tag approved"
        onClick={() => navigate(`/user/${targetUserId}`)}
        title="查看TA的情感简历"
      >
        已互换信息
      </button>
    );
  }
  if (status === 'pendingSent') {
    return <span className="share-tag pending">已发送申请</span>;
  }
  if (status === 'pendingReceived') {
    return (
      <button className="share-tag warn" onClick={onShowConfirm}>
        对方申请互换信息
      </button>
    );
  }
  return (
    <button className="share-tag active" onClick={onRequest}>
      互换信息
    </button>
  );
}