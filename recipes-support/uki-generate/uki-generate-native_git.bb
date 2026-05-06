SUMMARY = "Tool to generate a Xen Unified Kernel Image (UKI)"
DESCRIPTION = "Appends .config/.kernel/.ramdisk PE sections to xen.efi to \
produce a Xen unified kernel image. Sourced from qubes-core-admin-linux."
HOMEPAGE = "https://github.com/QubesOS/qubes-core-admin-linux"

LICENSE = "GPL-2.0-or-later"
# Upstream ships no top-level LICENSE; the package spec states GPLv2+.
LIC_FILES_CHKSUM = "file://rpm_spec/core-dom0-linux.spec.in;md5=6e064faa4d2c324967a5c058970c1317"

SRC_URI = "git://github.com/QubesOS/qubes-core-admin-linux.git;protocol=https;branch=main"
SRCREV = "32a14bba4a098770a9dc20fdb4c60f1e7bb82f90"

PV = "0.0+git${SRCPV}"
S = "${WORKDIR}/git"

inherit native

do_compile[noexec] = "1"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/uki-generate ${D}${bindir}/uki-generate
}
