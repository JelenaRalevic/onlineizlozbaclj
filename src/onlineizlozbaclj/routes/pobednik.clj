(ns onlineizlozbaclj.routes.pobednik
  (:require [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]
            [onlineizlozbaclj.models.baza :as db]
            [compojure.response :refer [render]]
            [buddy.auth :refer [authenticated?]]
            [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [struct.core :as st]
            [clojure.java.io :as io]
            [liberator.representation :refer [ring-response as-response]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [ring.util.response :refer [redirect]]))

(def pobednik-schema
  {:userID [st/required st/number]
   :pasID [st/required st/number]})

(defn pas-validation? [params]
  (st/valid? {:userID (read-string (:userID params))
              :pasID (read-string (:pasID params))} pobednik-schema))
  
(defn get-pobednici-page [page session]
  (render-file page
               {:title "Pobednici"
                :logged (:identity session)
                :pobednici (db/get-pobednik)}))

(defn pobednici [session]
    (get-pobednici-page "views/pobednici.html" session))

(defn add-pobednik-to-db [pobednik]
  (-> (db/add-pobednik pobednik)))


(defn pobednik-page-submit [{:keys [params session]}]
    (add-pobednik-to-db params)
        (redirect "/psi"))


(defroutes pobednik-routes
  (GET "/pobednici" request (pobednici (:session request)))
  (POST "/psi" request (pobednik-page-submit request)))   
  
