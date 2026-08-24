import { useEffect, useState } from 'react';

/** 必须与 src/index.css 响应式令牌、src/styles/desktop.css 的断点保持一致 */
const DESKTOP_QUERY = '(min-width: 768px)';

/** 是否处于桌面布局(≥768px)。仅用于 CSS 媒体查询覆盖不了的结构性分支(如消息双栏)。 */
export function useIsDesktop(): boolean {
  const [isDesktop, setIsDesktop] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(DESKTOP_QUERY).matches
  );

  useEffect(() => {
    const mql = window.matchMedia(DESKTOP_QUERY);
    const onChange = (e: MediaQueryListEvent) => setIsDesktop(e.matches);
    mql.addEventListener('change', onChange);
    return () => mql.removeEventListener('change', onChange);
  }, []);

  return isDesktop;
}
