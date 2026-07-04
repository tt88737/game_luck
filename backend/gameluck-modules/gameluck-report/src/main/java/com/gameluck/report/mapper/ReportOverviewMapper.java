package com.gameluck.report.mapper;

import com.gameluck.report.domain.vo.ReportOverviewSummaryVo;
import org.apache.ibatis.annotations.Param;

/**
 * Report overview aggregate mapper.
 */
public interface ReportOverviewMapper {

    ReportOverviewSummaryVo selectSummary(@Param("tenantId") String tenantId);
}

