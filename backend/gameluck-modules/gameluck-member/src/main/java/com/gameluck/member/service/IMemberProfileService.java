package com.gameluck.member.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.member.domain.bo.MemberProfileBo;
import com.gameluck.member.domain.vo.MemberProfileVo;

import java.util.Collection;
import java.util.List;

/**
 * Member profile service.
 */
public interface IMemberProfileService {

    TableDataInfo<MemberProfileVo> queryPageList(MemberProfileBo bo, PageQuery pageQuery);

    MemberProfileVo queryById(Long id);

    List<MemberProfileVo> queryList(MemberProfileBo bo);

    Boolean insertByBo(MemberProfileBo bo);

    Boolean updateByBo(MemberProfileBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    MemberProfileVo updateStatus(Long id, String status);
}
