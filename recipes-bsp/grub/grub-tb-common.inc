PV = "2.12"

SRC_URI = " \
           git://github.com/accek-itl/grub.git;name=grub;branch=${BRANCH_grub};protocol=https \
           git://github.com/coreutils/gnulib;name=gnulib;branch=${BRANCH_gnulib};protocol=https;destsuffix=git/gnulib \
          "

BRANCH_grub = "tb-dev-accek-test12"
SRCREV_grub = "110d103c723118cedd15dcb168d0bc15f8cd42e5"

# We add gnulib to SRC_URI to avoid downloading it during the do_configure().
# The bootstrap script seems to be smart enough to skip fetching repo again
# if the local revision is correct. Another way to "solve" that would be
# to add gnulib as a submodule in our fork.
#
# We need to update this with each grub rebase, to make sure it is in sync
# with the bootstrap.conf, otherwise it will be sync during do_configure()
# again.
BRANCH_gnulib = "master"
SRCREV_gnulib = "9f48fb992a3d7e96610c4ce8be969cff2d61a01b"

SRCREV_FORMAT = "grub_gnulib"

TARGET_CC_ARCH += "${LDFLAGS}"

S = "${WORKDIR}/git"

do_configure:prepend() {
    cd ${S}
    ${S}/bootstrap
    ${S}/autogen.sh
}

do_configure:append() {
    # Work around a workaround in poky!
    #
    # Tarballs of 2.12 are missing this file, so poky creates it not taking
    # into account that this breaks out-of-tree builds: shell commands in Make
    # rules run in the build tree and can't find the file in the source tree.
    #
    # We ensure an empty extra_deps.lst exists in both trees: grub-efi's
    # Makefile uses $(top_srcdir) (needs it in S), while grub/grub-native
    # use a relative path (needs it in B).
    : > ${S}/grub-core/extra_deps.lst
    if [ "${B}" != "${S}" ]; then
        mkdir -p ${B}/grub-core
        : > ${B}/grub-core/extra_deps.lst
    fi
}
