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

/** 帖子分类(与后端 PostCategory 枚举对应) */
export const POST_CATEGORIES = [
  { code: 'study', name: '学习交流', icon: 'book' },
  { code: 'life', name: '校园生活', icon: 'users' },
  { code: 'confession', name: '表白墙', icon: 'heart' },
  { code: 'lostfound', name: '失物招领', icon: 'search' },
  { code: 'job', name: '求职招聘', icon: 'send' },
  { code: 'food', name: '美食探店', icon: 'star' },
  { code: 'sports', name: '运动健身', icon: 'message' },
  { code: 'other', name: '其他', icon: 'inbox' },
] as const;

export const QUICK_ACTIONS = [
  { key: 'like', label: '喜欢', icon: 'heart' }, { key: 'letter', label: '来信', icon: 'send' }, { key: 'card', label: '我的卡片', icon: 'star' },
  { key: 'matches', label: '互相喜欢', icon: 'sparkles' }, { key: 'liked-me', label: '谁喜欢我', icon: 'handshake' },
] as const;
