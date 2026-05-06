# Build a Xen "Unified Kernel Image" by appending PE sections to xen.efi.
# See xen/docs/misc/efi.pandoc and
# https://xenbits.xen.org/docs/unstable/misc/efi.html#unified-xen-kernel-image
#
# Delegates the section append/layout to QubesOS' uki-generate, provided
# by the uki-generate-native recipe.

XEN_UNIFIED_NAME      ?= "xen-${MACHINE}.unified.efi"

XEN_UNIFIED_XEN_EFI   ?= "${DEPLOY_DIR_IMAGE}/xen-${MACHINE}.efi"
# Filename of the embedded xen.cfg, resolved against FILESPATH at task time.
# Image recipes inheriting this class should ship the file under their
# files/ directory (which is on FILESPATH automatically).
XEN_UNIFIED_CFG_NAME  ?= "xen-unified.cfg"
XEN_UNIFIED_KERNEL    ?= "${DEPLOY_DIR_IMAGE}/bzImage-${MACHINE}.bin"
XEN_UNIFIED_RAMDISK   ?= "${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.cpio.xz"
XEN_UNIFIED_UCODE    ??= ""
XEN_UNIFIED_XSM      ??= ""

do_xen_unified_image[depends] += "xen:do_deploy"
do_xen_unified_image[depends] += "virtual/kernel:do_deploy"
do_xen_unified_image[depends] += "binutils-native:do_populate_sysroot"
do_xen_unified_image[depends] += "uki-generate-native:do_populate_sysroot"

# Track the embedded xen.cfg in the task signature so edits invalidate the
# cached unified image. Resolved at parse time against FILESPATH.
do_xen_unified_image[file-checksums] += "${@bb.utils.which(d.getVar('FILESPATH'), d.getVar('XEN_UNIFIED_CFG_NAME')) or ''}:True"

addtask xen_unified_image after do_image_complete before do_build

python do_xen_unified_image() {
    import os, subprocess

    xen_efi = d.getVar('XEN_UNIFIED_XEN_EFI')
    cfg     = d.getVar('XEN_UNIFIED_CFG') or bb.utils.which(
                  d.getVar('FILESPATH'), d.getVar('XEN_UNIFIED_CFG_NAME'))
    kernel  = d.getVar('XEN_UNIFIED_KERNEL')
    ramdisk = d.getVar('XEN_UNIFIED_RAMDISK')
    ucode   = d.getVar('XEN_UNIFIED_UCODE') or ''
    xsm     = d.getVar('XEN_UNIFIED_XSM') or ''
    uki     = os.path.join(d.getVar('STAGING_BINDIR_NATIVE'), 'uki-generate')

    for label, path in (('xen.efi', xen_efi), ('xen.cfg', cfg),
                        ('kernel', kernel), ('ramdisk', ramdisk)):
        if not path or not os.path.isfile(path):
            bb.fatal("xen-unified-image: required %s not found at %s" % (label, path))

    out_path = os.path.join(d.getVar('DEPLOY_DIR_IMAGE'), d.getVar('XEN_UNIFIED_NAME'))

    cmd = [uki, xen_efi, cfg, kernel, ramdisk]
    if ucode and os.path.isfile(ucode):
        cmd += ['--custom-section', 'ucode', ucode]
    if xsm and os.path.isfile(xsm):
        cmd += ['--custom-section', 'xsm', xsm]
    cmd += [out_path]

    bb.note("xen-unified-image: %s" % ' '.join(cmd))
    subprocess.check_call(cmd)
}
