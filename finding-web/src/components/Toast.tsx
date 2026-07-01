import { useEffect, useState } from 'react';
import './Toast.css';

let toastFn: ((msg: string) => void) | null = null;

export function showToast(message: string) {
  toastFn?.(message);
}

export default function ToastContainer() {
  const [msg, setMsg] = useState<string | null>(null);

  useEffect(() => {
    toastFn = setMsg;
    return () => { toastFn = null; };
  }, []);

  useEffect(() => {
    if (msg) {
      const t = setTimeout(() => setMsg(null), 2000);
      return () => clearTimeout(t);
    }
  }, [msg]);

  if (!msg) return null;
  return <div className="toast">{msg}</div>;
}
