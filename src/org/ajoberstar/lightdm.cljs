(ns org.ajoberstar.lightdm
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.local :as time]
            [cljs-time.format :as format]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def clock-format (format/formatter "yyyy-MM-dd hh:mm:ss A"))

(defn clock-component []
  (let [clock (reagent/atom (time/local-now))]
    (fn []
      (js/setTimeout #(reset! clock (time/local-now)) 1000)
      [:span (format/unparse clock-format @clock)])))

(reagent/render [clock-component] (js/document.getElementById "time"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Session component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [sessions (-> (. js/lightdm -sessions) (js->clj :keywordize-keys true))
      active (-> (. js/lightdm -default-session) (js->clj :keywordize-keys true))]
  (def sessions-db (reagent/atom {:active-session active
                                  :sessions sessions})))

(defn session-component []
  (let [{:keys [active-session sessions]} @sessions-db
        active-key (:key active-session)]
    [:select
     {:defaultValue active-key}
     (map (fn [{:keys [key name]}]
            ^{:key key} [:option {:value key} name])
          sessions)]))

(reagent/render [session-component] (js/document.getElementById "sessions"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
