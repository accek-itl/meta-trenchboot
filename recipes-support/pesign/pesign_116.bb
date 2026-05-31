SUMMARY = "PE/COFF Authenticode signing tools (pesign, efikeygen, etc.)"
DESCRIPTION = "pesign provides command-line tools to manipulate Authenticode \
signatures on PE/COFF binaries (e.g. shim, grub, kernel EFI stubs), generate \
signing keys (efikeygen), and verify signatures (pesigcheck/pesum). Useful as \
an alternative/complement to sbsigntools for Secure Boot workflows."
HOMEPAGE = "https://github.com/rhboot/pesign"
SECTION = "devel"

LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263"

DEPENDS = "efivar nss nspr popt util-linux"

SRC_URI = "https://github.com/rhboot/pesign/releases/download/${PV}/pesign-${PV}.tar.bz2"
SRC_URI[sha256sum] = "35331f75689863e5be595f2bb04a8bc934ce734b8d76fa5d6aeb4d85424e8996"

S = "${WORKDIR}/pesign-${PV}"

COMPATIBLE_HOST = "(i.86|x86_64|arm|aarch64).*-linux"

inherit pkgconfig

# pesign's Makefile uses -Werror and a pile of strict flags; binutils/gcc churn
# easily breaks the build, so relax those (mirrors how Fedora packages it).
# pesign's Makefile derives ARCH from `uname -m` on the build host; force it.
PESIGN_ARCH = "x86_64"
PESIGN_ARCH:x86 = "ia32"
PESIGN_ARCH:x86-64 = "x86_64"
PESIGN_ARCH:arm = "arm"
PESIGN_ARCH:aarch64 = "aarch64"

EXTRA_OEMAKE = "\
    ARCH=${PESIGN_ARCH} \
    HOSTARCH=${PESIGN_ARCH} \
    CC='${CC}' \
    CCLD='${CC}' \
    AR='${AR}' \
    RANLIB='${RANLIB}' \
    OBJCOPY='${OBJCOPY}' \
    PKG_CONFIG=pkg-config \
    prefix=${prefix}/ \
    libdir=${libdir}/ \
    libdatadir=${nonarch_libdir}/ \
    libexecdir=${libexecdir}/ \
    datadir=${datadir}/ \
    mandir=${mandir}/ \
    bindir=${bindir}/ \
    includedir=${includedir}/ \
    rundir=/run/ \
    CFLAGS='${CFLAGS} -Wno-error' \
    LDFLAGS='${LDFLAGS}' \
"

do_configure() {
    # pesign converts .mdoc to man pages via `mandoc`, which isn't in the
    # poky/oe layers we use. Strip man page building+install rather than
    # add a mandoc-native recipe (we don't need man pages in the image).
    sed -i \
        -e 's/^MAN1TARGETS=.*/MAN1TARGETS=/' \
        -e '/INSTALL.*MAN1TARGETS/d' \
        -e '/INSTALL.*mandir.*man1/d' \
        ${S}/src/Makefile
}

do_compile() {
    oe_runmake
}

do_install() {
    oe_runmake install INSTALLROOT=${D}
    # Don't ship the daemon's systemd unit or per-host run dir; we only need
    # the signing/verification CLIs (pesign, efikeygen, pesigcheck, pesum,
    # authvar, pesign-client). The libexec/ helpers are RPM-build scaffolding
    # (Fedora-only) that drag in a /bin/bash dependency we don't want.
    rm -rf ${D}/run
    rm -rf ${D}${libexecdir}/pesign
    rm -f  ${D}${sysconfdir}/rpm/macros.pesign
    rmdir --ignore-fail-on-non-empty ${D}${libexecdir} 2>/dev/null || true
    rmdir --ignore-fail-on-non-empty ${D}${sysconfdir}/rpm 2>/dev/null || true
}

# Default FILES picks up ${bindir}, ${libexecdir}, ${sysconfdir}. Add the
# empty pki dirs explicitly so QA doesn't drop them.
FILES:${PN} += "\
    ${sysconfdir}/pki/pesign \
    ${sysconfdir}/pki/pesign-rh-test \
"
ALLOW_EMPTY:${PN} = "1"
