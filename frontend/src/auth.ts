import Keycloak from 'keycloak-js'

const oidcEnabled = import.meta.env.VITE_AUTH_MODE === 'oidc'
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'xiaohezi',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'xiaohezi-web'
})

export async function initializeAuth() {
  if (!oidcEnabled) return Boolean(sessionStorage.getItem('xhz_token'))
  return keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
}
export async function beginLogin() {
  if (!oidcEnabled) return false
  await keycloak.login({ redirectUri: window.location.origin })
  return true
}
export async function accessToken() {
  if (!oidcEnabled) return sessionStorage.getItem('xhz_token')
  if (!keycloak.authenticated) return null
  await keycloak.updateToken(30)
  return keycloak.token ?? null
}
export function isOidcMode() { return oidcEnabled }
export async function endSession() {
  if (oidcEnabled) await keycloak.logout({ redirectUri: window.location.origin })
  else sessionStorage.removeItem('xhz_token')
}
