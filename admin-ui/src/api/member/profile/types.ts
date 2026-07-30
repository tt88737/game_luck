export interface MemberProfileVO extends BaseEntity {
  id: string | number;
  tenantId: string;
  memberNo: string;
  username: string;
  nickname: string;
  status: string;
  riskLevel: string;
  riskReason: string;
  riskSource: string;
  riskUpdatedTime: string;
  kycStatus: string;
  kycReviewReason: string;
  kycReviewedBy: string;
  kycReviewTime: string;
  registerChannel: string;
  countryCode: string;
  stateCode: string;
  ageConfirmed: boolean;
  termsAccepted: boolean;
  privacyAccepted: boolean;
  sweepstakesRulesAccepted: boolean;
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
  kycStatus?: string;
  kycReviewReason?: string;
  kycReviewedBy?: string;
  kycReviewTime?: string;
  registerChannel?: string;
  countryCode?: string;
  stateCode?: string;
  lastLoginTime?: string;
  remark?: string;
}

export interface MemberProfileQuery extends PageQuery {
  memberNo?: string;
  username?: string;
  nickname?: string;
  status?: string;
  riskLevel?: string;
  kycStatus?: string;
  registerChannel?: string;
  countryCode?: string;
  stateCode?: string;
  beginTime?: string;
  endTime?: string;
}
