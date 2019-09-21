(ns onlineizlozbaclj.routes.loginregistration  
  (:require [compojure.core :refer :all]
            [bcrypt-clj.auth :as bcrypt]
            [struct.core :as st]
            [ring.util.response :refer [redirect]]
            [selmer.parser :refer [render-file]]
            [onlineizlozbaclj.models.baza :as db]
            [compojure.response :refer [render]]
            [buddy.auth :refer [authenticated?]]
            [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [liberator.representation :refer [ring-response as-response]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]))

(def login-schema
  {:username [st/required st/string]
   :password [st/required st/string]})

(defn login-validation? [params]
  (st/valid? {:username (:username params)
              :password (:password params)} login-schema))

(defn get-login-page [&[error]]
  (render-file "views/login.html" {:title "Logovanje"
                                   :error error}))

(defn get-user-by-username-from-db [params]
  (-> (select-keys params [:username])
      (db/find-user)
      (first)))


(defn login-page-submit [{:keys [params session]}]
  (let [user (get-user-by-username-from-db params)]
    (cond
      (not (login-validation? params))
      (get-login-page "Unesite svoj username i password")
      (empty? user)
      (get-login-page "Unesite kredencijale")
      :else
      (assoc (redirect "/"):session (assoc session :identity user)))))


(defn logout [request]
  (-> (redirect "/")
      (assoc :session {})))


(def register-schema
  [[:imePrezime st/required st/string]
   [:username st/required st/string]
   [:password st/required st/string]
   [:email st/required st/email]])

(defn register-validaton? [params]
  (st/valid? {:imePrezime (:imePrezime params)
              :username (:username params)
              :password (:password params)
              :email (:email params)} register-schema))

(defn get-registration-page [&[error]]
  (render-file "views/registration.html" {:title "Registrovanje"
                                          :error error}))
(defn add-user-to-db [params]
  (-> (db/add-user (assoc params :rola "vlasnik"))))

(defn registration-page-submit [{:keys [params session]}]	
  (let [user (get-user-by-username-from-db params)]
    (cond
      (not (register-validaton? params))
      (get-registration-page "Potrebno je popuniti sva polja!")
      (not-empty user)
      (get-registration-page (str "Uneti kredencijali vec postoje!"))
      :else
      (assoc (redirect "/"):session (assoc session :identity (add-user-to-db params))))))



(defroutes log-routes
           (GET "/login" [] (get-login-page))
           (POST "/login" request (login-page-submit request))
           (GET "/logout" request (logout request))
           (GET "/registration" [] (get-registration-page))     
           (POST "/registration" request (registration-page-submit request)))
           