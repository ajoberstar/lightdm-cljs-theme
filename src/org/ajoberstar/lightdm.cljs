(ns org.ajoberstar.lightdm
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.local :as time]
            [cljs-time.format :as format]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Trianglify background
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- new-wallpaper []
  (-> {:width     (aget js/window "innerWidth")
       :height    (aget js/window "innerHeight")
       :cell_size (-> 100 rand-int (+ 25))
       :variance  (-> 0.75 rand (+ 0.25))}
      clj->js
      js/Trianglify
      .png))

(defn wallpaper-component []
  [:img {:src (new-wallpaper)}])

(reagent/render [wallpaper-component] (js/document.getElementById "wallpaper"))

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
    [:select {:id "session"}
     {:defaultValue active-key}
     (map (fn [{:keys [key name]}]
            ^{:key key} [:option {:value key} name])
          sessions)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- user-defaults [user]
  (if (seq (:image user))
    user
    (assoc user :image "/img/unknown.svg")))

(let [users (-> (. js/lightdm -users) (js->clj :keywordize-keys true))]
  (def users-db (reagent/atom {:active-user nil
                               :users (map user-defaults users)})))

(defn- login []
  (let [password (-> (js/document.getElementById "password") .-value)
        session (-> (js/document.getElementById "session") .-value)]
    (js/lightdm.provide_secret password)
    (if js/lightdm.is_authenticated
      (js/lightdm.login js/lightdm.authentication_user session)
      (do
        (aset (js/document.getElementById "password") "value" "")
        (println "Login failed.")))))

(aset js/window "login" login)

(defn login-component []
  [:form {:action "javascript: login()"}
   [:input {:id "password" :type "password" :placeholder "Password"}]
   [session-component]])

(defn- toggle-login [current-active user]
  (if current-active
    (do
      (js/lightdm.cancel_authentication)
      nil)
    (do
      (js/lightdm.start_authentication (:name user))
      user)))

(defn user-component [{:keys [display_name image name] :as user}]
  ^{:key name} [:div {:class "user"}
                [:img {:class "avatar"
                       :src image
                       :on-click #(swap! users-db update :active-user toggle-login user)}]
                [:span {:class "username"} display_name]
                (if (= user (:active-user @users-db)) [login-component])])

(defn users-component []
  (let [{:keys [active-user users]} @users-db]
    [:div {:id "users"}
     (if active-user
       [user-component active-user]
       (doall (map user-component users)))]))

(reagent/render [users-component] (js/document.getElementById "container"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; callback needed by lightdm, we don't use it
(aset js/window "show_prompt" identity)
(aset js/window "authentication_complete" identity)
