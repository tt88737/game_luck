import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { MemberProfileForm, MemberProfileQuery, MemberProfileVO } from './types';

export function listMemberProfile(query: MemberProfileQuery): AxiosPromise<MemberProfileVO[]> {
  return request({
    url: '/member/profile/list',
    method: 'get',
    params: query
  });
}

export function getMemberProfile(id: string | number): AxiosPromise<MemberProfileVO> {
  return request({
    url: '/member/profile/' + id,
    method: 'get'
  });
}

export function addMemberProfile(data: MemberProfileForm) {
  return request({
    url: '/member/profile',
    method: 'post',
    data
  });
}

export function updateMemberProfile(data: MemberProfileForm) {
  return request({
    url: '/member/profile',
    method: 'put',
    data
  });
}

export function updateMemberProfileStatus(id: string | number, status: string): AxiosPromise<MemberProfileVO> {
  return request({
    url: '/member/profile/' + id + '/status/' + status,
    method: 'post'
  });
}

export function delMemberProfile(id: string | number | Array<string | number>) {
  return request({
    url: '/member/profile/' + id,
    method: 'delete'
  });
}
