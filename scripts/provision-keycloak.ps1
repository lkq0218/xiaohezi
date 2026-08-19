param([string]$ComposeFile = "$PSScriptRoot/../infra/docker-compose.yml")
$ErrorActionPreference = 'Stop'
$script = @'
set -eu
KCADM=/opt/keycloak/bin/kcadm.sh
$KCADM config credentials --server http://localhost:8080 --realm master \
  --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"
create_user() {
  user="$1"; password="$2"; roles="$3"
  $KCADM create users -r xiaohezi -s username="$user" -s enabled=true 2>/dev/null || true
  $KCADM set-password -r xiaohezi --username "$user" --new-password "$password"
  $KCADM add-roles -r xiaohezi --uusername "$user" --rolename USER
  if [ "$roles" = admin ]; then
    $KCADM add-roles -r xiaohezi --uusername "$user" --rolename ADMIN
  fi
}
create_user demo-user "$XHZ_DEMO_USER_PASSWORD" user
create_user demo-admin "$XHZ_DEMO_ADMIN_PASSWORD" admin
'@
docker compose -f $ComposeFile exec -T keycloak sh -c $script
if ($LASTEXITCODE -ne 0) { throw 'Keycloak provisioning failed' }
Write-Output 'Created demo-user (USER) and demo-admin (USER, ADMIN) without storing passwords in Git.'
