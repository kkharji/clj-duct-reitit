(ns conduit.spec
  "Namespace to register and validate request/response based on
  https://github.com/gothinkster/realworld/blob/main/api/openapi.yml
  Using malli"
  (:require [malli.core :as m]
            [malli.registry :as mr]
            [malli.util :as mu]
            [conduit.spec.util :refer [schemas-from-feilds optional-keys closed-schema]]
            [duct.reitit.util :refer [spy]]))

(def ^:private fields
  {;; User Schema
   :user/username            :string
   :user/email               :string
   :user/password            :string
   :user/bio                 :string
   :user/token               :string
   :user/image               :string

   ;; Profile Schema
   :profile/bio              :user/bio
   :profile/following        :boolean
   :profile/username         :user/username
   :profile/image            :user/image

   ;; Article Schema
   :article/slug             :string
   :article/title            :string
   :article/description      :string
   :article/body             :string
   :article/author           :schema/profile
   :article/tag-list         [:vector :string]
   :article/created-at       :string
   :article/updated-at       :string
   :article/favorited        :boolean
   :article/favorites-count  :int

   ;; Comment Schema
   :comment/id               :int
   :comment/author           :schema/profile
   :comment/body             :string
   :comment/created-at       :string
   :comment/updated-at       :string})

(def ^:private schema
  (let [{:keys [comment article profile user]} (schemas-from-feilds fields)]
    {:schema/user    (-> user    optional-keys closed-schema)
     :schema/comment (-> comment optional-keys closed-schema)
     :schema/article (-> article optional-keys closed-schema)
     :schema/profile (-> profile optional-keys closed-schema)}))

(def ^:private actions
  {:actions.user/update :schema/user
   :actions.user/login    [:map
                           [:email :user/email]
                           [:password :user/password]]
   :actions.user/register [:map
                           [:email :user/email]
                           [:username :user/username]
                           [:password :user/password]]
   :actions.article/create [:map
                            [:body :article/body]
                            [:description :article/description]
                            [:title :article/title]
                            [:tag-list {:optional true} :article/tag-list]]
   :actions.article/update [:map
                            [:title {:optional true} :article/title]
                            [:body {:optional true} :article/body]
                            [:description {:optional true} :article/description]]})

(def ^:private requests
  {:request.user/register   [:map [:user :actions.user/register]]
   :request.user/update     [:map [:user :actions.user/update]]
   :request.user/login      [:map [:user :actions.user/login]]
   :request.article/create  [:map [:article :actions.article/create]]
   :request.article/update  [:map [:article :actions.article/update]]})

(def ^:private responses
  {:response/article       [:map [:article :schema/article]]
   :response/articles      [:map [:articles [:vec :schema/article] [:count :int]]]
   :response/user          [:map [:user :schema/user]]
   :response/comment       [:map [:comment :schema/comment]]
   :response/comments      [:map [:articles [:vec :schema/comment]]]})

(mr/set-default-registry!
 (merge (m/default-schemas) (mu/schemas) fields schema actions requests responses))

(comment
  (m/validate
   :request.user/login
   {:user {:email "tami5@gmail.com" :password "1234566"}})

  (m/validate
   :request.user/update
   {:user {:email "tami5@gmail.com" :password "1234566"}})

  (not (m/validate
        :request.user/update
        {:user {:not-field "tami5@gmail.com" :password "1234566"}})))

