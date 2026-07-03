export interface MemberProfileVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  memberNo: string;
  username: string;
  nickname: string;
  status: string;
  riskLevel: string;
  registerChannel: string;
  lastLoginTime: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface MemberProfileForm {
  id?: string | number;
  username?: string;
  nickname?: string;
  status?: string;
  riskLevel?: string;
  registerChannel?: string;
  lastLoginTime?: string;
  remark?: string;
}

export interface MemberProfileQuery extends PageQuery {
  memberNo?: string;
  username?: string;
  nickname?: string;
  status?: string;
  riskLevel?: string;
  registerChannel?: string;
  beginTime?: string;
  endTime?: string;
}
