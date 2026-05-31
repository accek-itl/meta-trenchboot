require grub-tb-common.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}/${DISTRO}:${THISDIR}/${PN}:"

SRC_URI += "file://cfg"

# Mirror Qubes' grub builder: build the host utility programs (grub-mkimage,
# grub-fstest, ...) with host flags rather than the freestanding target flags.
EXTRA_OECONF += "--with-utils=host"

# Poky's core2 tune (TUNE_CCARGS) injects -mfpmath=sse, which conflicts with the
# -mno-sse that grub adds for its freestanding target build: cc1 warns "SSE
# instruction set disabled, using 387 arithmetics". grub's configure feature
# probes use a hardcoded -Werror (e.g. the -no-pie detection in acinclude.m4),
# so that warning fails the probe; -no-pie is then dropped from TARGET_LDFLAGS
# and linking the EFI image at an absolute address fails on this default-PIE
# toolchain ("PHDR segment not covered by LOAD segment"). Fedora's grub build
# doesn't pass -mfpmath=sse, so drop it here to match and keep grub unpatched.
TUNE_CCARGS:remove = "-mfpmath=sse"


GRUB_BUILDIN = " \
                all_video boot btrfs cat chain configfile echo efifwsetup \
                efinet ext2 fat font gfxmenu gfxterm gzio halt hfsplus \
                iso9660 jpeg loadenv loopback lvm mdraid09 mdraid1x minicmd \
                normal part_apple part_msdos part_gpt password_pbkdf2 png \
                reboot regexp search search_fs_uuid search_fs_file \
                search_label serial sleep syslinuxcfg test tftp video xfs \
                backtrace http linux usb usbserial_common usbserial_pl2303 \
                usbserial_ftdi usbserial_usbdebug keylayouts at_keyboard \
                multiboot2 slaunch net lsmmap hexdump memrw \
                "

# UEFI Secure Boot: sign the assembled grub EFI image with our development key
# (no-op unless SECURE_BOOT_SIGN = "1"). grub keeps its shim_lock support
# (i.e. NOT built --disable-shim-lock) so it can verify the chainloaded Xen UKI
# via shim. The deployed image is ${GRUB_IMAGE_PREFIX}${GRUB_IMAGE}.
inherit sb-sign
do_deploy:append() {
    sb_sign_file "${DEPLOYDIR}/${GRUB_IMAGE_PREFIX}${GRUB_IMAGE}"
}
