import { accessToken } from './auth'

const TOKEN_KEY = 'xhz_token'

export type Card = {
  type: string
  title: string
  actionToken?: string
  fields?: Record<string, string>
  options?: string[]
}

export type AgentReply = {
  messageId: string
  conversationId: string
  content: string
  intent: string
  confidence: number
  card?: Card
  citations?: { title: string; chunkId: string }[]
  evidence?: { id: string; label: string; value: string; observedAt: string }[]
  limitations?: string[]
  modelTrace?: {
    intentSource?: string
    intentModel?: string
    responseSource?: string
    responseModel?: string
    promptTokens: number
    completionTokens: number
    degraded: boolean
  }
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = await accessToken()
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }
  })
  const body = await response.json()
  if (!response.ok || body.code !== 'OK') throw new Error(body.message || '请求失败')
  return body.data as T
}

export async function login(username: string, password: string) {
  const data = await request<{ accessToken: string; displayName: string }>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ username, password })
  })
  sessionStorage.setItem(TOKEN_KEY, data.accessToken)
  return data
}

export const hasToken = () => Boolean(sessionStorage.getItem(TOKEN_KEY))
export const logout = () => sessionStorage.removeItem(TOKEN_KEY)

export async function createConversation() {
  return request<{ id: string }>('/api/conversations', { method: 'POST', body: '{}' })
}

export async function sendMessage(conversationId: string, message: string) {
  return request<AgentReply>(`/api/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ message, clientRequestId: crypto.randomUUID() })
  })
}

export async function confirmAction(actionToken: string) {
  return request<AgentReply>(`/api/actions/${actionToken}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ clientRequestId: crypto.randomUUID() })
  })
}
