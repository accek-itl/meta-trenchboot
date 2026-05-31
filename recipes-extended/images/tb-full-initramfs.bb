# nooelint: oelint.file.requirenotfound
require recipes-core/images/core-image-minimal.bb

inherit xen-unified-image

SUMMARY = "TrenchBoot full initramfs image for network boot"
DESCRIPTION = "Self-contained initramfs with Xen support, intended for PXE/network boot."
LICENSE = "MIT"

IMAGE_FEATURES:append = " ssh-server-openssh"

IMAGE_INSTALL:append = " \
                        packagegroup-tb-base \
                        packagegroup-security-tpm2 \
                        packagegroup-tb-tests \
                        packagegroup-tb-secureboot \
                        "

# Based on xen-image-minimal from meta-virtualization
XEN_PCIBACK_MODULE = ""
XEN_PCIBACK_MODULE:x86 = "kernel-module-xen-pciback"
XEN_PCIBACK_MODULE:x86-64 = "kernel-module-xen-pciback"
XEN_ACPI_PROCESSOR_MODULE = ""
XEN_ACPI_PROCESSOR_MODULE:x86 = "kernel-module-xen-acpi-processor"
XEN_ACPI_PROCESSOR_MODULE:x86-64 = "kernel-module-xen-acpi-processor"

XEN_KERNEL_MODULES ?= " \
                       kernel-module-xen-blkback kernel-module-xen-gntalloc kernel-module-tun \
                       kernel-module-xen-gntdev kernel-module-xen-netback kernel-module-xen-wdt \
                       ${@bb.utils.contains('MACHINE_FEATURES', 'pci', "${XEN_PCIBACK_MODULE}", '', d)} \
                       ${@bb.utils.contains('MACHINE_FEATURES', 'acpi', '${XEN_ACPI_PROCESSOR_MODULE}', '', d)} \
                      "

IMAGE_INSTALL:append = " \
                        ${XEN_KERNEL_MODULES} \
                        xen-tools \
                        qemu-system-x86-64 \
                        "

# Don't include kernel or debug vmlinux in the initramfs
PACKAGE_EXCLUDE = "kernel-image-* kernel-vmlinux"

# Output only cpio.xz for better compression
IMAGE_FSTYPES = "cpio.xz"

# Secure Boot: this is a cpio.xz/PXE image with no ESP to assemble. Build the
# signed shim alongside the Xen UKI (both land in DEPLOY_DIR_IMAGE for the
# netboot server). grub is intentionally NOT pulled in here: it is built by
# build-grub.sh, which mounts the slaunch-enabled grub worktree
# (grub-slaunch-rebase) as /work/grub -- this target's mount (grub-qubes-rebase)
# has no slaunch module, so building grub here would fail do_mkimage.
do_image_complete[depends] += "shim:do_deploy"

do_check_xen_state() {
    if [ "${@bb.utils.contains('DISTRO_FEATURES', 'xen', ' yes', 'no', d)}" = "no" ]; then
        die "DISTRO_FEATURES does not contain 'xen'"
    fi
}

addtask check_xen_state before do_rootfs
