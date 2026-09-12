import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * 头像点击统一入口:进入该用户的个人主页。
 *
 * 只有「个人主页」与「情感简历」两处的照片用图片预览(见 utils/preview)。
 * 当外层行/卡片本身也可点击时,这里会阻止冒泡,避免同时触发外层行为。
 * 无 userId(如群头像)时返回 undefined,保持原样不可点。
 */
export function useProfileClick() {
  const navigate = useNavigate();
  return (userId?: number | null) =>
    userId
      ? (e: MouseEvent) => {
          e.stopPropagation();
          navigate(`/user/${userId}`);
        }
      : undefined;
}
