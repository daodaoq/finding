import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import { historyApi } from '../../api/history';
import { useAuthStore } from '../../store/authStore';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import PostCard from '../../components/PostCard';
import ConfirmDialog from '../../components/ConfirmDialog';
import ReportDialog from '../../components/ReportDialog';
import { showToast } from '../../components/Toast';
import type { Post } from '../../types/post';
import type { Comment } from '../../types/comment';
import CommentList from './components/CommentList';
import CommentInput from './components/CommentInput';
import './index.css';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const postId = Number(id);
  const navigate = useNavigate();
  const user = useAuthStore(s => s.user);
  const { showLogin, requireLogin, handleLoginSuccess, handleClose } = useRequireLogin();

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [inputText, setInputText] = useState('');
  const [replyTo, setReplyTo] = useState<{ id: number; name: string } | null>(null);
  const [keyboardOffset, setKeyboardOffset] = useState(0);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [reportTarget, setReportTarget] = useState<{
    targetType: string; targetId: number; roomId?: number; title: string;
  } | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const replyFocusTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 卸载时清理回复聚焦定时器
  useEffect(() => () => {
    if (replyFocusTimer.current) clearTimeout(replyFocusTimer.current);
  }, []);

  // 删除自己的动态
  const handleDelete = async () => {
    if (!post) return;
    try {
      await postApi.delete(post.id);
      showToast('已删除');
      navigate(-1);
    } catch { showToast('删除失败'); }
    finally { setShowDeleteConfirm(false); }
  };

  // 移动端键盘适配：监听 visualViewport 变化
  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) return;

    const handleResize = () => {
      // 键盘弹出时 visualViewport.height < window.innerHeight
      const offset = window.innerHeight - vv.height;
      setKeyboardOffset(offset > 0 ? offset : 0);
    };

    vv.addEventListener('resize', handleResize);
    vv.addEventListener('scroll', handleResize);
    return () => {
      vv.removeEventListener('resize', handleResize);
      vv.removeEventListener('scroll', handleResize);
    };
  }, []);

  useEffect(() => {
    loadPost();
    loadComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postId]);

  const loadPost = async () => {
    try {
      const res = await postApi.detail(postId);
      setPost(res.data.data);
      historyApi.record('post', postId).catch(() => {}); // 记录浏览
    } catch { navigate(-1); }
    finally { setLoading(false); }
  };

  const loadComments = async () => {
    try {
      const res = await postApi.getComments(postId);
      setComments(res.data.data.records);
    } catch { showToast('加载评论失败'); }
  };

  const handleLike = () => {
    requireLogin(async () => {
      if (!post) return;
      try {
        await postApi.like(post.id);
        setPost(prev => prev ? { ...prev, isLiked: !prev.isLiked, likeCount: prev.isLiked ? prev.likeCount - 1 : prev.likeCount + 1 } : null);
      } catch { showToast('操作失败'); }
    });
  };

  const handleSendComment = () => {
    requireLogin(async () => {
      if (!inputText.trim()) return;
      try {
        const res = await postApi.addComment(postId, inputText.trim(), replyTo?.id);
        const newComment = res.data.data;
        if (replyTo) {
          // 添加子回复
          setComments(prev => prev.map(c =>
            c.id === replyTo.id
              ? { ...c, replies: [...(c.replies || []), newComment], replyCount: (c.replyCount || 0) + 1 }
              : c
          ));
        } else {
          setComments(prev => [newComment, ...prev]);
        }
        if (post) setPost(prev => prev ? { ...prev, commentCount: prev.commentCount + 1 } : null);
        setInputText('');
        setReplyTo(null);
      } catch { showToast('评论失败'); }
    });
  };

  const handleReply = (comment: Comment) => {
    setReplyTo({ id: comment.id, name: comment.nickname });
    setInputText('');
    // 延迟聚焦，确保 DOM 更新后再聚焦
    if (replyFocusTimer.current) clearTimeout(replyFocusTimer.current);
    replyFocusTimer.current = setTimeout(() => {
      inputRef.current?.focus();
      inputRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  };

  const handleDeleteComment = async (comment: Comment) => {
    try {
      await postApi.deleteComment(postId, comment.id);
      // 移除一级评论或其子回复
      setComments(prev => prev.filter(c => c.id !== comment.id).map(c =>
        c.replies ? { ...c, replies: c.replies.filter(r => r.id !== comment.id) } : c));
      if (post) setPost(prev => prev ? { ...prev, commentCount: Math.max(0, prev.commentCount - 1) } : null);
      showToast('评论已删除');
    } catch (e: any) { showToast(e?.message || '删除失败'); }
  };

  const handleCommentLike = (commentId: number) => {
    requireLogin(async () => {
      try {
        await postApi.likeComment(postId, commentId);
        // 更新评论列表中的点赞状态（支持一级评论和子回复）
        setComments(prev => prev.map(c => {
          if (c.id === commentId) {
            return { ...c, isLiked: !c.isLiked, likeCount: c.isLiked ? c.likeCount - 1 : c.likeCount + 1 };
          }
          if (c.replies) {
            return {
              ...c,
              replies: c.replies.map(r =>
                r.id === commentId
                  ? { ...r, isLiked: !r.isLiked, likeCount: r.isLiked ? r.likeCount - 1 : r.likeCount + 1 }
                  : r
              )
            };
          }
          return c;
        }));
      } catch { showToast('操作失败'); }
    });
  };

  if (loading) return <div className="post-detail-page"><LoadingSkeleton /></div>;
  if (!post) return null;

  return (
    <div className="post-detail-page">
      {/* Header */}
      <div className="pd-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h3>动态详情</h3>
        <button
          className="pd-report-btn"
          onClick={() => setReportTarget({ targetType: 'post', targetId: post.id, title: '该动态' })}
        >
          举报
        </button>
      </div>

      {/* 动态内容 */}
      <PostCard
        post={post}
        onLike={handleLike}
        onClick={() => {}}
        canManage={post.userId === user?.id}
        onEdit={() => navigate(`/create-post?id=${post.id}`)}
        onDelete={() => setShowDeleteConfirm(true)}
      />

      {/* 评论列表 */}
      <CommentList
        comments={comments}
        commentCount={post.commentCount}
        currentUserId={user?.id}
        onLike={handleCommentLike}
        onReply={handleReply}
        onReport={(c) => setReportTarget({ targetType: 'comment', targetId: c.id, title: '该评论' })}
        onDelete={handleDeleteComment}
      />

      {/* 底部输入栏 — 移动端键盘适配 */}
      <CommentInput
        inputText={inputText}
        onInputChange={setInputText}
        onSend={handleSendComment}
        replyTo={replyTo}
        onCancelReply={() => setReplyTo(null)}
        bottom={keyboardOffset}
        inputRef={inputRef}
      />

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />

      <ConfirmDialog
        visible={showDeleteConfirm}
        title="删除动态"
        message="确定删除这条动态吗？删除后不可恢复。"
        confirmText="删除"
        danger
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteConfirm(false)}
      />

      {reportTarget && (
        <ReportDialog
          targetType={reportTarget.targetType}
          targetId={reportTarget.targetId}
          roomId={reportTarget.roomId}
          title={reportTarget.title}
          onClose={() => setReportTarget(null)}
        />
      )}
    </div>
  );
}
