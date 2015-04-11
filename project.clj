(defproject lightdm-cljs-theme "0.1.0-SNAPSHOT"
  :description "LightDM Webkit theme written in CLJS."
  :url "https://github.com/ajoberstar/lightdm-cljs-theme"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.7.0-beta1"]

                 ;;js
                 [org.clojure/clojurescript "0.0-3196"]
                 [reagent "0.5.0"]
                 [re-frame "0.2.0"]
                 [com.andrewmcveigh/cljs-time "0.3.3"]
                 [figwheel "0.2.5"]]
  :resource-paths ["resources" "target/resources"]
  :target-path "target/%s"
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.2.5"]]
  :cljsbuild {:builds [{:id "development"
                        :source-paths ["src" "dev"]
                        :compiler {:output-to "target/resources/public/js/main.js"
                                   :output-dir "target/resources/public/js"
                                   :asset-path "js"
                                   :main org.ajoberstar.dev
                                   :optimizations :none}}]}
  :figwheel {:css-dirs ["resources/public/css"]})
