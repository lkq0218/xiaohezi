<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { confirmAction, createConversation, hasToken, login, logout, sendMessage, type AgentReply } from './api'
import { beginLogin, endSession, initializeAuth, isOidcMode } from './auth'

type ViewMessage = { id: string; role: 'user' | 'assistant'; content: string; reply?: AgentReply }

const loggedIn = ref(false)
const authReady = ref(false)
const oidcMode = isOidcMode()
const username = ref('demo')
const password = ref('demo123')
const loginError = ref('')
const conversationId = ref('')
const input = ref('')
const busy = ref(false)
const error = ref('')
const messageList = ref<HTMLElement>()
const messages = ref<ViewMessage[]>([{
  id: 'welcome',
  role: 'assistant',
  content: '你好，我是小赫兹。查电费、模拟缴费、用电知识、停电报修，我都可以帮你。'
}])
const shortcuts = ['为什么这个月电费这么高', '查一下这个月电费', '帮我交电费', '阶梯电价怎么算', '我家停电了', '转人工']

async function doLogin() {
  loginError.value = ''
  try {
    if (await beginLogin()) return
    await login(username.value, password.value)
    loggedIn.value = true
    await ensureConversation()
  } catch (e) {
    loginError.value = e instanceof Error ? e.message : '登录失败'
  }
}

async function ensureConversation() {
  if (!conversationId.value) conversationId.value = (await createConversation()).id
}

async function submit(value = input.value) {
  const text = value.trim()
  if (!text || busy.value) return
  error.value = ''
  input.value = ''
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })
  busy.value = true
  await scrollBottom()
  try {
    await ensureConversation()
    const reply = await sendMessage(conversationId.value, text)
    messages.value.push({ id: reply.messageId, role: 'assistant', content: reply.content, reply })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '服务暂时不可用'
  } finally {
    busy.value = false
    await scrollBottom()
  }
}

async function confirm(token: string) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const reply = await confirmAction(token)
    messages.value.push({ id: reply.messageId, role: 'assistant', content: reply.content, reply })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '确认失败'
  } finally {
    busy.value = false
    await scrollBottom()
  }
}

async function scrollBottom() {
  await nextTick()
  messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' })
}

async function doLogout() {
  await endSession()
  logout()
  loggedIn.value = false
  conversationId.value = ''
}

onMounted(async () => {
  try {
    loggedIn.value = await initializeAuth()
  } catch (e) {
    loginError.value = e instanceof Error ? e.message : '身份服务暂时不可用'
  } finally {
    authReady.value = true
  }
  if (loggedIn.value) {
    try { await ensureConversation() } catch { doLogout() }
  }
})
</script>

<template>
  <main v-if="!authReady" class="login-page"><section class="login-panel">正在检查登录状态…</section></main>
  <main v-else-if="!loggedIn" class="login-page">
    <section class="brand-panel">
      <div class="brand-mark">HZ</div>
      <p class="eyebrow">POWER SERVICE · 2026</p>
      <h1>让每一次用电服务，<br><em>简单一点。</em></h1>
      <p class="brand-copy">模拟小赫兹智能体，从理解你的需求，到完成一项具体业务。</p>
      <div class="power-line"><span></span><i></i><span></span></div>
    </section>
    <section class="login-panel">
      <form class="login-card" @submit.prevent="doLogin">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>进入智能服务台</h2>
        <p class="muted">使用演示账号体验完整业务流程</p>
        <label v-if="!oidcMode">账号<input v-model="username" autocomplete="username"></label>
        <label v-if="!oidcMode">密码<input v-model="password" type="password" autocomplete="current-password"></label>
        <p v-if="loginError" class="error">{{ loginError }}</p>
        <button class="primary" type="submit">{{ oidcMode ? '使用统一身份登录' : '登录并开始' }}</button>
        <p class="demo-hint">{{ oidcMode ? '由 Keycloak / OIDC 安全登录' : '演示账号 demo · 密码 demo123' }}</p>
      </form>
    </section>
  </main>

  <main v-else class="app-shell">
    <aside class="sidebar">
      <div class="logo-row"><div class="brand-mark small">HZ</div><strong>小赫兹</strong></div>
      <button class="new-chat" @click="conversationId = ''; messages = messages.slice(0, 1)">＋ 新对话</button>
      <nav>
        <p class="nav-title">常用服务</p>
        <button v-for="item in shortcuts" :key="item" @click="submit(item)">{{ item }}</button>
      </nav>
      <div class="sidebar-bottom">
        <span class="status-dot"></span> 模拟服务正常
        <button @click="doLogout">退出</button>
      </div>
    </aside>

    <section class="chat">
      <header>
        <div><p class="eyebrow">SMART POWER ASSISTANT</p><h2>智能电力服务</h2></div>
        <span class="demo-badge">演示环境 · 不产生真实扣款</span>
      </header>
      <div ref="messageList" class="messages" aria-live="polite">
        <div v-for="msg in messages" :key="msg.id" :class="['message-row', msg.role]">
          <div v-if="msg.role === 'assistant'" class="avatar">HZ</div>
          <article class="bubble">
            <p v-if="msg.reply?.modelTrace" class="model-trace">
              {{ msg.reply.modelTrace.intentSource }}
              <span v-if="msg.reply.modelTrace.intentModel">· {{ msg.reply.modelTrace.intentModel }}</span>
              <span v-if="msg.reply.modelTrace.responseModel">→ {{ msg.reply.modelTrace.responseModel }}</span>
              <span v-if="msg.reply.modelTrace.degraded">· 已降级</span>
            </p>
            <p>{{ msg.content }}</p>
            <section v-if="msg.reply?.card" class="biz-card">
              <div class="card-head"><span>{{ msg.reply.card.title }}</span><small>{{ msg.reply.intent }}</small></div>
              <dl v-if="msg.reply.card.fields">
                <template v-for="(value, key) in msg.reply.card.fields" :key="key">
                  <dt>{{ key }}</dt><dd>{{ value }}</dd>
                </template>
              </dl>
              <div v-if="msg.reply.card.options" class="option-list">
                <button v-for="option in msg.reply.card.options" :key="option" @click="submit(option)">{{ option }}</button>
              </div>
              <button v-if="msg.reply.card.actionToken" class="confirm-btn" :disabled="busy" @click="confirm(msg.reply.card.actionToken)">
                确认办理
              </button>
            </section>
            <details v-if="msg.reply?.evidence?.length" class="evidence-panel">
              <summary>查看分析依据</summary>
              <dl>
                <template v-for="item in msg.reply.evidence" :key="item.id">
                  <dt>{{ item.label }}</dt><dd>{{ item.value }}</dd>
                </template>
              </dl>
            </details>
            <div v-if="msg.reply?.limitations?.length" class="limitations">
              <strong>分析边界</strong>
              <ul><li v-for="item in msg.reply.limitations" :key="item">{{ item }}</li></ul>
            </div>
            <p v-if="msg.reply?.citations?.length" class="citation">来源：{{ msg.reply.citations[0].title }}</p>
          </article>
        </div>
        <div v-if="busy" class="message-row assistant"><div class="avatar">HZ</div><div class="typing"><i></i><i></i><i></i></div></div>
      </div>
      <footer>
        <div class="quick-row">
          <button v-for="item in shortcuts.slice(0, 4)" :key="item" @click="submit(item)">{{ item }}</button>
        </div>
        <form class="composer" @submit.prevent="submit()">
          <textarea v-model="input" rows="1" maxlength="1000" placeholder="说说你想办理什么…" @keydown.enter.exact.prevent="submit()"></textarea>
          <button :disabled="busy || !input.trim()" aria-label="发送">↑</button>
        </form>
        <p v-if="error" class="error center">{{ error }}</p>
        <p class="disclaimer">小赫兹可能会出错；业务结果请以结构化回执为准。</p>
      </footer>
    </section>
  </main>
</template>
