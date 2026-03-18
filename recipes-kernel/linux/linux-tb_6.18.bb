# nooelint: oelint.file.requirenotfound
require recipes-kernel/linux/linux-yocto.inc

SUMMARY = "Linux kernel"
DESCRIPTION = "Linux kernel 6.18 accek-tb"
HOMEPAGE = "https://github.com/TrenchBoot/linux"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

DEPENDS += "${@bb.utils.contains('ARCH', 'x86', 'elfutils-native', '', d)}"
DEPENDS += "openssl-native util-linux-native coreutils-native"

PV = "6.18-rc3"
KBRANCH = "linux-sl-master-11-30-25-v15-accek"
KMETA = "kernel-meta"
SRC_URI = "\
    git://github.com/accek-itl/linux.git;protocol=https;branch=${KBRANCH};name=machine; \
    git://git.yoctoproject.org/yocto-kernel-cache;type=kmeta;name=meta;protocol=https;branch=yocto-6.18;destsuffix=${KMETA} \
    file://defconfig \
    file://debug.cfg \
    file://efi.cfg \
"
SRCREV_machine = "59d66ea9420b84ecae512941a5caaf1a2080b3f6"
SRCREV_meta = "104a5c9d30a5d90664b80accec85db6b11449d8f"

LINUX_VERSION ?= "6.18-rc3"

KCONFIG_MODE = "--alldefconfig"

# Skip kernel_configcheck: kconfiglib in yocto-kernel-cache doesn't support
# the 'transitional' Kconfig keyword introduced in 6.18
do_kernel_configcheck[noexec] = "1"

COMPATIBLE_MACHINE:pcengines-apux = "pcengines-apux"
