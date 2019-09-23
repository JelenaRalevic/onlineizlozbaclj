(ns onlineizlozbaclj.models.baza
(:require [clojure.java.jdbc :as sql]
            [korma.core :as k]
            [korma.db :refer [defdb mysql]]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.edn :as edn]
            )
  (:import java.sql.DriverManager))

(def db-config(edn/read-string (slurp "config.edn")))

(defdb db (mysql db-config))
  
(k/defentity user
  (k/table :user))

(k/defentity vlasnik
  (k/table :vlasnik))
  
 (defn add-user [params]
  (k/insert user
  (k/values params)))

(defn delete-user [id]
  (k/delete user
  (k/where {:userID id})))

(defn find-user [params]
  (k/select user
            (k/where params)))

(defn find-user-by-id [params]
  (k/select user
            (k/where params)))

(defn get-users []
  (k/select user
  (k/where {:rola "user"})))

(defn update-user [params]
  (k/update user
            (k/set-fields params)
            (k/where {:id (:id params)})))
(defn search-user [text]
  (k/select user
            (k/where (or
                       {:userID text}
                       {:imePrezime text}
                       {:username text}
                       {:password text}
                       {:email text}
                       {:rola text}))
            (k/order :userID :ASC)))


(k/defentity pas
  (k/table :pas))

(defn get-pas []
  (k/select pas
          (k/fields :* [:vlasnik.imePrezime :vime])
          (k/join vlasnik (= :pas.vlasnikID :vlasnik.vlasnikID))))

(defn search-pas [text]
  (k/select pas
            (k/fields :* [:vlasnik.imePrezime :vime])
            (k/join vlasnik (= :pas.vlasnikID :vlasnik.vlasnikID) )
            (k/where (or
                       {:pasID text}
                       {:ime text}
                       {:boja text}
                       {:rasa text}
                       {:sampion text}
                       {:vlasnik.imePrezime text}))
            (k/order :pasID :ASC)))

(defn add-pas [params]
  (k/insert pas
  (k/values params)))

(defn delete-pas [id]
  (k/delete pas
  (k/where {:pasID id})))

(defn delete-pas-vlasnik [id]
  (k/delete pas
  (k/where {:vlasnikID id})))

(defn find-pas [params]
  (k/select pas
          (k/fields :* [:vlasnik.imePrezime :vime])
          (k/join vlasnik (= :pas.vlasnikID :vlasnik.vlasnikID))
          (k/where params)))


(defn find-pas-by-id [id]
  (k/select pas
          (k/fields :* [:vlasnik.imePrezime :vime])
          (k/join vlasnik (= :pas.vlasnikID :vlasnik.vlasnikID))
          (k/where {:pasID id})))

(defn update-pas [params]
  (k/update pas
            (k/set-fields params)
            (k/where {:pasID (:pasID params)})))

(k/defentity izlozba
  (k/table :izlozba))

(defn get-izlozbe[]
  (k/select izlozba
          (k/fields :* [:vlasnik.imePrezime :vime])
          (k/join vlasnik (= :izlozba.vlasnikID :vlasnik.vlasnikID))))

(defn add-izlozba[params]
  (k/insert izlozba
  (k/values params)))

(defn delete-izlozba [izlozbaID]  
  (k/delete izlozba
  (k/where {:izlozbaID izlozbaID})))

(defn delete-izlozba-vlasnik [vlasnik]
  (k/delete izlozba
  (k/where {:vlasnikID vlasnik})))

(defn find-izlozba [params]
  (k/select izlozba
          (k/fields :* [:vlasnik.imePrezime :vime])
          (k/join vlasnik (= :izlozba.vlasnikID :vlasnik.vlasnikID))
          (k/where params)))


(defn get-text-search [text]
  (str "%" text "%"))

(defn search-izlozbe [text]
  (k/select izlozba
            (k/fields :* [:vlasnik.imePrezime :vime])
            (k/join vlasnik (= :izlozba.vlasnikID :vlasnik.vlasnikID))
            (k/where (or
                       {:izlozbaID text}
                       {:tip text}
                       {:naziv text}
                       {:grad text}                       
                       {:vlasnik.imePrezime [like (get-text-search text)]}))
            (k/order :izlozbaID :ASC)))

(defn update-izlozba [params]
  (k/update izlozba
            (k/set-fields params)
            (k/where {:izlozbaID (:izlozbaID params)})))

(k/defentity pobednik
  (k/table :pobednik))

(defn get-pobednik []
   (k/select pobednik
          (k/fields :* [:pas.ime :pime])
          (k/join pas (= :pobednik.pasID :pas.pasID))))

(defn find-pobednik-by-user [id]
  (k/select pobednik
          (k/fields :* [:pas.ime :pime])
          (k/join pas (= :pobednik.pasID :pas.pasID))
          (k/where {:userID id})))

(defn find-pobednik-by-pas [id]
  (k/select pobednik
          (k/fields :* [:pas.ime :pime])
      (k/join pas (= :pobednik.pasID :pas.pasID))
      (k/where {:pasID id})))

(defn find-pobednik [params]
  (k/select pobednik
          (k/fields :* [:pas.ime :pime])
          (k/join pas (= :pobednik.pasID :pas.pasID))
          (k/where params)))

(defn add-pobednik [params]
  (k/insert pobednik
  (k/values params)))

(defn delete-pobednik [id]
  (k/delete pobednik
  (k/where {:pobednikID id})))

(defn delete-pobednik-pas [pas]
  (k/delete pobednik
  (k/where {:pasID pas})))

(defn delete-pobednik-user [user]
  (k/delete pobednik
  (k/where {:userID user})))

(defn get-vlasnik []
  (k/select vlasnik))

(defn search-vlasnik [text]
  (k/select vlasnik
            (k/where (or
                       {:vlasnikID text}
                       {:imePrezime text}
                       {:grad text}
                       {:email text}
                       {:username text}
                       {:password text}))
            (k/order :vlasnikID :ASC)))

(defn add-vlasnik [params]
  (k/insert vlasnik
  (k/values params)))

(defn delete-vlasnik [id]
  (k/delete vlasnik
  (k/where {:vlasnikID id})))

(defn find-vlasnik [params]
  (k/select vlasnik
            (k/where params)))

(defn find-vlasnik-by-id [params]
  (k/select vlasnik
            (k/where params)))
