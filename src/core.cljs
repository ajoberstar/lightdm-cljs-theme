(ns org.ajoberstar.lightdm.core
  (:require [om.core :as om]
            [om.dom :as dom]))

(enable-console-print!)

(def clock-state (atom {}))

(defn- tick []
  (swap! clock-state assoc :time (js/Date.)))

(defn clock-view [{:keys [time interval-id]} _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [new-id (js/setInterval tick 1000)]
        (swap! clock-state assoc :interval-id new-id)))
    om/IWillUnmount
    (will-unmount [_]
      (js/clearInterval interval-id))
    om/IRender
    (render [_]
      (dom/span nil (str time)))))

(om/root clock-view
         clock-state
         {:target (. js/document (getElementById "time"))})


(def user-state (atom {:active-user nil
                       :users []}))

