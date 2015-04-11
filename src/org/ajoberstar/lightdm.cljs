(ns org.ajoberstar.lightdm
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.local :as time]
            [cljs-time.format :as format]))


(enable-console-print!)



(comment
  (def clock-format (format/formatter "yyyy-MM-dd hh:mm:ss A"))

  (def clock-state (atom {}))

  (defn- tick []
    (->> (time/local-now)
         (format/unparse clock-format)
         (swap! clock-state assoc :time)))

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

  (let [sessions (-> (. js/lightdm -sessions) (js->clj :keywordize-keys true))
        active (-> (. js/lightdm -default-session) (js->clj :keywordize-keys true))]
    (def sessions-state (atom {:active-session active
                               :sessions sessions})))

  (defn session-mapper [active]
    (fn [{:keys [key name]}]
      (if (= key active)
        (dom/option #js {:selected true :value key} name)
        (dom/option #js {:value key} name))))

  (defn sessions-view [{:keys [active-session sessions]}]
    (reify om/IRender
      (render [_]
        (apply dom/select nil
               (-> active-session :key session-mapper (map sessions))))))

  (om/root sessions-view
           sessions-state
           {:target (. js/document (getElementById "sessions"))})

  (defn- user-defaults [user]
    (update user :image (fn [img] (if (seq img) img "/img/unknown.svg"))))

  (let [users (-> (. js/lightdm -users) (js->clj :keywordize-keys true))]
    (def users-state (atom {:active-user nil
                            :users (map user-defaults users)})))

  (defn inactive-user-view [user]
    (reify om/IRender
      (render [_]
        (dom/div #js {:className "user"}
                 (dom/img #js {:className "avatar"
                               :src (:image user)}
                          nil)
                 (dom/span #js {:className "username"} (str (:display_name user)))))))

  (defn active-user-view [user]
    (reify om/IRender
      (render [_]
        (dom/div #js {:className "user"}
                 (dom/img #js {:className "avatar"
                               :src (:image user)}
                          nil)
                 (dom/span #js {:className "username"} (str (:display_name user)))))))

  (defn users-view [{:keys [active-user users]} _]
    (reify om/IRender
      (render [_]
        (if active-user
          (dom/div #js {:id "users"} (om/build active-user-view active-user))
          (apply dom/div #js {:id "users"}
                 (om/build-all inactive-user-view users))))))

  (om/root users-view
           users-state
           {:target (. js/document (getElementById "container"))}))
