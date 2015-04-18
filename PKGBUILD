# Maintainer: ajoberstar <ajoberstar at gmail dot com>

pkgname=lightdm-webkit-theme-cljs-git
pkgver=0.0.0.rc.1.2.gf115804
pkgrel=1
pkgdesc='LightDM Webkit theme written in ClojureScript.'
arch=(any)
url='https://github.com/ajoberstar/lightdm-cljs-theme'
license=('GPL3')
depends=('lightdm-webkit2-greeter')
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
