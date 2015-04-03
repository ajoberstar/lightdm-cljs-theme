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


(def users-state (atom {:active-user nil
                       :users []}))

(->> (js->clj (. js/lightdm -users) :keywordize-keys true)
     (map (fn [user] (if (-> user :image seq)
                       user
                       (assoc user :image "/img/unknown.svg"))))
     (swap! users-state assoc :users))

(defn inactive-user-view [user]
  (reify om/IRender
    (render [_]
      (dom/img #js {:className "avatar"
                    :src (:image user)}
               nil))))

(defn users-view [{:keys [active-user users]} _]
  (reify om/IRender
    (render [_]
      (apply dom/div nil (om/build-all inactive-user-view users)))))

(om/root users-view
         users-state
         {:target (. js/document (getElementById "users"))})
