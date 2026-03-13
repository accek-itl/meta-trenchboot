LIC_FILES_CHKSUM ?= "file://COPYING;md5=d1a1e216f80b6d8da95fec897d0dbec9"

PV = "${XEN_REL}+tb"

SRC_URI = "git://github.com/TrenchBoot/xen.git;branch=${XEN_BRANCH};protocol=https"

SRCREV = "b1c8fd1cb769ff1e4a7612ebada76e323155bba1"

S = "${WORKDIR}/git"

XEN_REL = "4.22"
XEN_BRANCH = "aem-staging-2026-03-13"
