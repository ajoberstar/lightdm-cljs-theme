(ns org.ajoberstar.dev
  (:require [figwheel.client :as fw]
            [org.ajoberstar.lightdm]))

(fw/start {:on-jsload (fn [] (print "reloaded"))})
