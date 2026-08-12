import { useEffect, useRef, useState } from 'react';
import './SliderCaptcha.css';

/** 与后端 PuzzleCaptchaGenerator 一致的尺寸常量(原图坐标) */
const BG_W = 300;
const PIECE = 50;

interface Props {
  /** 背景图 base64(不含 data: 前缀) */
  bg: string;
  /** 拼图块 base64 */
  piece: string;
  /** 拼图块 top(原图坐标),固定不可上下移动 */
  y: number;
  /** 拖动结束:返回拼图块左边缘的 X(原图坐标)与耗时(ms) */
  onDone: (x: number, timeMs: number) => void;
}

/** 滑块拼图验证码 —— 拖动滑块把拼图块对齐到缺口,替换原文本图形验证码 */
export default function SliderCaptcha({ bg, piece, y, onDone }: Props) {
  const boxRef = useRef<HTMLDivElement>(null);
  const [boxW, setBoxW] = useState(BG_W);
  const [dragX, setDragX] = useState(0);
  const [dragging, setDragging] = useState(false);
  const dragStart = useRef({ px: 0, startX: 0, t: 0 });

  useEffect(() => {
    const el = boxRef.current;
    if (el && el.clientWidth > 0) setBoxW(el.clientWidth);
  }, []);

  const scale = boxW / BG_W;
  const pieceW = PIECE * scale;
  const maxX = boxW - pieceW;

  const onDown = (e: React.PointerEvent) => {
    (e.currentTarget as HTMLElement).setPointerCapture?.(e.pointerId);
    dragStart.current = { px: e.clientX, startX: dragX, t: Date.now() };
    setDragging(true);
  };
  const onMove = (e: React.PointerEvent) => {
    if (!dragging) return;
    const dx = e.clientX - dragStart.current.px;
    setDragX(Math.max(0, Math.min(maxX, dragStart.current.startX + dx)));
  };
  const onUp = () => {
    if (!dragging) return;
    setDragging(false);
    const x = Math.round(dragX / scale);   // 换算回原图坐标
    const timeMs = Date.now() - dragStart.current.t;
    onDone(x, timeMs);
  };

  const handlers = { onPointerDown: onDown, onPointerMove: onMove, onPointerUp: onUp, onPointerCancel: onUp };

  return (
    <div className="sc-wrap" ref={boxRef}>
      <div className="sc-bg">
        <img className="sc-bg-img" src={`data:image/png;base64,${bg}`} alt="" draggable={false} />
        <img
          className="sc-piece"
          src={`data:image/png;base64,${piece}`}
          alt=""
          draggable={false}
          style={{ left: dragX, top: y * scale, width: pieceW, height: pieceW }}
          {...handlers}
        />
      </div>
      <div className="sc-track" {...handlers}>
        <div className="sc-track-bg" />
        <div className="sc-knob" style={{ left: dragX }}>⟷</div>
      </div>
      <p className="sc-hint">拖动滑块将拼图放到正确位置</p>
    </div>
  );
}
