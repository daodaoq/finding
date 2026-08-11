/**
 * 时间格式化工具 —— 统一收敛页面内散落的 9 处格式化函数
 */

const pad2 = (n: number) => String(n).padStart(2, '0');

function formatHm(d: Date): string {
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/** 动态/评论时间：刚刚 / X分钟前 / X小时前 / M-D */
export function formatRelativeTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

/** 会话/通知列表时间：刚刚 / X分钟前 / 今天 HH:mm / 昨天 / M-D */
export function formatSessionTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return formatHm(d);
  if (diff < 172800000) return '昨天';
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

/** 聊天时间分隔线：今天 HH:mm / 昨天 HH:mm / M-D HH:mm */
export function formatChatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const date = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const time = formatHm(d);
  if (date.getTime() === today.getTime()) return `今天 ${time}`;
  if (date.getTime() === yesterday.getTime()) return `昨天 ${time}`;
  return `${d.getMonth() + 1}/${d.getDate()} ${time}`;
}

/** 气泡内时间：HH:mm */
export function formatClockTime(dateStr: string): string {
  return formatHm(new Date(dateStr));
}

/** 消息日期分组头：今天 / 昨天 / yyyy年M月d日 */
export function formatDateHeader(dateStr: string): string {
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const date = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  if (date.getTime() === today.getTime()) return '今天';
  if (date.getTime() === yesterday.getTime()) return '昨天';
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
}

/** 完整日期时间：YYYY-MM-DD HH:mm */
export function formatDateTime(dateStr: string): string {
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${formatHm(d)}`;
}