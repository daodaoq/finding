import { useCallback, useEffect, useRef } from 'react';

/** 请求过期(被更新的请求取代)时抛出,调用方应静默忽略 */
export class StaleRequestError extends Error {
  constructor() {
    super('STALE_REQUEST');
    this.name = 'StaleRequestError';
  }
}

/**
 * 判断错误是否属于「可静默忽略」的过期/取消:
 * - 本 hook 的序号保护抛出的 StaleRequestError
 * - axios 请求被 AbortController 中止(code = ERR_CANCELED)
 */
export function isStaleError(e: unknown): boolean {
  if (e instanceof StaleRequestError) return true;
  if (e && typeof e === 'object' && (e as { code?: string }).code === 'ERR_CANCELED') return true;
  return false;
}

interface RunResult<T> {
  /** 已包裹序号保护的 promise;过期时 reject StaleRequestError */
  promise: Promise<T>;
  /** 当前请求是否仍是最新的(用于 loading 收尾只允许最新请求执行) */
  isCurrent: () => boolean;
}

/**
 * 请求竞态守卫 —— 解决「快速切换条件/用户时,较旧请求覆盖最新状态」。
 *
 * - 每次 run() 自动取消上一次未完成请求(把 AbortSignal 透传给 axios `signal` 时真正中断网络)
 * - 序号保护:即便旧请求已 resolve,结果也会被丢弃,只有最新请求能写入状态
 * - 组件卸载时自动中止在途请求,避免卸载后 setState
 *
 * 用法:
 *   const { run } = useStaleGuard();
 *   useEffect(() => {
 *     const { promise, isCurrent } = run((signal) => request.get(url, { signal }));
 *     promise
 *       .then((res) => setData(res.data.data))
 *       .catch((e) => { if (!isStaleError(e)) showToast('加载失败'); })
 *       .finally(() => { if (isCurrent()) setLoading(false); });
 *   }, [run, url]);
 *
 * 注意:每次 run 会取消上一次,独立资源应各自调用一次 useStaleGuard()。
 */
export function useStaleGuard() {
  const controllerRef = useRef<AbortController | null>(null);
  const seqRef = useRef(0);

  // 卸载时中止在途请求
  useEffect(() => () => controllerRef.current?.abort(), []);

  const run = useCallback(<T,>(exec: (signal: AbortSignal) => Promise<T>): RunResult<T> => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    const seq = ++seqRef.current;
    const promise = exec(controller.signal).then((value) => {
      if (seqRef.current !== seq) throw new StaleRequestError();
      return value;
    });
    return { promise, isCurrent: () => seqRef.current === seq };
  }, []);

  return { run };
}
