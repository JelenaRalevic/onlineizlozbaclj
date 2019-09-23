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

(defn get-vlasnik-login-page [&[error]]
  (render-file "views/vlasnik-login.html" {:title "Logovanje vlasnika"
                                   :error error}))

(defn get-user-by-username-from-db [params]
  (-> (select-keys params [:username])
      (db/find-user)
      (first)))

(defn get-vlasnik-by-username-from-db [params]
  (-> (select-keys params [:username])
      (db/find-vlasnik)
      (first)))


(defn login-page-submit [{:keys [params session]}]
  (let [user (get-user-by-username-from-db params)]
    (cond
      (not (login-validation? params))
      (get-login-page "Unesite svoj username i password")
      (empty? user)
      (get-login-page "Unesite kredencijale")
      :else
      (assoc (redirect "/userForma"):session (assoc session :identity user)))))

(defn vlasnik-login-page-submit [{:keys [params session]}]
  (let [vlasnik (get-vlasnik-by-username-from-db params)]
    (cond
      (not (login-validation? params))
      (get-vlasnik-login-page "Unesite svoj username i password")
      :else
      (assoc (redirect "/vlasnikForma"):session (assoc session :identity vlasnik)))))

(defn logout [request]
  (-> (redirect "/")
      (assoc :session {})))
(defn vlasnik-logout [request]
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
              :email (:email params)
              } register-schema))

(defn get-registration-page [&[error]]
  (render-file "views/registration.html" {:title "Registrovanje"
                                          :error error}))
(defn add-user-to-db [params]
  (-> (db/add-user (assoc params :rola "user"))))

(defn registration-page-submit [{:keys [params session]}]	
  (let [user (get-user-by-username-from-db params)]
    (cond
      (not (register-validaton? params))
      (get-registration-page "Potrebno je popuniti sva polja!")
      (not-empty user)
      (get-registration-page (str "Uneti kredencijali vec postoje!"))
      :else
      (assoc (redirect "/"):session (assoc session :identity (add-user-to-db params))))))

(def vregister-schema
  [[:imePrezime st/required st/string]
   [:grad st/required st/string]
   [:email st/required st/string]
   [:username st/required st/string]
   [:password st/required st/string]])

(defn vregister-validaton? [params]
  (st/valid? {:imePrezime (:imePrezime params)
              :grad (:grad params)
              :email (:email params)
              :username (:username params)
              :password (:password params)} vregister-schema))

(defn get-vregistration-page [&[error]]
  (render-file "views/vregistration.html" {:title "Registrovanje vlasnika"
                                          :error error}))
(defn add-vlasnik-to-db [params]
  (-> (db/add-vlasnik params)))

(defn vregistration-page-submit [{:keys [params session]}]
     (assoc (redirect "/vlogin"):session (assoc session :identity (add-vlasnik-to-db params))))

(defn get-useri [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-users)
    (db/search-user text)))
(defn get-search-useri [params session]
  (render-file "views/useri.html" {:title "Search user"
                                             :logged (:identity session)
                                             :useri (get-useri nil)}))

(defresource search-user [{:keys [params session]}]
  :allowed-methods [:post]
  :handle-created (json/write-str (get-useri (:text params)))
  :available-media-types ["application/json"])

(defresource search-user [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-useri params session)
                  "application/json" (->(:text params)
                                        (get-useri)
                                        (json/write-str)))))


(defroutes log-routes
           (GET "/login" [] (get-login-page))
           (POST "/login" request (login-page-submit request))
           (GET "/logout" request (logout request))
           (GET "/registration" [] (get-registration-page))
           (POST "/registration" request (registration-page-submit request))
           (GET "/vlogin" [] (get-vlasnik-login-page))
           (POST "/vlogin" request (vlasnik-login-page-submit request))
           (GET "/vlogout" request (vlasnik-logout request))
           (GET "/vregistration" [] (get-vregistration-page))
           (POST "/vregistration" request (vregistration-page-submit request))
           (GET "/useri" request (search-user request)))
           