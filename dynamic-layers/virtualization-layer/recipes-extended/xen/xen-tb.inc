LIC_FILES_CHKSUM ?= "file://COPYING;md5=d1a1e216f80b6d8da95fec897d0dbec9"

PV = "${XEN_REL}+tb"

SRC_URI = "git://github.com/accek-itl/xen.git;branch=${XEN_BRANCH};protocol=https"

SRCREV = "14139fb3a91e73c42c8ddee155c28071f77356f9"

S = "${WORKDIR}/git"

XEN_REL = "4.17"
XEN_BRANCH = "aem-staging-2026-03-13-accek"
