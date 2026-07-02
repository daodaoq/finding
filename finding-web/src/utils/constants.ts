export const BOTTOM_NAV_ITEMS = [
  { key: 'home', label: '首页', icon: '🏠', path: '/', isCenter: false },
  { key: 'bridge', label: '鹊桥', icon: '💕', path: '/bridge', isCenter: false },
  { key: 'create', label: '', icon: '＋', path: '', isCenter: true },
  { key: 'messages', label: '消息', icon: '💬', path: '/messages', isCenter: false },
  { key: 'mine', label: '我的', icon: '👤', path: '/mine', isCenter: false },
] as const;

export const SQUARE_TABS = [
  { key: 'hot', label: '热门' },
  { key: 'latest', label: '最新' },
  { key: 'following', label: '关注' },
] as const;

export const MATE_CATEGORIES = [
  { code: 'travel', name: '旅游搭子', icon: '✈️' },
  { code: 'carpool', name: '拼车搭子', icon: '🚗' },
  { code: 'fitness', name: '健身搭子', icon: '💪' },
  { code: 'study', name: '学习搭子', icon: '📚' },
  { code: 'exam', name: '备考搭子', icon: '📝' },
  { code: 'sports', name: '运动搭子', icon: '⚽' },
  { code: 'gaming', name: '游戏搭子', icon: '🎮' },
  { code: 'entertainment', name: '娱乐搭子', icon: '🎬' },
  { code: 'other', name: '其他', icon: '📌' },
] as const;

export const QUICK_ACTIONS = [
  { key: 'like', label: '喜欢', icon: '❤️' },
  { key: 'letter', label: '情书', icon: '💌' },
] as const;
