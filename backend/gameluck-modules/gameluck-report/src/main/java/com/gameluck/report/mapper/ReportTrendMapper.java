package com.gameluck.report.mapper;

import com.gameluck.report.domain.vo.ReportDailyTrendVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportTrendMapper {
    List<ReportDailyTrendVo> selectDailyMembers(@Param("tenantId") String tenantId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyDeposits(@Param("tenantId") String tenantId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyGames(@Param("tenantId") String tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyPromotions(@Param("tenantId") String tenantId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<ReportDailyTrendVo> selectDailyRedemptions(@Param("tenantId") String tenantId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}
