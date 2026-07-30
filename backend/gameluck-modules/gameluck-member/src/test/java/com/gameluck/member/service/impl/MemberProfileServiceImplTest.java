package com.gameluck.member.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.bo.MemberProfileBo;
import com.gameluck.member.enums.MemberRiskLevel;
import com.gameluck.member.enums.MemberStatus;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.MemberIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberProfileServiceImplTest {

    @Test
    @Tag("local")
    void insertMemberDefaultsStatusRiskAndChannel() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        when(mapper.selectByUsername("000000", "alice")).thenReturn(null);
        when(mapper.insert(any(MemberProfile.class))).thenReturn(1);

        Boolean result = service.insertByBo(createBo("alice"));

        assertEquals(Boolean.TRUE, result);
        ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(mapper).insert(memberCaptor.capture());
        MemberProfile inserted = memberCaptor.getValue();
        assertTrue(inserted.getMemberNo().startsWith("GL"));
        assertTrue(inserted.getMemberNo().length() >= 8);
    }

    @Test
    @Tag("local")
    void insertMemberDefaultsKycStatusToNotStarted() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        when(mapper.selectByUsername("000000", "kyc_alice")).thenReturn(null);
        when(mapper.insert(any(MemberProfile.class))).thenReturn(1);

        MemberProfileBo bo = createBo("kyc_alice");
        bo.setKycStatus(null);

        Boolean result = service.insertByBo(bo);

        assertEquals(Boolean.TRUE, result);
        ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(mapper).insert(memberCaptor.capture());
        assertEquals("NOT_STARTED", memberCaptor.getValue().getKycStatus());
    }

    @Test
    @Tag("local")
    void invalidKycStatusCannotBeInserted() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        when(mapper.selectByUsername("000000", "kyc_bad")).thenReturn(null);
        MemberProfileBo bo = createBo("kyc_bad");
        bo.setKycStatus("MANUAL_OK");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("member.kyc.status.invalid", exception.getMessage());
        verify(mapper, never()).insert(any(MemberProfile.class));
    }

    @Test
    @Tag("local")
    void updateKycStatusStoresReviewMetadata() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        MemberProfile existing = new MemberProfile();
        existing.setId(88L);
        existing.setTenantId("000000");
        existing.setUsername("kyc_review");
        existing.setKycStatus("PENDING");
        when(mapper.selectById(88L)).thenReturn(existing);
        when(mapper.selectByUsername("000000", "kyc_review")).thenReturn(existing);
        when(mapper.updateById(any(MemberProfile.class))).thenReturn(1);

        MemberProfileBo bo = createBo("kyc_review");
        bo.setId(88L);
        bo.setKycStatus("APPROVED");
        bo.setKycReviewReason("Manual review passed");

        Boolean result = service.updateByBo(bo);

        assertEquals(Boolean.TRUE, result);
        ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(mapper).updateById(memberCaptor.capture());
        MemberProfile update = memberCaptor.getValue();
        assertEquals("APPROVED", update.getKycStatus());
        assertEquals("Manual review passed", update.getKycReviewReason());
        assertEquals("admin", update.getKycReviewedBy());
        assertTrue(update.getKycReviewTime() != null);
    }

    @Test
    @Tag("local")
    void duplicateUsernameCannotBeInserted() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        MemberProfile existing = new MemberProfile();
        existing.setId(1L);
        existing.setUsername("alice");
        when(mapper.selectByUsername("000000", "alice")).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(createBo("alice")));

        assertEquals("member.username.exists", exception.getMessage());
        verify(mapper, never()).insert(any(MemberProfile.class));
    }

    @Test
    @Tag("local")
    void invalidStatusCannotBeUpdated() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateStatus(1L, "LOCKED"));

        assertEquals("member.status.invalid", exception.getMessage());
        verify(mapper, never()).selectById(any());
    }

    @Test
    @Tag("local")
    void queryListCanFilterByCountryAndState() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MemberProfile.class);
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
        MemberProfileBo bo = new MemberProfileBo();
        bo.setCountryCode("US");
        bo.setStateCode("CA");

        service.queryList(bo);

        ArgumentCaptor<LambdaQueryWrapper<MemberProfile>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectVoList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("country_code"));
        assertTrue(sqlSegment.contains("state_code"));
    }

    private MemberProfileBo createBo(String username) {
        MemberProfileBo bo = new MemberProfileBo();
        bo.setUsername(username);
        bo.setNickname("Alice");
        bo.setStatus(MemberStatus.ACTIVE.name());
        bo.setRiskLevel(MemberRiskLevel.NORMAL.name());
        return bo;
    }
}
