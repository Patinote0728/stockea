$ErrorActionPreference = 'Stop'

$directorioCertificados = $PSScriptRoot
$almacen = Join-Path $directorioCertificados 'backend-keystore.p12'
$certificado = Join-Path $directorioCertificados 'backend.crt'
$contrasenaLocal = 'stockea-local-dev'

$herramientaKeytool = Get-Command keytool -ErrorAction SilentlyContinue
$comandoKeytool = if ($herramientaKeytool) { $herramientaKeytool.Source }
if (-not $herramientaKeytool -and $env:JAVA_HOME) {
    $rutaJavaHome = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
    if (Test-Path -LiteralPath $rutaJavaHome) {
        $comandoKeytool = $rutaJavaHome
    }
}
if (-not $comandoKeytool) {
    $directorioJdk = Join-Path $env:ProgramFiles 'Java'
    $comandoKeytool = Get-ChildItem -LiteralPath $directorioJdk -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'bin/keytool.exe' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
}
if (-not $comandoKeytool) {
    throw 'No se encontro keytool. Instala un JDK compatible y vuelve a ejecutar este script.'
}

if (-not (Test-Path -LiteralPath $almacen)) {
    & $comandoKeytool -genkeypair -alias stockea-backend -keyalg RSA -keysize 2048 -validity 365 `
        -dname 'CN=backend' -ext 'SAN=dns:backend' -storetype PKCS12 `
        -keystore $almacen -storepass $contrasenaLocal -keypass $contrasenaLocal
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el certificado local del backend.' }
}

& $comandoKeytool -exportcert -rfc -alias stockea-backend -keystore $almacen `
    -storepass $contrasenaLocal -file $certificado
if ($LASTEXITCODE -ne 0) { throw 'No se pudo exportar el certificado local para el proxy.' }

Write-Host 'Certificado local generado. Solo se usa para el enlace cifrado de desarrollo.'
