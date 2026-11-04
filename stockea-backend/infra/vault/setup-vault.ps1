<#
.SYNOPSIS
  Prepara el Vault local (modo dev, en memoria): AppRole + política + secretos del IDP y SMTP.

.DESCRIPTION
  Idempotente: se puede ejecutar de nuevo (por ejemplo tras reiniciar el contenedor, que pierde todo).
    1. Habilita auth/approle.
    2. Escribe la política stockea-idp-read (infra/vault/stockea-idp-read.hcl).
    3. Crea el rol stockea-backend con esa política.
    4. Siembra secret/stockea/idp (KV v2) si no existe, o si se pasan los secretos por variables de entorno.
    5. Si se pasan credenciales SMTP, las siembra en secret/stockea/mail (KV v2).
    6. Regenera .vault/role-id y .vault/secret-id (ignorados por git).
  Nunca imprime RoleId, SecretId, tokens ni valores de secretos.

  Variables de entorno opcionales para el secreto del IDP:
    STOCKEA_IDP_VERIFIER_CLIENT_SECRET  (cliente stockea-pwd-verifier)
    STOCKEA_IDP_ADMIN_CLIENT_SECRET     (cliente stockea-admin-api)
    STOCKEA_MAIL_SMTP_USERNAME
    STOCKEA_MAIL_SMTP_PASSWORD
  Si no existen y el secreto no está sembrado, se escribe un valor PLACEHOLDER (suficiente para el spike).

.EXAMPLE
  .\infra\vault\setup-vault.ps1
#>
$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $root

$service = 'vault'
$role = 'stockea-backend'
$policy = 'stockea-idp-read'
$policyFile = Join-Path $PSScriptRoot 'stockea-idp-read.hcl'
$vaultDir = Join-Path $root '.vault'

# El token raíz de dev ya vive dentro del contenedor (VAULT_DEV_ROOT_TOKEN_ID); no se lee ni se imprime aquí.
$prefix = 'export VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=$VAULT_DEV_ROOT_TOKEN_ID; '

function Invoke-Vault([string] $Command, [string] $InputText = $null) {
    $dockerArgs = @('compose', 'exec', '-T', $service, 'sh', '-c', ($prefix + $Command))
    # stderr nativo no debe ser un error fatal; el resultado se decide por el codigo de salida.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        if ($null -ne $InputText) { $out = $InputText | & docker @dockerArgs 2>&1 } else { $out = & docker @dockerArgs 2>&1 }
    } finally {
        $ErrorActionPreference = $previous
    }
    if ($LASTEXITCODE -ne 0) { return [pscustomobject]@{ Ok = $false; Output = ($out -join "`n") } }
    return [pscustomobject]@{ Ok = $true; Output = ($out -join "`n") }
}

function Assert-Vault($Result, [string] $Step) {
    if (-not $Result.Ok) { throw "Fallo el paso '$Step': $($Result.Output)" }
}

Write-Host '[1/6] Comprobando Vault...'
$status = Invoke-Vault 'vault status -format=json'
Assert-Vault $status 'vault status (esta levantado? docker compose up -d vault)'

Write-Host '[2/6] Habilitando auth/approle...'
$auths = Invoke-Vault 'vault auth list -format=json'
Assert-Vault $auths 'auth list'
if ($auths.Output -notmatch '"approle/"') {
    Assert-Vault (Invoke-Vault 'vault auth enable approle') 'auth enable approle'
} else {
    Write-Host '      ya estaba habilitado.'
}

Write-Host "[3/6] Escribiendo la politica $policy..."
Assert-Vault (Invoke-Vault "vault policy write $policy -" (Get-Content $policyFile -Raw -Encoding UTF8)) 'policy write'

Write-Host "[4/6] Creando el rol $role..."
Assert-Vault (Invoke-Vault ("vault write auth/approle/role/$role token_policies=$policy " +
        'token_ttl=1h token_max_ttl=4h secret_id_ttl=0 secret_id_num_uses=0 token_num_uses=0')) 'role write'

Write-Host '[5/6] Sembrando secret/stockea/idp...'
$verifier = $env:STOCKEA_IDP_VERIFIER_CLIENT_SECRET
$admin = $env:STOCKEA_IDP_ADMIN_CLIENT_SECRET
$exists = (Invoke-Vault 'vault kv get -mount=secret -format=json stockea/idp').Ok
if ($verifier -or $admin -or -not $exists) {
    $placeholder = 'CAMBIAR-POR-EL-SECRETO-REAL'
    $json = @{
        'pwd-verifier-client-secret' = $(if ($verifier) { $verifier } else { $placeholder })
        'admin-api-client-secret'    = $(if ($admin) { $admin } else { $placeholder })
    } | ConvertTo-Json -Compress
    Assert-Vault (Invoke-Vault 'vault kv put -mount=secret stockea/idp -' $json) 'kv put'
    if (-not ($verifier -and $admin)) {
        Write-Warning 'El secreto del IDP contiene valores PLACEHOLDER; sustituyelos por los secretos reales de Keycloak.'
    }
} else {
    Write-Host '      ya existe; se conserva (usa las variables de entorno para sobrescribirlo).'
}

$mailUsername = $env:STOCKEA_MAIL_SMTP_USERNAME
$mailPassword = $env:STOCKEA_MAIL_SMTP_PASSWORD
if (($mailUsername -and -not $mailPassword) -or (-not $mailUsername -and $mailPassword)) {
    throw 'Para sembrar credenciales SMTP debes proporcionar usuario y contraseña.'
}
if ($mailUsername -and $mailPassword) {
    $mailJson = @{
        'smtp-username' = $mailUsername
        'smtp-password' = $mailPassword
    } | ConvertTo-Json -Compress
    Assert-Vault (Invoke-Vault 'vault kv put -mount=secret stockea/mail -' $mailJson) 'kv put mail'
}

Write-Host '[6/6] Regenerando .vault/role-id y .vault/secret-id...'
New-Item -ItemType Directory -Force -Path $vaultDir | Out-Null
$roleId = Invoke-Vault "vault read -field=role_id auth/approle/role/$role/role-id"
Assert-Vault $roleId 'read role-id'
$secretId = Invoke-Vault "vault write -f -field=secret_id auth/approle/role/$role/secret-id"
Assert-Vault $secretId 'write secret-id'
[System.IO.File]::WriteAllText((Join-Path $vaultDir 'role-id'), $roleId.Output.Trim())
[System.IO.File]::WriteAllText((Join-Path $vaultDir 'secret-id'), $secretId.Output.Trim())

Write-Host ''
Write-Host 'Listo. Para ejecutar la prueba del spike (sin imprimir el RoleId):'
Write-Host '  $env:STOCKEA_VAULT_ROLE_ID = (Get-Content ''.vault\role-id'' -Raw).Trim()'
Write-Host '  .\mvnw.cmd -q ''-Dtest=Spk007VaultAppRoleSpikeTest'' test'
Write-Host '  Remove-Item Env:STOCKEA_VAULT_ROLE_ID'
