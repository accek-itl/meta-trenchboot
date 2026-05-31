# Sign EFI/PE artifacts (shim, grub, Xen UKI) for UEFI Secure Boot using the
# development key committed in this layer (see secure-boot/README.md).
#
# Opt-in: set SECURE_BOOT_SIGN = "1" (e.g. in the target's kas local_conf_header).
# When "0"/unset, every helper here is a no-op and nothing extra is built.
#
# Two helpers are provided:
#   sb_sign_file <path>   - shell, for do_deploy:append in recipes (grub, shim)
#   sb_sign_path(d, path) - python, for python tasks (the Xen UKI class)
# Both sign <path> in place (sbsign) and then verify it (sbverify).

SECURE_BOOT_SIGN ??= "0"

# Key material (committed development key). SB_KEYDIR is set in conf/layer.conf.
SB_KEY      ?= "${SB_KEYDIR}/db.key"
SB_CERT     ?= "${SB_KEYDIR}/db.crt"
SB_CERT_DER ?= "${SB_KEYDIR}/db.cer"

# Pull in sbsign/sbverify only when signing is enabled.
DEPENDS += "${@'sbsigntool-native' if d.getVar('SECURE_BOOT_SIGN') == '1' else ''}"

sb_sign_file() {
    if [ "${SECURE_BOOT_SIGN}" != "1" ]; then
        return 0
    fi
    if [ ! -f "${SB_KEY}" ] || [ ! -f "${SB_CERT}" ]; then
        bbfatal "sb-sign: missing key/cert (${SB_KEY} / ${SB_CERT})"
    fi
    bbnote "sb-sign: signing $1 with ${SB_CERT}"
    sbsign --key "${SB_KEY}" --cert "${SB_CERT}" --output "$1.signed" "$1"
    mv "$1.signed" "$1"
    sbverify --cert "${SB_CERT}" "$1"
}

def sb_sign_path(d, path):
    """Sign 'path' in place (and verify) if SECURE_BOOT_SIGN is enabled."""
    import os, subprocess
    if d.getVar('SECURE_BOOT_SIGN') != '1':
        return
    key  = d.getVar('SB_KEY')
    cert = d.getVar('SB_CERT')
    if not (key and cert and os.path.isfile(key) and os.path.isfile(cert)):
        bb.fatal("sb-sign: missing key/cert (%s / %s)" % (key, cert))
    nbin     = d.getVar('STAGING_BINDIR_NATIVE')
    sbsign   = os.path.join(nbin, 'sbsign')
    sbverify = os.path.join(nbin, 'sbverify')
    bb.note("sb-sign: signing %s with %s" % (path, cert))
    subprocess.check_call([sbsign, '--key', key, '--cert', cert,
                           '--output', path + '.signed', path])
    os.replace(path + '.signed', path)
    subprocess.check_call([sbverify, '--cert', cert, path])
