import RailUserCard from './RailUserCard';
import RailHotPosts from './RailHotPosts';
import RailMatePicks from './RailMatePicks';
import RailAnnouncements from './RailAnnouncements';
import RailFooter from './RailFooter';
import './rail.css';

/** 首页右侧栏组合件(桌面 ≥768px 由 Home 页以 useIsDesktop 挂载,移动端不渲染)。 */
export default function HomeRail() {
  return (
    <aside className="home-rail">
      <div className="rail-sticky">
        <RailUserCard />
        <RailHotPosts />
        <RailMatePicks />
        <RailAnnouncements />
        <RailFooter />
      </div>
    </aside>
  );
}
