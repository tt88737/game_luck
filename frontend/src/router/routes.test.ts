import { describe, expect, it } from 'vitest'
import { router } from './index'

describe('C-side production routes', () => {
  it('redirects root and legacy app entry to lobby', async () => {
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')

    await router.push('/app')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
  })

  it('redirects legacy app routes to canonical production routes', async () => {
    const redirects: Array<[string, string]> = [
      ['/app/store', '/store'],
      ['/app/activity', '/promo'],
      ['/app/wallet', '/me/wallet'],
      ['/app/redemption', '/me/redeem'],
      ['/app/kyc', '/me/kyc'],
      ['/app/inbox', '/inbox'],
      ['/app/slots/lucky_slots', '/lobby/slots/lucky_slots'],
    ]

    for (const [from, to] of redirects) {
      await router.push(from)
      await router.isReady()
      expect(router.currentRoute.value.path).toBe(to)
    }
  })

  it('opens auth modal through canonical lobby query redirects', async () => {
    await router.push('/app/register')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
    expect(router.currentRoute.value.query.auth).toBe('register')

    await router.push('/app/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
    expect(router.currentRoute.value.query.auth).toBe('login')
  })
})
