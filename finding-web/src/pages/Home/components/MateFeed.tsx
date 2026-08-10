import MateCard from '../../../components/MateCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import AppIcon from '../../../components/AppIcon';
import { MATE_CATEGORIES } from '../../../utils/constants';
import type { Mate } from '../../../types/mate';
// 共享信息流样式（分类网格 + 排序栏 + 搭子列表 + .no-more）
import '../../../components/Feed.css';

const MATE_SORT_OPTIONS = [
  { key: 'time', label: '时间最近' },
  { key: 'distance', label: '距离最近' },
] as const;

interface Props {
  mates: Mate[];
  loading: boolean;
  hasMore: boolean;
  category: string;
  sortBy: string;
  onCategoryChange: (code: string) => void;
  onSortChange: (key: string) => void;
  onJoin: (id: number) => void;
  onOpen: (id: number) => void;
}

/** 首页「搭子」Tab 信息流（分类筛选 + 排序 + 搭子卡片列表） */
export default function MateFeed({
  mates,
  loading,
  hasMore,
  category,
  sortBy,
  onCategoryChange,
  onSortChange,
  onJoin,
  onOpen,
}: Props) {
  return (
    <>
      {/* 分类筛选网格 */}
      <div className="square-categories">
        <div className="category-grid">
          {MATE_CATEGORIES.map((cat) => (
            <button
              key={cat.code}
              className={`category-cell ${category === cat.code ? 'active' : ''}`}
              onClick={() => onCategoryChange(cat.code)}
            >
              <span className="category-cell-icon"><AppIcon name={cat.icon} size={16} /></span>
              <span className="category-cell-name">{cat.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 排序选项 */}
      <div className="square-sort-bar">
        <span className="sort-label">
          {category ? MATE_CATEGORIES.find((c) => c.code === category)?.name : '全部搭子'}
        </span>
        <div className="sort-options">
          {MATE_SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              className={`sort-chip ${sortBy === opt.key ? 'active' : ''}`}
              onClick={() => onSortChange(opt.key)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 搭子列表 */}
      <div className="square-mate-list">
        {mates.map((mate) => (
          <MateCard key={mate.id} mate={mate} onJoin={onJoin} onClick={onOpen} />
        ))}
        {loading && <LoadingSkeleton />}
        {!loading && mates.length === 0 && (
          <EmptyState
            icon="search"
            message={
              category
                ? `暂无"${MATE_CATEGORIES.find((c) => c.code === category)?.name}"邀约`
                : '暂无搭子邀约'
            }
          />
        )}
        {!hasMore && mates.length > 0 && <p className="no-more">— 没有更多了 —</p>}
      </div>
    </>
  );
}
