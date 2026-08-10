import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { resumeApi } from '../../api/resume';
import { uploadApi } from '../../api/upload';
import { showToast } from '../../components/Toast';
import LoadingSkeleton from '../../components/LoadingSkeleton';
import AppIcon, { type AppIconName } from '../../components/AppIcon';
import type { UserResume } from '../../types/resume';
import './index.css';

/** 情感简历编辑页 —— 9 个卡片竖向排列 + 相册上传/拖拽排序/删除,输入框提示语来自填写模板 */
export default function ResumeEditPage() {
  const [form, setForm] = useState<Partial<UserResume>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const dragIndex = useRef<number | null>(null);
  const navigate = useNavigate();

  const set = <K extends keyof UserResume>(key: K, value: UserResume[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  useEffect(() => {
    resumeApi.getMine().then((res) => {
      const data = res.data.data;
      if (data) setForm(data as Partial<UserResume>);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      await resumeApi.save(form);
      showToast('保存成功');
      navigate(-1);
    } catch (e: any) {
      showToast(e?.message || '保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  // ── 相册上传 ──
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;
    setUploading(true);
    try {
      const urls: string[] = [];
      for (const f of files) {
        const res = await uploadApi.uploadImage(f);
        urls.push(res.data.data);
      }
      setForm((prev) => ({ ...prev, photoAlbum: [...(prev.photoAlbum || []), ...urls] }));
      showToast('上传成功');
    } catch {
      showToast('上传失败，请重试');
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const removePhoto = (index: number) => {
    setForm((prev) => ({
      ...prev,
      photoAlbum: (prev.photoAlbum || []).filter((_, i) => i !== index),
    }));
  };

  const movePhoto = (index: number, dir: -1 | 1) => {
    setForm((prev) => {
      const arr = [...(prev.photoAlbum || [])];
      const to = index + dir;
      if (to < 0 || to >= arr.length) return prev;
      [arr[index], arr[to]] = [arr[to], arr[index]];
      return { ...prev, photoAlbum: arr };
    });
  };

  // ── 拖拽排序(桌面端) ──
  const handleDragStart = (index: number) => { dragIndex.current = index; };
  const handleDragOver = (e: React.DragEvent) => { e.preventDefault(); };
  const handleDrop = (index: number) => {
    const from = dragIndex.current;
    dragIndex.current = null;
    if (from === null || from === index) return;
    setForm((prev) => {
      const arr = [...(prev.photoAlbum || [])];
      const [moved] = arr.splice(from, 1);
      arr.splice(index, 0, moved);
      return { ...prev, photoAlbum: arr };
    });
  };

  if (loading) {
    return (
      <div className="re-page">
        <div className="re-header">
          <button className="back-btn" onClick={() => navigate(-1)}>←</button>
          <span>情感简历</span>
        </div>
        <LoadingSkeleton />
      </div>
    );
  }

  return (
    <div className="re-page">
      <div className="re-header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <span>情感简历</span>
        <button className="re-save" onClick={handleSave} disabled={saving}>
          {saving ? '保存中' : '保存'}
        </button>
      </div>

      <div className="re-body">
        {/* 板块1 基础信息栏 */}
        <Card icon="user" title="基础信息栏">
          <div className="re-field">
            <label className="re-field-label">性别</label>
            <select
              className="re-input"
              value={form.gender ?? ''}
              onChange={(e) => set('gender', e.target.value ? Number(e.target.value) : undefined)}
            >
              <option value="">未设置</option>
              <option value={1}>男</option>
              <option value={2}>女</option>
            </select>
          </div>
          <Field label="年龄" type="number" value={form.age} onChange={(v) => set('age', v)} placeholder="如 23" />
          <Field label="生日" type="date" value={form.birthday ?? ''} onChange={(v) => set('birthday', v)} />
          <Field label="星座" value={form.constellation ?? ''} onChange={(v) => set('constellation', v)} placeholder="如 金牛座" />
          <Field label="身高(cm)" type="number" value={form.heightCm} onChange={(v) => set('heightCm', v)} placeholder="如 178" />
          <Field label="体重(kg)" type="number" value={form.weightKg} onChange={(v) => set('weightKg', v)} placeholder="如 68" />
          <Field label="城市" value={form.hometown ?? ''} onChange={(v) => set('hometown', v)} placeholder="如 山东淄博" />
          <Field label="校区" value={form.campus ?? ''} onChange={(v) => set('campus', v)} placeholder="如 西校区" />
          <Field label="专业年级" value={form.majorGrade ?? ''} onChange={(v) => set('majorGrade', v)} placeholder="如 计算机学院 大四" />
          <Field label="职业" value={form.career ?? ''} onChange={(v) => set('career', v)} placeholder="如 程序员实习生 / 学生" />
          <Field label="日常作息" value={form.dailyRoutine ?? ''} onChange={(v) => set('dailyRoutine', v)} placeholder="如 早睡早起 / 晚上 12 点前必睡" />
          <Field label="恋爱状态" textarea value={form.relationshipStatus ?? ''} onChange={(v) => set('relationshipStatus', v)} placeholder="单身多久了？期待奔结婚 / 长久陪伴 / 轻松恋爱？" />
          <Field label="择偶核心底线" textarea value={form.coreBottomLine ?? ''} onChange={(v) => set('coreBottomLine', v)} placeholder="绝对不能接受的事，如：欺骗、冷暴力、养鱼、暧昧不清" />
        </Card>

        {/* 板块2 自我画像 */}
        <Card icon="palette" title="自我画像 · 我是一个什么样的人">
          <Field label="性格优点" textarea value={form.personalityTraits ?? ''} onChange={(v) => set('personalityTraits', v)} placeholder="如：共情力、细心、情绪稳定、有责任感、粘人程度、微拖延等（真实不完美）" />
          <Field label="小缺点" textarea value={form.flaws ?? ''} onChange={(v) => set('flaws', v)} placeholder="如：慢热、偶尔敏感、不会主动、轻微拖延" />
          <Field label="个人三观" textarea value={form.worldview ?? ''} onChange={(v) => set('worldview', v)} placeholder="金钱观、消费观、婚恋观、家庭观念、吵架处理方式" />
          <Field label="个人标签" textarea value={form.personalTags ?? ''} onChange={(v) => set('personalTags', v)} placeholder="如：爱吃醋但讲道理、偏爱双向奔赴、不冷暴力" />
          <Field label="MBTI 人格" value={form.mbti ?? ''} onChange={(v) => set('mbti', v)} placeholder="如 ENFP（可填可不填）" />
          <Field label="恋爱中的样子" textarea value={form.inLoveLook ?? ''} onChange={(v) => set('inLoveLook', v)} placeholder="恋爱时会是什么状态？" />
        </Card>

        {/* 板块3 过往恋爱复盘 */}
        <Card icon="thought" title="过往恋爱复盘 · 核心亮点，不吐槽前任">
          <Field label="恋爱次数" value={form.relationshipCount ?? ''} onChange={(v) => set('relationshipCount', v)} placeholder="如 2次 / 0次" />
          <Field label="分手核心原因" textarea value={form.breakupReason ?? ''} onChange={(v) => set('breakupReason', v)} placeholder="客观描述，不诋毁" />
          <Field label="恋爱短板" textarea value={form.loveShortcoming ?? ''} onChange={(v) => set('loveShortcoming', v)} placeholder="自己在这段关系里的不足" />
          <Field label="从前感情里学到的东西" textarea value={form.loveInsight ?? ''} onChange={(v) => set('loveInsight', v)} placeholder="如：学会主动沟通、懂得换位思考、不再一味讨好、重视仪式感" />
          <Field label="自己在感情里的成长" textarea value={form.loveGrowth ?? ''} onChange={(v) => set('loveGrowth', v)} placeholder="改掉了哪些毛病？如：不再冷暴力、学会及时表达" />
        </Card>

        {/* 板块4 恋爱相处模式 */}
        <Card icon="handshake" title="恋爱相处模式 · 对方最关心的部分">
          <Field label="日常陪伴" textarea value={form.dailyCompany ?? ''} onChange={(v) => set('dailyCompany', v)} placeholder="空闲时间怎么分配？能不能秒回？忙的时候怎么报备？" />
          <Field label="吵架模式" textarea value={form.fightMode ?? ''} onChange={(v) => set('fightMode', v)} placeholder="不冷战、愿意低头沟通、当天矛盾当天解决" />
          <Field label="表达爱意方式" textarea value={form.loveExpression ?? ''} onChange={(v) => set('loveExpression', v)} placeholder="行动派 / 语言浪漫？擅长送礼物？喜欢见面陪伴？" />
          <Field label="与异性边界" textarea value={form.oppositeBoundary ?? ''} onChange={(v) => set('oppositeBoundary', v)} placeholder="和异性朋友的相处尺度、分寸感" />
        </Card>

        {/* 板块5 个人生活与规划 */}
        <Card icon="sprout" title="个人生活与规划 · 展示长期稳定性">
          <Field label="爱好与日常" textarea value={form.hobbies ?? ''} onChange={(v) => set('hobbies', v)} placeholder="休闲娱乐、运动、美食、追剧、旅行、学习提升" />
          <Field label="日常状态" textarea value={form.dailyStatus ?? ''} onChange={(v) => set('dailyStatus', v)} placeholder="平时的一天大致怎么过" />
          <Field label="生活习惯" textarea value={form.lifeHabits ?? ''} onChange={(v) => set('lifeHabits', v)} placeholder="作息、爱好、习惯" />
          <Field label="短期规划" textarea value={form.shortTermPlan ?? ''} onChange={(v) => set('shortTermPlan', v)} placeholder="工作发展、定居城市、年度目标" />
          <Field label="长期婚恋规划" textarea value={form.marriagePlan ?? ''} onChange={(v) => set('marriagePlan', v)} placeholder="多久考虑确定关系？谈多久打算见家长？结婚节奏？" />
        </Card>

        {/* 板块6 理想另一半 */}
        <Card icon="heart" title="理想另一半 · 择偶要求">
          <Field label="硬性条件（底线）" textarea value={form.hardConditions ?? ''} onChange={(v) => set('hardConditions', v)} placeholder="年龄范围、城市、是否同城、有无不良嗜好（酗酒、冷暴力、暧昧不清）" />
          <Field label="软性期待（灵魂契合）" textarea value={form.softExpectations ?? ''} onChange={(v) => set('softExpectations', v)} placeholder="性格、情绪稳定性、沟通习惯、对待感情的态度、三观契合点" />
        </Card>

        {/* 板块7 加分项 */}
        <Card icon="star" title="加分项 · 我能为恋爱带来什么">
          <Field label="情绪价值" textarea value={form.bonusPoints ?? ''} onChange={(v) => set('bonusPoints', v)} placeholder="永远站在对方这边、倾听烦恼、缓解焦虑；实际付出：会做饭、擅长规划旅行、细心记住纪念日；未来规划：愿意把对方规划进自己的人生" />
        </Card>

        {/* 板块8 走心宣言 */}
        <Card icon="mail" title="走心宣言">
          <Field label="对爱情的期待" textarea value={form.loveExpectation ?? ''} onChange={(v) => set('loveExpectation', v)} placeholder="期待一段怎样的感情" />
          <Field label="对新恋情的态度及承诺" textarea value={form.loveAttitude ?? ''} onChange={(v) => set('loveAttitude', v)} placeholder="承诺不养鱼、认真专一" />
        </Card>

        {/* 板块9 生活相册 */}
        <Card icon="image" title="生活相册">
          <p className="re-album-hint">展示生活随拍，可拖拽排序或点箭头调整顺序，点击 ✕ 删除</p>
          <div className="re-album">
            {(form.photoAlbum || []).map((url, i) => (
              <div
                key={i}
                className="re-album-item"
                draggable
                onDragStart={() => handleDragStart(i)}
                onDragOver={handleDragOver}
                onDrop={() => handleDrop(i)}
              >
                <img src={url} alt="" />
                <button className="re-album-del" onClick={() => removePhoto(i)}>✕</button>
                <div className="re-album-move">
                  <button onClick={() => movePhoto(i, -1)}>‹</button>
                  <button onClick={() => movePhoto(i, 1)}>›</button>
                </div>
                <span className="re-album-drag">⠿</span>
              </div>
            ))}
            <div className="re-album-add" onClick={() => fileRef.current?.click()}>
              {uploading ? <span className="re-album-uploading">上传中...</span> : <span className="re-album-plus">＋</span>}
            </div>
          </div>
          <input ref={fileRef} type="file" accept="image/*" multiple style={{ display: 'none' }} onChange={handleFileChange} />
        </Card>

        <button className="re-submit" onClick={handleSave} disabled={saving}>
          {saving ? '保存中...' : '保存情感简历'}
        </button>
      </div>
    </div>
  );
}

function Card({ icon = 'book', title, children }: { icon?: AppIconName; title: string; children: React.ReactNode }) {
  return (
    <section className="re-card">
      <h3 className="re-card-title"><AppIcon name={icon} size={18} />{title}</h3>
      {children}
    </section>
  );
}

function Field({ label, value, onChange, type = 'text', textarea, placeholder }: {
  label: string; value: any; onChange: (v: any) => void; type?: string; textarea?: boolean; placeholder?: string;
}) {
  const common = {
    className: 're-input',
    value: value ?? '',
    placeholder,
  };
  return (
    <div className="re-field">
      <label className="re-field-label">{label}</label>
      {textarea ? (
        <textarea rows={2} {...common} onChange={(e) => onChange(e.target.value)} />
      ) : (
        <input
          {...common}
          type={type}
          onChange={(e) =>
            onChange(type === 'number'
              ? (e.target.value === '' ? undefined : Number(e.target.value))
              : e.target.value)
          }
        />
      )}
    </div>
  );
}
