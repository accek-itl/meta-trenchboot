SUMMARY = "Trenchboot support packagegroup"
DESCRIPTION = "Trenchboot support packagegroup"

inherit packagegroup

PACKAGES = " \
            ${PN}-base \
            ${PN}-utils \
            ${PN}-tests \
            ${PN}-secureboot \
            "

RDEPENDS:${PN}-base += " \
                        kernel-modules \
                        skl \
                        intel-sinit-acm \
                        aem \
                        "

RDEPENDS:${PN}-utils += " \
                         packagegroup-security-tpm2 \
                         util-linux-bash-completion \
                         vim \
                         rsync \
                         kexec \
                         gawk \
                         "

# Secure Boot / MOK / Authenticode userspace tooling.
# mokutil:  read/write MOK list + SB state via efivars (efivarfs).
# pesign:   sign and verify PE binaries (alternative to sbsigntool); also
#           ships efikeygen for generating SB signing keys.
# binutils: provides objcopy/objdump/strip/readelf, frequently needed
#           alongside pesign to inspect/manipulate PE sections (.sbat,
#           .reloc, etc.) when iterating on shim/grub/UKI artifacts.
RDEPENDS:${PN}-secureboot += " \
                              mokutil \
                              pesign \
                              efivar \
                              binutils \
                              "

RDEPENDS:${PN}-tests = " \
    trenchboot-tests \
    trenchboot-hcl-report \
"
