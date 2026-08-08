import { useInfoShareStore } from '../store/infoShareStore';
import { bridgeApi } from '../api/bridge';
import { showToast } from './Toast';
import './ConfirmDialog.css';

/**
 * 全局「互换信息」请求弹窗 —— 收到 WebSocket 推送后在任意页面弹出。
 */
export default function InfoShareModal() {
  const prompt = useInfoShareStore((s) => s.prompt);
  const clearPrompt = useInfoShareStore((s) => s.clearPrompt);
  const bump = useInfoShareStore((s) => s.bump);

  if (!prompt) return null;

  const handle = async (approve: boolean) => {
    const shareId = prompt.shareId;
    try {
      await bridgeApi.infoShareHandle(shareId, approve ? 1 : 2);
      showToast(approve ? '已同意互换详细信息' : '已拒绝互换申请');
    } catch (e: any) {
      showToast(e?.message || '操作失败，请重试');
    } finally {
      clearPrompt();
      bump();
    }
  };

  return (
    <div className="confirm-overlay" onClick={clearPrompt}>
      <div className="confirm-card" onClick={(e) => e.stopPropagation()}>
        <h4 className="confirm-title">互换信息请求</h4>
        <p className="confirm-message">
          「{prompt.fromNickname}」想要和你互换详细信息，是否同意？
        </p>
        <div className="confirm-buttons">
          <button className="confirm-btn cancel" onClick={() => handle(false)}>拒绝</button>
          <button className="confirm-btn primary" onClick={() => handle(true)}>同意</button>
        </div>
      </div>
    </div>
  );
}
