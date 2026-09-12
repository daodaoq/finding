import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Card, Col, Row, Statistic, Tooltip, theme } from 'antd';
import {
  UserOutlined, FileTextOutlined, TeamOutlined, CheckCircleOutlined,
  UserAddOutlined, WarningOutlined, UsergroupAddOutlined,
} from '@ant-design/icons';
import request from '../api/request';
import './Dashboard.css';

interface DashboardStats {
  totalUsers: number;
  todayPosts: number;
  todayMates: number;
  pendingVerifications: number;
  todayNewUsers: number;
  totalMates: number;
  pendingReports: number;
  groupCount: number;
}

interface TrendData {
  dates: string[];
  newUsers: number[];
  newPosts: number[];
  newMates: number[];
  activeUsers: number[];
}

interface QualityData {
  genderRatio: { male: number; female: number; maleRate: number; femaleRate: number };
  verificationRate: { approved: number; total: number; rate: number };
  retention: { active7d: number; activeRate: number; retained7d: number; retentionRate: number };
  moderationSla: {
    pendingPosts: number; pendingAppeals: number; pendingVerifications: number; pendingReports: number;
    oldestPendingPostHours: number; oldestPendingAppealHours: number; oldestPendingVerificationHours: number;
  };
}

/** 柱区高度(px)。柱高按像素算:百分比高度在 auto 高度的包含块里会退化成 auto,柱子会消失。 */
const PLOT_H = 120;

/** y 轴刻度阶梯,把最大值向上取整到台阶上,刻度即整数 */
const TICK_STEPS = [1, 1.2, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10];

function niceCeil(v: number): number {
  if (!Number.isFinite(v) || v <= 0) return 1;
  const pow = 10 ** Math.floor(Math.log10(v));
  return (TICK_STEPS.find((s) => v / pow <= s + 1e-9) ?? 10) * pow;
}

/** 令牌色是 6 位 hex,补 alpha 后缀得到浅色;非 hex 原样返回 */
const withAlpha = (color: string, alpha: string) =>
  /^#[0-9a-f]{6}$/i.test(color) ? `${color}${alpha}` : color;

const fmtTick = (v: number) => (Number.isInteger(v) ? String(v) : v.toFixed(1));

interface TrendChartProps {
  title: string;
  color: string;
  values?: number[];
  dates?: string[];
}

/** 单条趋势的柱状图:y 轴刻度 + 网格线 + 渐变柱 + 悬停提示 + 空数据态 */
function TrendChart({ title, color, values = [], dates = [] }: TrendChartProps) {
  const { token } = theme.useToken();
  const [grown, setGrown] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => setGrown(true));
    return () => cancelAnimationFrame(id);
  }, []);

  const max = values.length ? Math.max(...values) : 0;
  const top = niceCeil(max);
  const sum = values.reduce((a, b) => a + b, 0);
  const ticks = top >= 2 ? [top, top / 2, 0] : [top, 0];
  const posOf = (v: number) => `${(v / top) * 100}%`;

  return (
    <div
      className="trend-chart"
      style={{
        '--plot-h': `${PLOT_H}px`,
        '--bar-color': color,
        '--bar-color-soft': withAlpha(color, 'b3'),
        '--bar-color-line': withAlpha(color, '59'),
        '--panel-bg': withAlpha(color, '0d'),
        '--tick-color': token.colorTextTertiary,
        '--grid-color': token.colorSplit,
      } as CSSProperties}
    >
      <div className="trend-chart__head">
        <span className="trend-chart__dot" />
        <span className="trend-chart__title">{title}</span>
        <span className="trend-chart__sum">7 日合计 {sum}</span>
      </div>

      <div className="trend-chart__values">
        {values.map((v, i) => <span key={i}>{v > 0 ? v : ''}</span>)}
      </div>

      <div className="trend-chart__plot">
        <div className="trend-chart__yaxis">
          {ticks.map((t) => (
            <span key={t} className="trend-chart__tick" style={{ bottom: posOf(t) }}>{fmtTick(t)}</span>
          ))}
        </div>
        <div className="trend-chart__canvas">
          <div className="trend-chart__grid">
            {ticks.map((t) => <i key={t} className={t === 0 ? 'is-base' : ''} style={{ bottom: posOf(t) }} />)}
          </div>
          {max === 0 && <span className="trend-chart__empty">近 7 天暂无数据</span>}
          <div className="trend-chart__bars">
            {values.map((v, i) => (
              <Tooltip key={i} title={`${dates[i] ?? ''} · ${v}`}>
                <div className="trend-chart__col">
                  <div
                    className="trend-chart__bar"
                    style={{
                      height: grown ? Math.max(v > 0 ? 4 : 2, Math.round((v / top) * PLOT_H)) : 0,
                      opacity: v > 0 ? 1 : 0.3,
                      transitionDelay: `${i * 40}ms`,
                    }}
                  />
                </div>
              </Tooltip>
            ))}
          </div>
        </div>
      </div>

      <div className="trend-chart__dates">
        {dates.map((d, i) => <span key={i}>{d?.slice(5)}</span>)}
      </div>
    </div>
  );
}

export default function Dashboard() {
  const { token } = theme.useToken();
  const [stats, setStats] = useState<DashboardStats>({
    totalUsers: 0, todayPosts: 0, todayMates: 0, pendingVerifications: 0,
    todayNewUsers: 0, totalMates: 0, pendingReports: 0, groupCount: 0,
  });
  const [trend, setTrend] = useState<TrendData | null>(null);
  const [quality, setQuality] = useState<QualityData | null>(null);

  useEffect(() => {
    request.get('/admin/dashboard').then((res) => {
      if (res.data?.data) setStats(res.data.data);
    }).catch(() => {});
    request.get('/admin/dashboard/trend', { params: { days: 7 } }).then((res) => {
      if (res.data?.data) setTrend(res.data.data);
    }).catch(() => {});
    request.get('/admin/dashboard/quality').then((res) => {
      if (res.data?.data) setQuality(res.data.data);
    }).catch(() => {});
  }, []);

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>数据面板</h2>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="总用户数" value={stats.totalUsers} prefix={<UserOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="今日动态" value={stats.todayPosts} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="搭子邀约" value={stats.todayMates} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="待审核认证" value={stats.pendingVerifications} prefix={<CheckCircleOutlined />} valueStyle={{ color: token.colorPrimary }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="今日新增用户" value={stats.todayNewUsers} prefix={<UserAddOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="搭子总数" value={stats.totalMates} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="待处理投诉" value={stats.pendingReports} prefix={<WarningOutlined />} valueStyle={{ color: token.colorError }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="群聊总数" value={stats.groupCount} prefix={<UsergroupAddOutlined />} /></Card>
        </Col>
      </Row>
      <Card title="近 7 天趋势" style={{ marginTop: 16 }}>
        {trend ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} xl={12}>
              <TrendChart title="新增用户" values={trend.newUsers} dates={trend.dates} color={token.colorPrimary} />
            </Col>
            <Col xs={24} xl={12}>
              <TrendChart title="新增动态" values={trend.newPosts} dates={trend.dates} color={token.colorSuccess} />
            </Col>
            <Col xs={24} xl={12}>
              <TrendChart title="新增搭子" values={trend.newMates} dates={trend.dates} color={token.colorInfo} />
            </Col>
            <Col xs={24} xl={12}>
              <TrendChart title="活跃用户(登录)" values={trend.activeUsers} dates={trend.dates} color={token.colorWarning} />
            </Col>
          </Row>
        ) : (
          <p style={{ color: token.colorTextTertiary, textAlign: 'center', padding: 20 }}>加载中...</p>
        )}
      </Card>
      <Card title="质量与漏斗指标" style={{ marginTop: 16 }}>
        {quality ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="性别比">
                <p>男 {quality.genderRatio.male}（{quality.genderRatio.maleRate}%）</p>
                <p>女 {quality.genderRatio.female}（{quality.genderRatio.femaleRate}%）</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="认证率">
                <Statistic value={quality.verificationRate.rate} suffix="%" precision={2} />
                <p style={{ color: token.colorTextSecondary }}>已认证 {quality.verificationRate.approved} / {quality.verificationRate.total}</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="留存">
                <p>7 日活跃 {quality.retention.active7d}（{quality.retention.activeRate}%）</p>
                <p>老用户留存率 {quality.retention.retentionRate}%</p>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card size="small" title="审核时效">
                <p>待审:动态 {quality.moderationSla.pendingPosts} / 申诉 {quality.moderationSla.pendingAppeals}</p>
                <p>认证 {quality.moderationSla.pendingVerifications} / 投诉 {quality.moderationSla.pendingReports}</p>
                <p style={{ color: token.colorWarning }}>最久待审动态 {quality.moderationSla.oldestPendingPostHours}h</p>
              </Card>
            </Col>
          </Row>
        ) : (
          <p style={{ color: token.colorTextTertiary, textAlign: 'center', padding: 20 }}>加载中...</p>
        )}
      </Card>
      <Card title="快速入口" style={{ marginTop: 16 }}>
        <p>📋 <a href="/verification">实名认证审核</a> — 待审核: {stats.pendingVerifications}条</p>
        <p>📝 <a href="/posts">动态内容管理</a> — 今日新增: {stats.todayPosts}条</p>
        <p>⚠️ <a href="/reports">投诉管理</a> — 待处理: {stats.pendingReports}条</p>
        <p>🖼️ <a href="/banners">首页轮播管理</a></p>
      </Card>
    </div>
  );
}
