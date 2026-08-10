export const BOTTOM_NAV_ITEMS = [
  { key: 'home', label: '社区', icon: 'home', path: '/', isCenter: false },
  { key: 'bridge', label: '相识', icon: 'heart', path: '/bridge', isCenter: false },
  { key: 'create', label: '发布', icon: 'pen', path: '', isCenter: true },
  { key: 'messages', label: '消息', icon: 'message', path: '/messages', isCenter: false },
  { key: 'mine', label: '我的', icon: 'user', path: '/mine', isCenter: false },
] as const;

export const SQUARE_TABS = [
  { key: 'hot', label: '热门' }, { key: 'latest', label: '最新' }, { key: 'following', label: '关注' },
] as const;

export const MATE_CATEGORIES = [
  { code: 'travel', name: '旅游搭子', icon: 'location' }, { code: 'carpool', name: '拼车搭子', icon: 'users' },
  { code: 'fitness', name: '健身搭子', icon: 'heart' }, { code: 'study', name: '学习搭子', icon: 'book' },
  { code: 'exam', name: '备考搭子', icon: 'pen' }, { code: 'sports', name: '运动搭子', icon: 'users' },
  { code: 'gaming', name: '游戏搭子', icon: 'message' }, { code: 'entertainment', name: '娱乐搭子', icon: 'calendar' },
  { code: 'other', name: '其他', icon: 'inbox' },
] as const;

export const QUICK_ACTIONS = [
  { key: 'like', label: '喜欢', icon: 'heart' }, { key: 'letter', label: '来信', icon: 'send' },
] as const;
