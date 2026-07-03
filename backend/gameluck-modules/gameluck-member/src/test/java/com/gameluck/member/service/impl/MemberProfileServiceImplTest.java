package com.gameluck.member.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.domain.bo.MemberProfileBo;
import com.gameluck.member.enums.MemberRiskLevel;
import com.gameluck.member.enums.MemberStatus;
import com.gameluck.member.mapper.MemberProfileMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper);
        when(mapper.selectByUsername("000000", "alice")).thenReturn(null);
        when(mapper.insert(any(MemberProfile.class))).thenReturn(1);

        Boolean result = service.insertByBo(createBo("alice"));

        assertEquals(Boolean.TRUE, result);
        verify(mapper).insert(any(MemberProfile.class));
    }

    @Test
    @Tag("local")
    void duplicateUsernameCannotBeInserted() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper);
        MemberProfile existing = new MemberProfile();
        existing.setId(1L);
        existing.setUsername("alice");
        when(mapper.selectByUsername("000000", "alice")).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(createBo("alice")));

        assertEquals("member username already exists", exception.getMessage());
        verify(mapper, never()).insert(any(MemberProfile.class));
    }

    @Test
    @Tag("local")
    void invalidStatusCannotBeUpdated() {
        MemberProfileMapper mapper = mock(MemberProfileMapper.class);
        MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateStatus(1L, "LOCKED"));

        assertEquals("invalid member status", exception.getMessage());
        verify(mapper, never()).selectById(any());
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
