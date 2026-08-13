import { adminTokenStorage } from './adminTokenStorage';

/**
 * 触发 CSV 文件下载。
 * 用原生 fetch(而非 axios)绕开拦截器对 JSON 响应的假设,并显式携带 Authorization 头。
 */
export async function downloadCsv(path: string, filename: string): Promise<void> {
  const token = adminTokenStorage.get();
  const res = await fetch(`/api/v1/admin/export/${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('导出失败');
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
