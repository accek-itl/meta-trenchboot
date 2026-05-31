SUMMARY = "Tool to manage MOK (Machine Owner Key) database used by shim"
DESCRIPTION = "mokutil is the userspace utility for managing the Machine \
Owner Key list and related shim Secure Boot state stored in EFI variables. \
Used to enroll/delete MOK keys, query SB state, set MOK passwords, etc."
HOMEPAGE = "https://github.com/lcp/mokutil"
SECTION = "base"

LICENSE = "GPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504"

DEPENDS = "efivar keyutils openssl libxcrypt"

SRC_URI = "https://github.com/lcp/mokutil/archive/refs/tags/${PV}.tar.gz;downloadfilename=mokutil-${PV}.tar.gz"
SRC_URI[sha256sum] = "839d677c4fc9805f1565703ca32863e4652692c53da66a88ae9b9e30676f9e17"

S = "${WORKDIR}/mokutil-${PV}"

COMPATIBLE_HOST = "(i.86|x86_64|arm|aarch64).*-linux"

inherit autotools pkgconfig bash-completion

RRECOMMENDS:${PN} = "kernel-module-efivarfs"
# Pull the bash-completion sub-package automatically into anything that
# RDEPENDS on mokutil (so packagegroup-tb-secureboot doesn't have to know).
RRECOMMENDS:${PN} += "${PN}-bash-completion"
