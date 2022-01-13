(ns conduit.handler.user
  (:require [conduit.db.user :as user]
            [ring.util.response :as rs]
            [buddy.sign.jwt :as jwt]
            [clj-dev.utils :refer [spy]]))

(defn ^:private wrap-with-token [user jwt-secret]
  (->> (jwt/sign {:user-id (:id user)} jwt-secret)
       (assoc user :token)
       (hash-map :user)))

(defn login [{:keys [db jwt-secret parameters]}]
  (if-let [user (user/login db (get-in parameters [:body :user]))]
    (rs/response (wrap-with-token user jwt-secret))
    (rs/bad-request "Wrong email or password.")))

(defn register [{:keys [db jwt-secret parameters]}]
  (try (-> (user/register db (get-in parameters [:body :user]))
           (wrap-with-token jwt-secret)
           (rs/response))
       (catch Exception e
         (rs/bad-request (ex-message e)))))
