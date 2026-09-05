import { Link } from 'react-router-dom';
import { APP_CONFIG } from '../../utils/config';
import './rail.css';

/** 右侧栏页脚:关于/协议/帮助链接 + 版本号 */
export default function RailFooter() {
  return (
    <div className="rail-footer">
      <nav className="rail-footer-links">
        <Link to="/mine/about">关于我们</Link>
        <Link to="/legal/terms">用户协议</Link>
        <Link to="/legal/privacy">隐私政策</Link>
        <Link to="/mine/help">帮助</Link>
      </nav>
      <p className="rail-footer-version">Finding {APP_CONFIG.VERSION} · {APP_CONFIG.SCHOOL_NAME}</p>
    </div>
  );
}
