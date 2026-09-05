import { useEffect, useState } from 'react';
import { homeApi } from '../../api/home';
import type { Announcement } from '../../api/home';
import RailCard from './RailCard';
import './rail.css';

/** 公告:取全量按 id 倒序取最新 2 条(校园公告量级小,客户端过滤即可)。 */
export default function RailAnnouncements() {
  const [list, setList] = useState<Announcement[] | null>(null);

  useEffect(() => {
    let alive = true;
    homeApi.announcements(0)
      .then((res) => {
        if (!alive) return;
        const all = res.data.data || [];
        setList([...all].sort((a, b) => b.id - a.id).slice(0, 2));
      })
      .catch(() => { if (alive) setList([]); });
    return () => { alive = false; };
  }, []);

  return (
    <RailCard title="公告">
      {list === null && <p className="rail-empty">加载中...</p>}
      {list !== null && list.length === 0 && <p className="rail-empty">暂无公告</p>}
      {list !== null && list.map((a) => (
        <div key={a.id} className="rail-ann-row">
          <b>{a.title}</b>
          <time>{formatMD(a.createdAt)}</time>
        </div>
      ))}
    </RailCard>
  );
}

function formatMD(input?: string): string {
  if (!input) return '';
  const d = new Date(input.indexOf(' ') > 0 ? input.replace(' ', 'T') : input);
  if (Number.isNaN(d.getTime())) return input.slice(5, 10);
  return `${d.getMonth() + 1}月${d.getDate()}日`;
}
