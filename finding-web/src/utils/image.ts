/**
 * 图片前端压缩 —— 宽高超过 maxDim 时等比缩放,PNG 保留透明,其余转 JPEG。
 * 小图(<500KB 且无需缩放)直接原样返回,避免无谓开销。
 */
export function compressImage(file: File, maxDim = 1920, quality = 0.85): Promise<File> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      let { width, height } = img;
      const scale = Math.min(1, maxDim / Math.max(width, height));
      if (scale >= 1 && file.size < 500 * 1024) {
        resolve(file); // 小图不压缩
        return;
      }
      width = Math.round(width * scale);
      height = Math.round(height * scale);
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) { resolve(file); return; }
      ctx.drawImage(img, 0, 0, width, height);
      const outType = file.type === 'image/png' ? 'image/png' : 'image/jpeg';
      canvas.toBlob((blob) => {
        if (blob) {
          const ext = outType === 'image/png' ? '.png' : '.jpg';
          resolve(new File([blob], file.name.replace(/\.\w+$/, ext), { type: outType }));
        } else {
          resolve(file);
        }
      }, outType, quality);
    };
    img.onerror = () => { URL.revokeObjectURL(url); resolve(file); };
    img.src = url;
  });
}
