(ns onlineizlozbaclj.routes.izlozba
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
             [clojure.edn :as edn]
            [ring.util.response :refer [redirect]]))

(def file-config (edn/read-string (slurp "file-config.edn")))

(defn create-file-name [{:keys [fname content-type]}]
  (str (:short-img-location file-config) fname "." (last (str/split content-type #"/"))))

(defn get-picture-url [params]
  (if (contains? params :url)
    (:url params)
    (->(assoc (:file params) :fname (:name params))
       (create-file-name))))

(def izlozba-schema
  {:tip [st/required st/string]
   :naziv [st/required st/string]
   :grad [st/required st/string]
   :vlasnikID [st/required st/number]})

(defn izlozba-validation? [params]
  (st/valid? {:tip (:tip params)
              :naziv (:naziv params)
              :grad (:grad params)
              :vlasnikID (:vlasnikID params)} izlozba-schema))

(defn get-izlozbe-page [page session]
  (render-file page
               {:title "izlozbe"
                :logged (:identity session)
                :izlozbe (db/get-izlozbe)}))

(defn authenticated-admin? [session]
  (and (authenticated? session)
       (="admin" (:rola (:identity session)))))

(defn upload-picture [{:keys [fname tempfile]}]
  (io/copy tempfile (io/file (:resources-folder file-config) fname)))

(defn izlozbe [session]  
   (cond
    (not (authenticated? session))
    (redirect "/login")
    (authenticated-admin? session)
    (render-file "views/izlozbe-search-admin.html" {:title "Prikaz izlozbi"
                                                     :logged (:identity session)
                                                     :izlozbe (db/get-izlozbe)})
    :else
    (get-izlozbe-page "views/izlozbe.html" session)))

(defn get-add-izlozba-page [session &[message]]
  (if-not (authenticated? session)
    (redirect "/vlogin")
    (render-file "views/izlozba-add.html" {:title "Dodaj izlozbu"
                                            :logged (:identity session)})))

(defn add-izlozba [{:keys [params session]}]
    (println params)
    (izlozba-validation? params)
    (db/add-izlozba params)
    (redirect "/vlasnikForma"))

(defn get-izlozbe [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-izlozbe)
    (db/search-izlozbe text)))

(defn get-search-izlozbe [params session]
  (render-file "views/izlozba-search-vl.html" {:title "Search izlozbe"
                                             :logged (:identity session)
                                             :izlozbe (get-izlozbe nil)}))

(defresource search-izlozba [{:keys [params session]}]
  :allowed-methods [:post]
  :authenticated? (authenticated? session)
  :handle-created (json/write-str (get-izlozbe (:text params)))
  :available-media-types ["application/json"])

(defresource search-izlozba [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-izlozbe params session)
                  "application/json" (->(:text params)
                                        (get-izlozbe)
                                        (json/write-str)))))

(defn get-izlozba-edit-page [page params session]
  (render-file page {:title "Izlozba"
                     :logged (:identity session)
                     :izlozba (first (db/find-izlozba params))}))

(defn get-izlozba [{:keys [params session]}]
   (if-not (authenticated? session)
    (redirect "/vlogin")
    (get-izlozba-edit-page "views/edit-izlozba.html" params session)))

(defresource update-izlozba [{:keys [params session]}]
  :allowed-methods [:put]  
  :available-media-types ["application/json"]
  (println params)
  (db/update-izlozba params))

(defresource delete-izlozba [{:keys [params session]}]
  :allowed-methods [:delete]  
  :available-media-types ["application/json"])

(defroutes izlozba-routes
  (GET "/izlozbe" request (izlozbe (:session request)))
  (GET "/addizlozba" request (get-add-izlozba-page (:session request)))
  (POST "/addizlozba" request (add-izlozba request))
  (GET "/pretragaizlozbi" request (search-izlozba request))
  (GET "/izlozba/:izlozbaID" request (get-izlozba request))
  (PUT "/izlozba" request (update-izlozba request))
  (DELETE "/izlozba" request (delete-izlozba request)))