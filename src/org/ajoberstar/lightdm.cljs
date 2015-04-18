(ns org.ajoberstar.lightdm
  (:require-macros [reagent.ratom :as reagent])
  (:require [reagent.core :as reagent]
            [re-frame.core :as reframe]
            [cljs-time.local :as time]
            [cljs-time.format :as format]))

(enable-console-print!)

(declare get-lightdm)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reframe/register-sub
  :active-auth
  (fn [db [_]]
    (reagent/reaction {:user (:active-user @db)
                       :ready (:show-prompt @db)})))

(reframe/register-sub
  :current-flash
  (fn [db [_]]
    (reagent/reaction (:flash-msg @db))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reframe/register-handler
  :auth-begin
  (fn [db [_ user]]
    (println "Auth begin")
    (js/lightdm.start_authentication (:name user))
    (assoc db :active-user user)))

(reframe/register-handler
  :auth-prompt
  (fn [db [_]]
    (println "Auth prompt")
    (assoc db :show-prompt true)))

(reframe/register-handler
  :auth-cancel
  (fn [db [_]]
    (println "Auth cancel")
    (js/lightdm.cancel_authentication)
    (-> db
        (dissoc :active-user)
        (dissoc :show-prompt)
        (dissoc :flash-msg))))

(reframe/register-handler
  :auth-do
  (fn [db [_ user]]
    (println "Auth do")
    (let [password (js/document.getElementById "password")
          session (js/document.getElementById "session")]
      (js/lightdm.provide_secret (aget password "value"))
      db)))

(reframe/register-handler
  :auth-complete
  (fn [db [_]]
    (println "Auth complete")
    (if (get-lightdm "is_authenticated")
      (do
        (js/lightdm.login
          (get-lightdm "authentication_user")
          (aget (js/document.getElementById "session") "value"))
        (assoc db :flash-msg "Login succeeded"))
      (do
        (aset (js/document.getElementById "password") "value" "")
        (reframe/dispatch [:auth-begin (:active-user db)])
        (assoc db :flash-msg "Login failed")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LightDM Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-lightdm [key]
  (-> (aget js/lightdm key)
      (js->clj :keywordize-keys true)))

(aset js/window "show_prompt" #(reframe/dispatch [:auth-prompt]))
(aset js/window "authentication_complete" #(reframe/dispatch [:auth-complete]))

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
    [:img {:src "img/restart.svg"}]
    [:span "Restart"]]
   [:button {:on-click #(js/lightdm.shutdown)}
    [:img {:src "img/shutdown.svg"}]
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
(defn flash-component []
  (let [flash (reframe/subscribe [:current-flash])]
    (fn []
      [:div {:id "flash"} @flash])))

(defn session-component []
  (let [sessions (get-lightdm "sessions")
        default (-> "default_session" get-lightdm :key)]
    [:select {:id "session" :defaultValue default}
     (map (fn [{:keys [key name]}]
            ^{:key key} [:option {:value key} name])
          sessions)]))

(defn login-component [user]
  [:div {:id "login"}
   [flash-component]
   [:form {:on-submit #(do
                         (reframe/dispatch [:auth-do user])
                         false)}
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
        auth (reframe/subscribe [:active-auth])]
    (fn []
      [:div {:id "users"}
       (if (:ready @auth)
         [:div
          [(user-component :auth-cancel) (:user @auth)]
          [login-component (:user @auth)]]
         (map (user-component :auth-begin) users))])))

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
