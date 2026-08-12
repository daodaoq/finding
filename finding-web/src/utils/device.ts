/** 稳定设备指纹(localStorage 持久化),用于注册防批量限流等 */
const KEY = 'finding_device_id';

export function getDeviceId(): string {
  try {
    let id = localStorage.getItem(KEY);
    if (!id) {
      id = (crypto.randomUUID?.() as string | undefined)
        ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
      localStorage.setItem(KEY, id);
    }
    return id;
  } catch {
    // localStorage 不可用(隐私模式等)时,退化为会话级随机 id
    return `anon-${Math.random().toString(36).slice(2, 10)}`;
  }
}
