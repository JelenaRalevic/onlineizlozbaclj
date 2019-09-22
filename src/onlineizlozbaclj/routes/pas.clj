(ns onlineizlozbaclj.routes.pas
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

(defn get-pobednik [user]
  (db/find-pobednik-by-user (:userID user)))
(defn get-psi-page [page session]
  (render-file page
               {:title "Psi"
                :logged (:identity session)
                :psi (db/get-pas)
                :pobednici (get-pobednik (:identity session))}))

(defn upload-picture [{:keys [fname tempfile]}]
  (io/copy tempfile (io/file (:resources-folder file-config) fname)))

(defn get-pas-slika-from-db [params]
  (:slika (first (db/find-pas (select-keys params [:pasID])))))

(defn file-exists? [params]
  (.exists (clojure.java.io/as-file (str (:resources-folder file-config) (get-pas-slika-from-db params)))))


(defn psi [session]
    (get-psi-page "views/psi.html" session))

(defn get-psi [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-pas)
    (db/search-pas text)))

(defn get-psi-vl [text]
  (if (or (nil? text)
          (= "" text))
    (db/get-pas)
    (db/search-pas text)))

(defn authenticated-admin? [session]
  (and (authenticated? session)
       (="admin" (:rola (:identity session)))))

(defn get-search-psi [params session]
   (cond
    (not (authenticated? session))
    (redirect "/login")
    (authenticated-admin? session)
    (render-file "views/pas-search-admin.html" {:title "Prikaz pasa"
                                                 :logged (:identity session)
                                                 :psi (get-psi nil)})
    :else
    (render-file "views/pas-search.html" {:title "Search pas"
                                           :logged (:identity session)
                                           :psi (get-psi nil)})))

(defn get-search-psi-vl [params session]
  (render-file "views/pas-search-vl.html" {:title "Search pas"
                                             :logged (:identity session)
                                             :psi (get-psi-vl nil)}))

(defresource search-psi [{:keys [params session]}]
  :allowed-methods [:post]
  :handle-created (json/write-str (get-psi (:text params)))
  :available-media-types ["application/json"])

(defresource search-pas-vl [{:keys [params session]}]
  :allowed-methods [:post]
  :handle-created (json/write-str (get-psi-vl (:text params)))
  :available-media-types ["application/json"])

(defresource search-pas [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-psi params session)
                  "application/json" (->(:text params)
                                        (get-psi)
                                        (json/write-str)))))

(defresource search-pas-vl [{:keys [params session]}]
  :allowed-methods [:post]
  :authenticated? (authenticated? session)
  :handle-created (json/write-str (get-psi-vl (:text params)))
  :available-media-types ["application/json"])

(defresource search-pas-vl [{:keys [params session]}]
  :allowed-methods [:get]
  :available-media-types ["text/html" "application/json"]
  :handle-ok #(let [media-type (get-in % [:representation :media-type])]
                (condp = media-type
                  "text/html" (get-search-psi-vl params session)
                  "application/json" (->(:text params)
                                        (get-psi-vl)
                                        (json/write-str)))))

(defn get-add-pas-page [session &[message]]
  (if-not (authenticated? session)
    (redirect "/vlogin")
    (render-file "views/pas-add.html" {:title "Kreiraj psa"
                                        :logged (:identity session)})))

(defn add-pas [{:keys [params session]}]
    (pas-validation? params)
    (println params)
    (db/add-pas params)
    (redirect "/vlasnikForma"))

(defn get-pas-edit-page [page params session]
  (render-file page {:title "Pas"
                     :logged (:identity session)
                     :pas (first (db/find-pas params))}))

(defn get-pas [{:keys [params session]}]
   (if-not (authenticated? session)
    (redirect "/vlogin")
    (get-pas-edit-page "views/edit-pas.html" params session)))

(defresource update-pas [{:keys [params session]}]
  :allowed-methods [:put]  
  :available-media-types ["application/json"]
  (println params)
  (db/update-pas params))


(defresource delete-pas [{:keys [params session]}]
  :allowed-methods [:delete] 
  (db/delete-pobednik-pas (:pasID params))
  (db/delete-pas (:pasID params))
  :available-media-types ["application/json"])


(defroutes pas-routes
  (GET "/psi" request (psi (:session request)))  
  (GET "/pretraga" request (search-pas request))
  (GET "/pretragavl" request (search-pas-vl request))
  (GET "/addpas" request (get-add-pas-page (:session request)))
  (POST "/addpas" request (add-pas request))
  (GET "/pas/:pasID" request (get-pas request))
  (PUT "/pas" request (update-pas request))  
  (DELETE "/pas" request (delete-pas request)))



