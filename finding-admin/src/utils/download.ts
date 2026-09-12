import { adminTokenStorage } from './adminTokenStorage';

/**
 * 下载管理端受保护的文件(二进制)。
 * 用原生 fetch(而非 axios)绕开拦截器对 JSON 响应的假设,并显式携带 Authorization 头。
 */
export async function downloadAdminFile(url: string, filename: string): Promise<void> {
  const token = adminTokenStorage.get();
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('下载失败');
  const blob = await res.blob();
  const objectUrl = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = objectUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(objectUrl);
}

/** 触发 CSV 导出下载(数据导出页使用) */
export async function downloadCsv(path: string, filename: string): Promise<void> {
  await downloadAdminFile(`/api/v1/admin/export/${path}`, filename);
}

/** 下载违禁词导入模板(.xlsx) */
export async function downloadForbiddenWordTemplate(): Promise<void> {
  await downloadAdminFile('/api/v1/admin/forbidden-words/import-template', 'forbidden-words-template.xlsx');
}
