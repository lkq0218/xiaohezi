import { beforeEach, describe, expect, it, vi } from 'vitest'
import { login } from './api'

describe('API client', () => {
  const storage = new Map<string, string>()

  beforeEach(() => {
    storage.clear()
    vi.stubGlobal('sessionStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key)
    })
  })

  it('stores token after successful login', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: { accessToken: 'token', displayName: '演示用户' }
      })
    }))

    await login('demo', 'demo123')

    expect(storage.get('xhz_token')).toBe('token')
  })

  it('does not store token when server rejects login', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ code: 'INVALID_CREDENTIALS', message: '账号或密码错误' })
    }))

    await expect(login('bad', 'bad')).rejects.toThrow('账号或密码错误')
    expect(storage.has('xhz_token')).toBe(false)
  })
})
