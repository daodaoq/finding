/** 管理端通用分页结构(后端 PageVO) */
export interface AdminPage<T> {
  records: T[];
  total: number;
  page?: number;
  size?: number;
}

/** 用户列表行(管理端用户管理 / 聊天审查) */
export interface AdminUserRecord {
  id: number;
  nickname: string;
  phone: string;
  school?: string;
  status: number;
  realNameVerified: number;
  createdAt: string;
}

/** 用户详情(含完整字段,管理端可见) */
export interface AdminUserDetail extends AdminUserRecord {
  avatar?: string;
  gender?: number;
  birthday?: string;
  email?: string;
  signature?: string;
  city?: string;
  role?: string;
}

/** 新建/编辑用户表单(payload) */
export interface AdminUserForm {
  nickname: string;
  phone: string;
  /** 编辑时留空表示不修改密码 */
  password?: string;
  avatar?: string;
  school?: string;
  gender: number;
  birthday?: string;
  email?: string;
  role: string;
  signature?: string;
  city?: string;
  status: number;
}

/** 群聊列表行(聊天审查) */
export interface AdminGroupRecord {
  id: number;
  name: string;
  memberCount?: number;
  createdAt?: string;
}

/** 情感简历字段值(文本/数字/相册数组,均可空) */
export type ResumeFieldValue = string | number | string[] | undefined;
/** 情感简历编辑表单:key 为简历字段名,值为混合类型 */
export type ResumeForm = Record<string, ResumeFieldValue>;
