# nooelint: oelint.file.requirenotfound
require recipes-core/images/core-image-minimal.bb

SUMMARY = "TrenchBoot Compatibility Checker"
DESCRIPTION = "Minimal EFI-bootable image with only GRUB for compatibility testing"
LICENSE = "MIT"

IMAGE_FSTYPES = "wic wic.gz wic.bmap"
