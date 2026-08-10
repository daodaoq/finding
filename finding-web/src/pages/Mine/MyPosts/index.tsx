import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postApi } from '../../../api/post';
import { useAuthStore } from '../../../store/authStore';
import PostCard from '../../../components/PostCard';
import LoadingSkeleton from '../../../components/LoadingSkeleton';
import EmptyState from '../../../components/EmptyState';
import ConfirmDialog from '../../../components/ConfirmDialog';
import { showToast } from '../../../components/Toast';
import { useInfiniteList } from '../../../hooks/useInfiniteList';
import type { Post } from '../../../types/post';
import '../subpage.css';

export default function MyPostsPage() {
  const navigate = useNavigate();
  const currentUser = useAuthStore(s => s.user);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const { items: posts, loading, hasMore, setItems: setPosts, loadMore } =
    useInfiniteList<Post, []>({
      fetcher: async (p) => {
        const res = await postApi.myPosts(p, 10);
        return res.data.data;
      },
      onError: () => showToast('加载失败'),
    });

  const handleLike = async (id: number) => {
    try {
      await postApi.like(id);
      setPosts(prev => prev.map(p => p.id === id ? { ...p, isLiked: !p.isLiked, likeCount: p.isLiked ? p.likeCount - 1 : p.likeCount + 1 } : p));
    } catch { showToast('操作失败'); }
  };

  const handleDelete = async () => {
    if (deleteTarget == null) return;
    try {
      await postApi.delete(deleteTarget);
      showToast('已删除');
      setPosts(prev => prev.filter(p => p.id !== deleteTarget));
    } catch { showToast('删除失败'); }
    finally { setDeleteTarget(null); }
  };

  return (
    <div className="subpage">
      <div className="subpage-header">
        <button className="back-btn" onClick={() => navigate('/mine')}>←</button>
        <h2>我的动态</h2>
      </div>
      <div className="subpage-list">
        {loading && <LoadingSkeleton />}
        {!loading && posts.map(p => (
          <div key={p.id}>
            {(p.reviewStatus === 1 || p.reviewStatus === 2) && (
              <div style={{
                background: p.reviewStatus === 2 ? '#fff1f0' : '#fff7e6',
                color: p.reviewStatus === 2 ? '#f5222d' : '#d46b08',
                fontSize: 12, padding: '6px 16px',
              }}>
                {p.reviewStatus === 1
                  ? '审核中，暂不对他人可见'
                  : `审核未通过：${p.reviewReason || '未通过'}`}
              </div>
            )}
            <PostCard
              post={p}
              onLike={handleLike}
              onClick={id => navigate(`/square/post/${id}`)}
              canManage={p.userId === currentUser?.id}
              onEdit={id => navigate(`/create-post?id=${id}`)}
            onDelete={id => setDeleteTarget(id)}
            />
          </div>
        ))}
        {!loading && posts.length === 0 && <EmptyState message="还没有发布过动态" />}
        {hasMore && posts.length > 0 && (
          <button className="load-more-btn" onClick={loadMore} disabled={loading}>
            {loading ? '加载中...' : '加载更多'}
          </button>
        )}
      </div>

      <ConfirmDialog
        visible={deleteTarget != null}
        title="删除动态"
        message="确定删除这条动态吗？删除后不可恢复。"
        confirmText="删除"
        danger
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
