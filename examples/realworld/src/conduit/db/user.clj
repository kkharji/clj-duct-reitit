(ns conduit.db.user
  (:require [duct.database.sql]
            [buddy.hashers :as hashers]
            [conduit.db :refer [execute-one!]]))

(defprotocol User
  (register [spec user])
  (login    [spec user]))

(extend-protocol User
  duct.database.sql.Boundary

  (register [spec user]
    (let [keys [:username :email :bio :image :password]
          user (update (select-keys user keys) :password hashers/encrypt)
          query {:insert-into :users :values [user]}]
      (execute-one! query spec)))

  (login [spec {:keys [email password]}]
    (let [query {:select [:*] :from [:users] :where [:= :email email]}
          user (execute-one! query spec)]
      (when (and user (hashers/check password (:password user)))
        (dissoc user :password)))))
