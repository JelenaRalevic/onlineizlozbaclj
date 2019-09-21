(ns onlineizlozbaclj.routes.home  
  (:require [compojure.core :refer :all]
            [onlineizlozbaclj.models.baza :as db]
            [selmer.parser :refer [render-file]]
            [compojure.response :refer [render]]
            [selmer.parser :refer [render-file]]
            [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [struct.core :as st]
            [clojure.java.io :as io]
            [liberator.representation :refer [ring-response as-response]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [ring.util.response :refer [redirect]]))

(def pas-schema
  {:ime [st/required st/string]
   :boja [st/required st/string]
   :rasa [st/required st/string]
   :sampion [st/required st/string]
	 :slika [st/required st/string]
	 :opis [st/required st/string]
   :vlasnikID [st/required st/number]
})

(defn pas-validation? [params]
  (st/valid? {:ime (:ime params)
              :boja (:boja params)
              :rasa (:rasa params)
              :sampion (:sampion params)
              :slika (:slika params)
              :opis (:opis params)
              :vlasnikID (read-string (:vlasnikID params))} pas-schema))

(defn get-home-page [page session]
  (render-file page
               {:title "Home"
                :vlasnici (count (db/get-vlasnik))
                :psi (count (db/get-pas))
                :izlozbe (count (db/get-izlozbe))}))

(defn home-page [session]
  (get-home-page "views/home.html" session))

(defn send-submit [{:keys [params session]}]
  
  (get-home-page "views/home.html" session))

(defn get-psi [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-pas)
    (db/search-pas text)))

(defn get-search-psi [params session]
    (render-file "views/search-pas-home.html" {:title "Home page"
	                                              :psi (get-psi nil)}))

(defresource search-pas-home [{:keys [params session]}]
  :allowed-methods [:post]
  :handle-created (json/write-str (get-psi (:text params)))
  :available-media-types ["application/json"])

(defresource search-pas-home [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-psi params session)
                  "application/json" (->(:text params)
                                        (get-psi)
                                        (json/write-str)))))
(defn get-izlozbe [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-izlozbe)
    (db/search-izlozbe text)))

(defn get-search-izlozbe [params session]
  (render-file "views/search-izlozbe-home.html" {:title "Home page"
                                                  :izlozbe (get-izlozbe nil)}))

(defresource search-izlozbe-home [{:keys [params session]}]
  :allowed-methods [:post]
  :handle-created (json/write-str (get-izlozbe (:text params)))
  :available-media-types ["application/json"])

(defresource search-izlozbe-home [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-izlozbe params session)
                  "application/json" (->(:text params)
                                        (get-izlozbe)
                                        (json/write-str)))))


(defroutes home-routes
           (GET "/" request (home-page (:session request)))
           (POST "/" request (send-submit request))
           (GET "/pretragahome" request (search-pas-home request))
           (GET "/pretragaizlozbihome" request (search-izlozbe-home request)))





