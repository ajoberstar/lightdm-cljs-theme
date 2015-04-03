(ns org.ajoberstar.lightdm.dev
  (:require [figwheel.client :as fw]
            [org.ajoberstar.lightdm.core]))

(fw/start {:on-jsload (fn [] (print "reloaded"))})
