# Maintainer: ajoberstar <ajoberstar at gmail dot com>

pkgname=lightdm-webkit-theme-cljs-git
pkgver=0.0.0_36_g9144b56
pkgrel=1
pkgdesc='LightDM Webkit theme written in ClojureScript.'
arch=(any)
url='https://github.com/ajoberstar/lightdm-cljs-theme'
license=('GPL3')
depends=('lightdm-webkit-greeter')
makedepends=('git' 'java-environment' 'leiningen')
provides=('lightdm-webkit-theme-cljs')
source=('git://github.com/ajoberstar/lightdm-cljs-theme.git')

_gitname=lightdm-cljs-theme

pkgver() {
    cd "$_gitname"
    git describe --tags --long | sed 's/-/./g'
}

build() {
    cd "$_gitname"
    lein cljsbuild once production
    cp -r resources/* target/resources/
}

package() {
    mkdir -p ${pkgdir}/usr/share/lightdm-webkit/themes/cljs
    cp -r ${srcdir}/${_gitname}/target/resources/public/* ${pkgdir}/usr/share/lightdm-webkit/themes/cljs/ 
}

md5sums=('SKIP')
