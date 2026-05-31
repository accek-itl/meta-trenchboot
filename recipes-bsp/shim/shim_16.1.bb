SUMMARY = "UEFI Secure Boot first-stage loader (shim)"
DESCRIPTION = "shim is a trivial EFI application that loads and verifies a \
second-stage loader (grub) via LoadImage/StartImage or its built-in \
VENDOR_CERT. Built here with our development cert as VENDOR_CERT and signed \
with our development key (see secure-boot/README.md). Self-contained: built \
from the upstream rhboot/shim release tarball (which bundles gnu-efi), with \
none of meta-secure-core's key-management framework."
HOMEPAGE = "https://github.com/rhboot/shim"
SECTION = "bootloaders"

LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=b92e63892681ee4e8d27e7a7e87ef2bc"

DEPENDS = "openssl util-linux-native openssl-native"

# Release tarball bundles gnu-efi, so no submodule/gnu-efi-native dance.
SRC_URI = "https://github.com/rhboot/shim/releases/download/${PV}/shim-${PV}.tar.bz2 \
           file://0001-Fix-build-with-binutils-2.46.patch \
          "
SRC_URI[sha256sum] = "46319cd228d8f2c06c744241c0f342412329a7c630436fce7f82cf6936b1d603"

S = "${WORKDIR}/shim-${PV}"

COMPATIBLE_HOST = "(i.86|x86_64).*-linux"
PARALLEL_MAKE = ""

EFI_ARCH = "x64"
EFI_ARCH:x86 = "ia32"
EFI_ARCH:x86-64 = "x64"

inherit deploy sb-sign

# Bake our development cert (DER) as shim's VENDOR_CERT so shim trusts our key
# for the second stage (grub), in addition to whatever the firmware has in db.
# We do NOT use shim's own ENABLE_SBSIGN; the binary is signed afterwards with
# the same key via sb_sign_file (so the firmware launches it from db).
EXTRA_OEMAKE = "\
    CROSS_COMPILE=${TARGET_PREFIX} \
    LIB_GCC=`${CC} -print-libgcc-file-name` \
    OPENSSL=${STAGING_BINDIR_NATIVE}/openssl \
    HEXDUMP=${STAGING_BINDIR_NATIVE}/hexdump \
    VENDOR_CERT_FILE=${SB_CERT_DER} \
"

do_compile() {
    oe_runmake
}

do_install() {
    install -d ${D}/boot/efi/EFI/BOOT
    install -m 0644 ${B}/shim${EFI_ARCH}.efi ${D}/boot/efi/EFI/BOOT/BOOTX64.EFI
    # MokManager, useful for enrolling keys via the MOK menu (optional).
    [ -f ${B}/mm${EFI_ARCH}.efi ] && \
        install -m 0644 ${B}/mm${EFI_ARCH}.efi ${D}/boot/efi/EFI/BOOT/mm${EFI_ARCH}.efi || true
}
FILES:${PN} = "/boot"

do_deploy() {
    install -d ${DEPLOYDIR}
    install -m 0644 ${B}/shim${EFI_ARCH}.efi ${DEPLOYDIR}/shim${EFI_ARCH}.efi
    sb_sign_file "${DEPLOYDIR}/shim${EFI_ARCH}.efi"
}
addtask deploy before do_build after do_install
