import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { postApi } from '../../api/post';
import { useAuthStore } from '../../store/authStore';
import { useRequireLogin } from '../../hooks/useRequireLogin';
import LoginModal from '../../components/LoginModal';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import EmptyState from '../../components/EmptyState';
import PostCard from '../../components/PostCard';
import { showToast } from '../../components/Toast';
import type { Post } from '../../types/post';
import type { Comment } from '../../types/comment';
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
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadPost();
    loadComments();
  }, [postId]);

  const loadPost = async () => {
    try {
      const res = await postApi.detail(postId);
      setPost(res.data.data);
    } catch { navigate(-1); }
    finally { setLoading(false); }
  };

  const loadComments = async () => {
    try {
      const res = await postApi.getComments(postId);
      setComments(res.data.data.records);
    } catch { /* */ }
  };

  const handleLike = () => {
    requireLogin(async () => {
      if (!post) return;
      try {
        await postApi.like(post.id);
        setPost(prev => prev ? { ...prev, isLiked: !prev.isLiked, likeCount: prev.isLiked ? prev.likeCount - 1 : prev.likeCount + 1 } : null);
      } catch { /* */ }
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
    inputRef.current?.focus();
  };

  const handleCommentLike = (commentId: number) => {
    requireLogin(async () => {
      showToast('评论点赞功能开发中');
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
      </div>

      {/* 帖子内容 */}
      <PostCard post={post} onLike={handleLike} onClick={() => {}} />

      {/* 评论标题 */}
      <div className="pd-comment-header">
        <span>评论 {post.commentCount > 0 ? `(${post.commentCount})` : ''}</span>
      </div>

      {/* 评论列表 */}
      <div className="pd-comment-list">
        {comments.length === 0 && <EmptyState icon="💬" message="暂无评论，来说点什么吧" />}
        {comments.map(comment => (
          <div key={comment.id} className="comment-item">
            <div className="comment-avatar">
              {comment.avatar ? <img src={comment.avatar} alt="" /> : <span>👤</span>}
            </div>
            <div className="comment-body">
              <div className="comment-top">
                <span className="comment-name">{comment.nickname}</span>
                <span className="comment-time">{formatTime(comment.createdAt)}</span>
              </div>
              <div className="comment-content">
                {comment.parentId && <span className="reply-target">回复 </span>}
                {comment.content}
              </div>
              <div className="comment-actions">
                <button onClick={() => handleCommentLike(comment.id)}>
                  {comment.isLiked ? '❤️' : '🤍'} {comment.likeCount || ''}
                </button>
                <button onClick={() => handleReply(comment)}>回复</button>
              </div>

              {/* 子回复 */}
              {comment.replies && comment.replies.length > 0 && (
                <div className="sub-replies">
                  {comment.replies.map(reply => (
                    <div key={reply.id} className="reply-item">
                      <div className="comment-avatar small">
                        {reply.avatar ? <img src={reply.avatar} alt="" /> : <span>👤</span>}
                      </div>
                      <div className="comment-body">
                        <div className="comment-top">
                          <span className="comment-name">{reply.nickname}</span>
                          <span className="comment-time">{formatTime(reply.createdAt)}</span>
                        </div>
                        <div className="comment-content">
                          <span className="reply-target">回复 {comment.nickname}: </span>
                          {reply.content}
                        </div>
                      </div>
                    </div>
                  ))}
                  {comment.replyCount && comment.replyCount > 3 && (
                    <div className="view-all-replies">查看全部 {comment.replyCount} 条回复 ›</div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 底部输入栏 */}
      <div className="pd-input-bar">
        {replyTo && (
          <div className="reply-indicator">
            回复 @{replyTo.name}
            <button onClick={() => setReplyTo(null)}>✕</button>
          </div>
        )}
        <div className="pd-input-row">
          <input
            ref={inputRef}
            className="pd-input"
            type="text"
            placeholder={replyTo ? `回复 ${replyTo.name}...` : '说点什么...'}
            value={inputText}
            onChange={e => setInputText(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSendComment(); }}
          />
          <button className="pd-send-btn" onClick={handleSendComment} disabled={!inputText.trim()}>
            发送
          </button>
        </div>
      </div>

      <LoginModal visible={showLogin} onClose={handleClose} onSuccess={handleLoginSuccess} />
    </div>
  );
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
