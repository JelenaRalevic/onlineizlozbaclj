(ns onlineizlozbaclj.routes.userforma
  (:require [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]
            [compojure.response :refer [render]]
            [buddy.auth :refer [authenticated?]]
            [onlineizlozbaclj.models.baza :as db]
            [ring.util.response :refer [redirect]]
            [struct.core :as st]
            [compojure.response :refer [render]]
            [buddy.auth :refer [authenticated?]]
            [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [liberator.representation :refer [ring-response as-response]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]))

(def vlasnik-schema
  {:imePrezime [st/required st/string]
   :grad [st/required st/string]
   :email [st/required st/string]
   :username [st/required st/string]
   :password [st/required st/string]})

(defn vlasnik-validation? [params]
  (st/valid? {:imePrezime (:imePrezime params)
              :grad (:grad params)
              :email (:email params)
              :username (:username params)
              :password (:password params)} vlasnik-schema))

(defn authenticated-admin? [session]
  (and (authenticated? session)
       (="admin" (:rola (:identity session)))))

(defn get-userforma-page [page session]
  (render-file page
               {:title "Glavna user forma"
                :logged (:identity session)}))

(defn userforma [session]
  (cond
    (not (authenticated? session))
    (redirect "/login")
    (authenticated-admin? session)
    (get-userforma-page "views/adminForma.html" session)
    :else
    (get-userforma-page "views/userForma.html" session)))


(defn get-vlasnikforma-page [page session]
  (render-file page
               {:title "Glavna forma vlasnika"
                :logged (:identity session)}))

(defn vlasnikforma [session]
  (cond
    (not (authenticated? session))
    (redirect "/vlogin")
    :else
    (get-vlasnikforma-page "views/vlasnikForma.html" session)))

(defn get-vlasnici [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-vlasnik)
    (db/search-vlasnik text)))

(defn get-search-vlasnici [params session]
  (render-file "views/vlasnici.html" {:title "Search vlasnik"
                                             :logged (:identity session)
                                             :vlasnici (get-vlasnici nil)}))

(defresource search-vlasnik [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-vlasnici params session)
                  "application/json" (->(:text params)
                                        (get-vlasnici)
                                        (json/write-str)))))

(defresource delete-vlasnik [{:keys [params session]}]
  :allowed-methods [:delete] 
  (println params)
  (db/delete-pas-vlasnik (:vlasnikID params))
  (db/delete-izlozba-vlasnik (:vlasnikID params))
  (db/delete-vlasnik (:vlasnikID params))
  :available-media-types ["application/json"])

(defroutes forme-routes
  (GET "/userForma" request (userforma (:session request)))
  (POST "/userForma" request (userforma request))
  (GET "/vlasnikForma" request (vlasnikforma (:session request)))
  (POST "/vlasnikForma" request (vlasnikforma request))
  (GET "/vlasnici" request (search-vlasnik request))
  (DELETE "/vlasnik/:vlasnikID" request (delete-vlasnik request)))