# Development Secure Boot key

**These are DEVELOPMENT keys. They are NOT secret and MUST NOT be used in any
production or shipped image.** The private key `db.key` is committed in the
clear on purpose, so the whole team builds a consistent, self-signed UEFI
Secure Boot chain without per-developer key juggling. Anyone with this repo can
sign binaries that this chain trusts. Regenerate a fresh, properly-protected
key before any real deployment.

## Contents

| file       | purpose                                                        |
|------------|----------------------------------------------------------------|
| `db.key`   | RSA-4096 private key (PEM) used to `sbsign` shim/grub/xen UKI   |
| `db.crt`   | self-signed X.509 cert (PEM) — signing cert + enrolled as db    |
| `db.cer`   | same cert in DER — the file a user enrolls into firmware Setup  |
| `guid.txt` | stable owner GUID used when enrolling the cert into PK/KEK/db   |

## How it is used

- `sb-sign.bbclass` (yocto) signs `shimx64.efi`, `grubx64.efi`, and the Xen
  `*.unified.efi` with `db.key`/`db.crt`.
- shim is built with `VENDOR_CERT = db.crt`, so it trusts this key for the
  downstream chain (grub -> UKI) independent of what is in firmware `db`.
- Trust chain: `firmware db (db.cer) -> shim -> grub -> chainload signed UKI`.

## Regenerating

```sh
openssl req -x509 -newkey rsa:4096 -keyout db.key -out db.crt -days 3650 \
  -nodes -sha256 -subj "/CN=TrenchBoot Devel Secure Boot key (DO NOT USE IN PRODUCTION)/"
openssl x509 -in db.crt -outform DER -out db.cer
python3 -c "import uuid; print(uuid.uuid4())" > guid.txt
```
