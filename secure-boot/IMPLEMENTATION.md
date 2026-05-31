# Secure Boot signing — implementation status (meta-trenchboot)

Trust chain: `firmware db (user enrolls db.cer) → shim → grub → signed Xen UKI`.
One committed development key (`secure-boot/db.*`) signs shim, grub and the UKI.
Opt-in via `SECURE_BOOT_SIGN = "1"` (already set for the initramfs target in
`kas-tb-full-initramfs.yml`). When unset, everything below is a no-op.

## QEMU validation (2026-05-27) — full chain PASSES, but TWO recipe fixes are required

The chain `firmware db → shim → grub → signed Xen UKI` was verified end to end
under SB-enforcing OVMF (`qemu-test/sb/test-fullchain.sh`): firmware ran shim,
shim verified+launched grub, grub's `shim_lock` cleared the signed UKI and the
Xen EFI loader executed. (A later Xen "Unsupported relocation type" is a
Xen-loader runtime issue inside the *verified* UKI, NOT a Secure Boot failure.)

The raw yocto deploy artifacts do **not** pass as-is. Two defects, each fixed in
the test harness but still TODO in the recipes:

1. **shim recipe** — `make` leaves a 128 KB COFF symbol table in `shimx64.efi`
   (`PointerToSymbolTable != 0`, NumberOfSymbols 3741). OVMF rejects it with
   `EFI_SECURITY_VIOLATION` *before* shim runs. A correct shim build has no
   symbol table (its Makefile's `objcopy -j <sections>` shouldn't copy one).
   Fix: strip it before `sb_sign_file` — truncate the PE to the end of section
   raw data and zero the COFF symbol-table fields (`objcopy --strip-all` leaves
   ~50 trailing bytes + a non-zero symtab pointer, which is NOT clean enough).
2. **grub recipe** — the deployed grub has **no `.sbat` section**, so shim
   rejects it on its SBAT self-check (shim 16.1's baked policy needs `grub,4`/
   `grub,5`). Note shim's "Verification failed: Security Violation" dialog here
   is *shim's own* error rejecting grub, easily mistaken for the firmware's.
   Fix: pass `--sbat <csv>` to `grub-mkimage` (objcopy `--add-section .sbat`
   mangles the PE → shim then dies "Malformed section header / Unsupported").

Also: the OVMF test ESP must be a **GPT disk with an EFI System Partition**
(`qemu-test/sb/mkesp-gpt.sh`); a bare FAT on the whole disk works for a single
file but is unreliable. shim's default 2nd stage is `\grubx64.efi`; its
VENDOR_CERT is our devel cert, so grub/UKI signed by our key pass directly (no
MOK enrollment).

## Implemented (components 1–6 of the plan)

| # | Piece | Where | Confidence |
|---|-------|-------|-----------|
| 1 | Dev key (db.key/crt/cer + guid) | `secure-boot/` | done |
| 2 | `sbsigntool-native` (sbsign/sbverify) | `recipes-devtools/sbsigntool/` (recipe + 6 patches copied verbatim from meta-secure-core `meta-signing-key`; `BBCLASSEXTEND=native`) | high (proven upstream recipe) |
| 3 | `sb-sign.bbclass` | `classes/sb-sign.bbclass` — `sb_sign_file` (shell) + `sb_sign_path` (python); keys via `SB_KEYDIR` (set in `conf/layer.conf`); gated by `SECURE_BOOT_SIGN` | high |
| 4 | `shim` recipe | `recipes-bsp/shim/shim_16.1.bb` (+ binutils patch) — release tarball, `VENDOR_CERT_FILE=db.cer`, then `sb_sign_file` | **needs a build pass** (see below) |
| 5 | grub signing | `recipes-bsp/grub/grub-efi_%.bbappend` — `inherit sb-sign` + `do_deploy:append` signs `${GRUB_IMAGE_PREFIX}${GRUB_IMAGE}` | high (signing path proven in QEMU) |
| 6 | UKI signing | `classes/xen-unified-image.bbclass` — `sb_sign_path` after `uki-generate` | high (proven in QEMU) |

## Userspace SB tooling in the image (added 2026-05-28)

`packagegroup-tb-secureboot` (pulled into `tb-full-initramfs`) ships:

- **mokutil** — manage the MOK (Machine Owner Key) list and shim's SB state
  via efivars (e.g. `mokutil --import db.cer`, `mokutil --sb-state`). Recipe:
  `recipes-support/mokutil/mokutil_0.7.2.bb`. Depends on `efivar keyutils
  openssl libxcrypt`; needs `efivarfs` mounted at runtime (pulled via
  `kernel-module-efivarfs` RRECOMMENDS).
- **pesign** — PE/COFF Authenticode signing toolset (`pesign`, `pesigcheck`,
  `pesum`, `efikeygen`, `authvar`, `pesign-client`). Alternative/complement
  to `sbsigntool`. Recipe: `recipes-support/pesign/pesign_116.bb`. Depends on
  `efivar nss nspr popt util-linux`. We install only the CLIs (no systemd
  service / pesign daemon).
- **efivar** — `efivar(8)` for raw EFI variable inspection (poky recipe,
  pulled as a hard RDEPENDS of mokutil and useful for debugging).

## Not done (out of "up to phase 6")
- **7. ESP/wic layout** — placing signed shim as `EFI/BOOT/BOOTX64.EFI`, grub as
  `grubx64.efi`, the UKI alongside, and an embedded grub cfg that chainloads the
  UKI. Until this lands, shim isn't pulled into any image (so the shim recipe is
  parsed but not built by `tb-full-initramfs`).
- **8. Enrollment docs / shipping db.cer on the image.**

## Validation notes
- I could not run bitbake in this environment, so builds aren't verified here.
- **sbsigntool / sb-sign / grub / UKI** are high-confidence: the recipe is the
  proven upstream one; the signing logic is exactly what the QEMU harness in
  `workspace/qemu-test/sb/` already validated end-to-end (signed grub + 107 MB
  UKI accepted under enforced Secure Boot, unsigned rejected).
- **shim is the one to shake out.** Test it in isolation first:
  `bitbake shim` (with `SECURE_BOOT_SIGN=1`). Likely iteration points: exact
  `make` target/output name (`shimx64.efi`), gnu-efi bundling, and `EXTRA_OEMAKE`
  vars. The binutils-2.46 patch is included.
- Quick offline check on any signed artifact:
  `sbverify --cert secure-boot/db.crt <file>` (also done automatically by the
  signing helpers).

## How to exercise now
1. `SECURE_BOOT_SIGN=1` build of `tb-full-initramfs` → grub + UKI come out signed.
2. Drop the signed `grub-efi-bootx64.efi` and `xen-*.unified.efi` into
   `workspace/qemu-test/sb/` staging and run the harness to confirm enforcement.
3. `bitbake shim` separately to validate the shim recipe; then wire phase 7.
