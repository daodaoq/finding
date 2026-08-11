/**
 * 统一业务错误对象 —— Axios 拦截器拒绝时抛出,携带 HTTP status、业务 code、可展示文案与是否可重试。
 * 页面统一用 getErrorMessage 解析,避免 catch (e: any) 丢失信息或静默吞错。
 */
export class AppError extends Error {
  status?: number;
  code?: number;
  /** 是否可重试(如网络失败 true;401/参数错误等 false) */
  retryable: boolean;

  constructor(message: string, opts: { status?: number; code?: number; retryable?: boolean } = {}) {
    super(message);
    this.name = 'AppError';
    this.status = opts.status;
    this.code = opts.code;
    this.retryable = opts.retryable ?? true;
  }
}

/** 从任意错误中提取可展示文案(unknown → string,带兜底) */
export function getErrorMessage(e: unknown, fallback = '操作失败'): string {
  if (e instanceof AppError) return e.message;
  if (e instanceof Error) return e.message || fallback;
  if (typeof e === 'string') return e;
  return fallback;
}
