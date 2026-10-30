# Política de mínimo privilegio del Backend: solo lectura de las credenciales del IDP (KV v2).
path "secret/data/stockea/idp" {
  capabilities = ["read"]
}
