SUMMARY = "Weston, a Wayland compositor, i.MX fork"
DESCRIPTION = "Weston is the reference implementation of a Wayland compositor"
HOMEPAGE = "http://wayland.freedesktop.org"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=d79ee9e66bb0f95d3386a7acae780b70 \
                    file://libweston/compositor.c;endline=26;md5=e342df749174a8ee11065583157c7a38"

SRCBRANCH = "weston-imx-4.0"
SRC_URI = "git://source.codeaurora.org/external/imx/weston-imx.git;protocol=https;branch=${SRCBRANCH} \
           file://weston.png \
           file://weston.desktop \
           file://0001-make-error-portable.patch \
           file://xwayland.weston-start \
           file://0001-weston-launch-Provide-a-default-version-that-doesn-t.patch \
"
SRCREV = "bf74309dd16d86537ff07eabf8b499cb2d25277d"
S = "${WORKDIR}/git"

UPSTREAM_CHECK_URI = "https://wayland.freedesktop.org/releases.html"

inherit autotools pkgconfig useradd distro_features_check
# depends on virtual/egl
REQUIRED_DISTRO_FEATURES = "opengl"

DEPENDS = "libxkbcommon gdk-pixbuf pixman cairo glib-2.0 jpeg"
DEPENDS += "wayland wayland-protocols libinput virtual/egl pango wayland-native"

WESTON_MAJOR_VERSION = "${@'.'.join(d.getVar('PV').split('.')[0:1])}"

EXTRA_OECONF = "--enable-setuid-install \
                --disable-rdp-compositor \
                "
EXTRA_OECONF_append_qemux86 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_qemux86-64 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_mx6 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
EXTRA_OECONF_append_mx7 = "\
		WESTON_NATIVE_BACKEND=fbdev-backend.so \
		"
PACKAGECONFIG ??= "${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'kms fbdev wayland egl', '', d)} \
                   ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', 'xwayland', '', d)} \
                   ${@bb.utils.filter('DISTRO_FEATURES', 'pam systemd x11', d)} \
                   clients launch"
# drm is not supported on mx6/mx7
PACKAGECONFIG_remove_mx6 = "kms"
PACKAGECONFIG_remove_mx7 = "kms"
PACKAGECONFIG_append_imxgpu2d = " imxg2d"
#
# Compositor choices
#
# Weston on KMS
PACKAGECONFIG[kms] = "--enable-drm-compositor,--disable-drm-compositor,drm udev virtual/mesa mtdev"
# Weston on Wayland (nested Weston)
PACKAGECONFIG[wayland] = "--enable-wayland-compositor,--disable-wayland-compositor,virtual/mesa"
# Weston on X11
PACKAGECONFIG[x11] = "--enable-x11-compositor,--disable-x11-compositor,virtual/libx11 libxcb libxcb libxcursor cairo"
# Headless Weston
PACKAGECONFIG[headless] = "--enable-headless-compositor,--disable-headless-compositor"
# Weston on framebuffer
PACKAGECONFIG[fbdev] = "--enable-fbdev-compositor,--disable-fbdev-compositor,udev mtdev"
# weston-launch
PACKAGECONFIG[launch] = "--enable-weston-launch,--disable-weston-launch,drm"
# VA-API desktop recorder
PACKAGECONFIG[vaapi] = "--enable-vaapi-recorder,--disable-vaapi-recorder,libva"
# Weston with EGL support
PACKAGECONFIG[egl] = "--enable-egl --enable-simple-egl-clients,--disable-egl --disable-simple-egl-clients,virtual/egl"
# Weston with cairo glesv2 support
PACKAGECONFIG[cairo-glesv2] = "--with-cairo-glesv2,--with-cairo=image,cairo"
# Weston with lcms support
PACKAGECONFIG[lcms] = "--enable-lcms,--disable-lcms,lcms"
# Weston with webp support
PACKAGECONFIG[webp] = "--with-webp,--without-webp,libwebp"
# Weston with systemd-login support
PACKAGECONFIG[systemd] = "--enable-systemd-login,--disable-systemd-login,systemd dbus"
# Weston with Xwayland support (requires X11 and Wayland)
PACKAGECONFIG[xwayland] = "--enable-xwayland,--disable-xwayland"
# colord CMS support
PACKAGECONFIG[colord] = "--enable-colord,--disable-colord,colord"
# Clients support
PACKAGECONFIG[clients] = "--enable-clients --enable-simple-clients --enable-demo-clients-install,--disable-clients --disable-simple-clients"
# Weston with PAM support
PACKAGECONFIG[pam] = "--with-pam,--without-pam,libpam"
# Weston with i.MX G2D renderer
PACKAGECONFIG[imxg2d] = "--enable-imxg2d,--disable-imxg2d,virtual/libg2d"

do_install_append() {
	# Weston doesn't need the .la files to load modules, so wipe them
	rm -f ${D}/${libdir}/libweston-${WESTON_MAJOR_VERSION}/*.la

	# If X11, ship a desktop file to launch it
	if [ "${@bb.utils.filter('DISTRO_FEATURES', 'x11', d)}" ]; then
		install -d ${D}${datadir}/applications
		install ${WORKDIR}/weston.desktop ${D}${datadir}/applications

		install -d ${D}${datadir}/icons/hicolor/48x48/apps
		install ${WORKDIR}/weston.png ${D}${datadir}/icons/hicolor/48x48/apps
	fi

	if [ "${@bb.utils.contains('PACKAGECONFIG', 'xwayland', 'yes', 'no', d)}" = "yes" ]; then
		install -Dm 644 ${WORKDIR}/xwayland.weston-start ${D}${datadir}/weston-start/xwayland
	fi
}

PACKAGES += "${@bb.utils.contains('PACKAGECONFIG', 'xwayland', '${PN}-xwayland', '', d)} \
             libweston-${WESTON_MAJOR_VERSION} ${PN}-examples"

FILES_${PN} = "${bindir}/weston ${bindir}/weston-terminal ${bindir}/weston-info ${bindir}/weston-launch ${bindir}/wcap-decode ${libexecdir} ${libdir}/${BPN}/*.so ${datadir}"

FILES_libweston-${WESTON_MAJOR_VERSION} = "${libdir}/lib*${SOLIBS} ${libdir}/libweston-${WESTON_MAJOR_VERSION}/*.so"
SUMMARY_libweston-${WESTON_MAJOR_VERSION} = "Helper library for implementing 'wayland window managers'."

FILES_${PN}-examples = "${bindir}/*"

FILES_${PN}-xwayland = "${libdir}/libweston-${WESTON_MAJOR_VERSION}/xwayland.so"
RDEPENDS_${PN}-xwayland += "xserver-xorg-xwayland"

RDEPENDS_${PN} += "xkeyboard-config"
RRECOMMENDS_${PN} = "liberation-fonts"
RRECOMMENDS_${PN}-dev += "wayland-protocols"

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM_${PN} = "--system weston-launch"
