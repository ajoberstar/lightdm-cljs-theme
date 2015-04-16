(ns org.ajoberstar.lightdm
  (:require-macros [reagent.ratom :as reagent])
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.local :as time]
            [cljs-time.format :as format]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reframe/register-sub
  :active-login
  (fn [db [_]]
    (reagent/reaction (:active-user @db))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reframe/register-handler
  :begin-login
  (fn [db [_ user]]
    (js/lightdm.start_authentication (:name user))
    (assoc db :active-user user)))

(reframe/register-handler
  :cancel-login
  (fn [db [_]]
    (js/lightdm.cancel_authentication)
    (dissoc db :active-user)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LightDM Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-lightdm [key]
  (-> (aget js/lightdm key)
      (js->clj :keywordize-keys true)))

;; callbacks that LightDM needs, but we don't use
(aset js/window "show_prompt" identity)
(aset js/window "authentication_complete" identity)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Trianglify images
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn trianglify [opts]
  (-> opts clj->js js/Trianglify .png))

(defn wallpaper []
  (letfn [(to-url [image] (str "url(" image ")"))]
    (-> {:width (aget js/window "innerWidth")
         :height (aget js/window "innerHeight")
         :cell_size (-> 75 rand-int (+ 50))
         :variance (-> 0.50 rand (+ 0.50))}
        trianglify
        to-url)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Power components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn power-component []
  [:div {:id "power"}
   [:button {:on-click #(js/lightdm.restart)}
    [:img {:src "/img/restart.svg"}]
    [:span "Restart"]]
   [:button {:on-click #(js/lightdm.shutdown)}
    [:img {:src "/img/shutdown.svg"}]
    [:span "Shutdown"]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def clock-format (format/formatter "yyyy-MM-dd hh:mm:ss A"))

(defn clock-component []
  (let [clock (reagent/atom (time/local-now))]
    (fn []
      (js/setTimeout #(reset! clock (time/local-now)) 1000)
      [:div {:id "time"} (format/unparse clock-format @clock)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn session-component []
  (let [sessions (get-lightdm "sessions")
        default (-> "default_session" get-lightdm :key)]
    [:select {:id "session" :defaultValue default}
     (map (fn [{:keys [key name]}]
            ^{:key key} [:option {:value key} name])
          sessions)]))

(defn login-component []
  [:div {:id "login"}
   [:form {:action "javascript: login()"}
    [:input {:id "password" :type "password" :placeholder "Password"}]
    [session-component]]])

(defn user-component [click]
  (fn [{:keys [display_name image name] :as user}]
    ^{:key name} [:div {:class "user"}
                  [:img {:class "avatar"
                         :src image
                         :on-click #(reframe/dispatch [click user])}]
                  [:span {:class "username"} display_name]]))

(defn user-defaults [user]
  (let [avatar-opts {:width 250 :height 250 :seed (:name user)}]
    (update user :image (fn [image]
                          (if (seq image)
                            image
                            (trianglify avatar-opts))))))

(defn users-component []
  (let [users (->> "users" get-lightdm (map user-defaults))
        active (reframe/subscribe [:active-login])]
    (fn []
      [:div {:id "users"}
       (if-let [active-user @active]
         [:div [(user-component :cancel-login) active-user] [login-component]]
         (map (user-component :begin-login) users))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Body component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn body-component []
  [:div {:id "container" :style {:background-image (wallpaper)}}
   [users-component]
   [:footer
    [power-component]
    [clock-component]]])

(reagent/render [body-component] (.-body js/document))
