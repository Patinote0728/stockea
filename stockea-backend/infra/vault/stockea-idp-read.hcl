# Política de mínimo privilegio del Backend: lectura de credenciales del IDP y SMTP (KV v2).
path "secret/data/stockea/idp" {
  capabilities = ["read"]
}

path "secret/data/stockea/mail" {
  capabilities = ["read"]
}
