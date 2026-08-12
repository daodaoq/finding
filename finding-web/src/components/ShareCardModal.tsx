import Modal from './Modal';
import './ShareCardModal.css';

interface Props {
  visible: boolean;
  /** 生成好的 PNG dataURL */
  image: string;
  onClose: () => void;
}

/** 分享卡片预览(HTTP 等无法写剪贴板的环境降级):展示图片,引导长按保存到相册 */
export default function ShareCardModal({ visible, image, onClose }: Props) {
  return (
    <Modal visible={visible} title="分享卡片" centered onClose={onClose}>
      <img className="scm-image" src={image} alt="帖子分享卡片" />
      <p className="scm-hint">长按图片保存到相册，再发给朋友</p>
      <button className="scm-done" onClick={onClose}>完成</button>
    </Modal>
  );
}
