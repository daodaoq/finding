import EmptyState from '../../../components/EmptyState';
import { formatRelativeTime } from '../../../utils/format';
import type { Comment } from '../../../types/comment';
import './CommentList.css';

interface Props {
  comments: Comment[];
  commentCount?: number;
  onLike: (commentId: number) => void;
  onReply: (comment: Comment) => void;
  onReport: (comment: Comment) => void;
}

/** 动态详情 - 评论列表（含子回复） */
export default function CommentList({ comments, commentCount, onLike, onReply, onReport }: Props) {
  return (
    <>
      {/* 评论标题 */}
      <div className="pd-comment-header">
        <span>评论 {commentCount && commentCount > 0 ? `(${commentCount})` : ''}</span>
      </div>

      {/* 评论列表 */}
      <div className="pd-comment-list">
        {comments.length === 0 && <EmptyState icon="💬" message="暂无评论，来说点什么吧" />}
        {comments.map((comment) => (
          <div key={comment.id} className="comment-item">
            <div className="comment-avatar">
              {comment.avatar ? <img src={comment.avatar} alt="" /> : <span>👤</span>}
            </div>
            <div className="comment-body">
              <div className="comment-top">
                <span className="comment-name">{comment.nickname}</span>
                <span className="comment-time">{formatRelativeTime(comment.createdAt)}</span>
              </div>
              <div className="comment-content">
                {comment.parentId && <span className="reply-target">回复 </span>}
                {comment.content}
              </div>
              <div className="comment-actions">
                <button onClick={() => onLike(comment.id)}>
                  {comment.isLiked ? '❤️' : '🤍'} {comment.likeCount || ''}
                </button>
                <button onClick={() => onReply(comment)}>回复</button>
                <button className="comment-report" onClick={() => onReport(comment)}>举报</button>
              </div>

              {/* 子回复 */}
              {comment.replies && comment.replies.length > 0 && (
                <div className="sub-replies">
                  {comment.replies.map((reply) => (
                    <div key={reply.id} className="reply-item">
                      <div className="comment-avatar small">
                        {reply.avatar ? <img src={reply.avatar} alt="" /> : <span>👤</span>}
                      </div>
                      <div className="comment-body">
                        <div className="comment-top">
                          <span className="comment-name">{reply.nickname}</span>
                          <span className="comment-time">{formatRelativeTime(reply.createdAt)}</span>
                        </div>
                        <div className="comment-content">
                          <span className="reply-target">回复 {comment.nickname}: </span>
                          {reply.content}
                        </div>
                        <div className="reply-actions">
                          <button className="comment-report" onClick={() => onReport(reply)}>举报</button>
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
    </>
  );
}