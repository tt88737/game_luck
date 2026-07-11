type MemberIdQuery = {
  memberId?: string | number;
  memberNo?: string;
};

export const normalizeMemberIdQuery = <T extends MemberIdQuery>(query: T): T => {
  const normalized = { ...query };
  const memberKeyword = normalized.memberId?.toString().trim();

  if (memberKeyword && !/^\d+$/.test(memberKeyword)) {
    normalized.memberNo = memberKeyword;
    normalized.memberId = undefined;
  }

  return normalized;
};
