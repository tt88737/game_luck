import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { GameBetOrderForm, GameBetOrderQuery, GameBetOrderVO } from './types';

export function listGameBet(query: GameBetOrderQuery): AxiosPromise<GameBetOrderVO[]> {
  return request({
    url: '/game/bet/list',
    method: 'get',
    params: query
  });
}

export function getGameBet(id: string | number): AxiosPromise<GameBetOrderVO> {
  return request({
    url: '/game/bet/' + id,
    method: 'get'
  });
}

export function addGameBet(data: GameBetOrderForm) {
  return request({
    url: '/game/bet',
    method: 'post',
    data
  });
}

export function placeGameBet(id: string | number): AxiosPromise<GameBetOrderVO> {
  return request({
    url: '/game/bet/' + id + '/place',
    method: 'post'
  });
}

export function settleGameBet(id: string | number): AxiosPromise<GameBetOrderVO> {
  return request({
    url: '/game/bet/' + id + '/settle',
    method: 'post'
  });
}

export function cancelGameBet(id: string | number): AxiosPromise<GameBetOrderVO> {
  return request({
    url: '/game/bet/' + id + '/cancel',
    method: 'post'
  });
}
