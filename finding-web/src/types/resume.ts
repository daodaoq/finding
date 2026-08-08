/** 情感简历 —— 与后端 user_resume 对应 */
export interface UserResume {
  userId: number;
  // 板块1 基础信息
  gender?: number;
  age?: number;
  birthday?: string;
  constellation?: string;
  heightCm?: number;
  weightKg?: number;
  campus?: string;
  majorGrade?: string;
  hometown?: string;
  career?: string;
  dailyRoutine?: string;
  relationshipStatus?: string;
  coreBottomLine?: string;
  // 板块2 自我画像
  mbti?: string;
  personalityTraits?: string;
  inLoveLook?: string;
  flaws?: string;
  worldview?: string;
  personalTags?: string;
  // 板块3 恋爱复盘
  relationshipCount?: string;
  breakupReason?: string;
  loveShortcoming?: string;
  loveInsight?: string;
  loveGrowth?: string;
  // 板块4 恋爱相处模式
  dailyCompany?: string;
  fightMode?: string;
  loveExpression?: string;
  oppositeBoundary?: string;
  // 板块5 个人生活与规划
  dailyStatus?: string;
  lifeHabits?: string;
  shortTermPlan?: string;
  longTermPlan?: string;
  hobbies?: string;
  marriagePlan?: string;
  // 板块6 理想的另一半
  hardConditions?: string;
  softExpectations?: string;
  // 板块7 加分项
  bonusPoints?: string;
  // 板块8 走心宣言
  loveExpectation?: string;
  loveAttitude?: string;
  // 板块9 生活相册
  photoAlbum?: string[];
}

/** 查看他人情感简历的返回(后端 ResumeViewVO) */
export interface ResumeView {
  infoShared: boolean;
  shareStatus: number; // 0=无 1=待处理 2=已互换 3=已拒绝
  resume: UserResume | null;
}

/** 聊天框「互换信息」按钮状态(后端 InfoShareStatusVO) */
export interface InfoShareStatus {
  status: 'none' | 'pendingSent' | 'pendingReceived' | 'approved' | 'rejected';
  shareId: number | null;
  otherUserId: number;
  otherNickname?: string;
  otherAvatar?: string;
}
