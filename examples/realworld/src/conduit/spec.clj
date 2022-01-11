(ns conduit.spec
  "Namespace to register and validate request/response based on
  https://github.com/gothinkster/realworld/blob/main/api/openapi.yml
  Using malli"
  (:require [malli.core :as m]
            [malli.registry :as mr]
            [malli.util :as mu]
            [conduit.spec.util :refer [schemas-from-feilds optional-keys closed-schema]]))

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

(def ^:private actions
  [;; User-Specific actions fields
   #:actions.user
    {:update    :schema/user
     :login     [:map
                 [:email :user/email]
                 [:password :user/password]]
     :register  [:map
                 [:email :user/email]
                 [:username :user/username]
                 [:password :user/password]]}

  ;; Article-Specific actions fields
   #:actions.article
    {:create  [:map
               [:body :article/body]
               [:description :article/description]
               [:title :article/title]
               [:tag-list {:optional true} :article/tag-list]]
     :update  [:map
               [:title {:optional true} :article/title]
               [:body {:optional true} :article/body]
               [:description {:optional true} :article/description]]}

  ;; Comment-Specific actions fields
   #:actions.comment
    {:create [:map [:body :comment/body]]}])

(def ^:private requests
  [;; User-Specific Requests
   #:request.user
    {:register   [:map [:user    :actions.user/register]]
     :update     [:map [:user    :actions.user/update]]
     :login      [:map [:user    :actions.user/login]]}

   ;; Article-Specific Requests
   #:request.article
    {:create     [:map [:article :actions.article/create]]
     :update     [:map [:article :actions.article/update]]}

   ;; Comment-Specific Requests
   #:request.comment
    {:create    [:map [:comment :actions.comment/create]]}])

(def ^:private responses
  #:response
   {:article        [:map [:article  :schema/article]]
    :articles       [:map [:articles [:vec :schema/article] [:count :int]]]
    :user           [:map [:user     :schema/user]]
    :comment        [:map [:comment  :schema/comment]]
    :comments       [:map [:comments [:vec :schema/comment]]]
    :tags           [:map [:tags [:vec :string]]]})

(def ^:private schema
  (let [{:keys [comment article profile user]} (schemas-from-feilds fields)]
    #:schema
     {:user    (-> user    optional-keys closed-schema)
      :comment (-> comment optional-keys closed-schema)
      :article (-> article optional-keys closed-schema)
      :profile (-> profile optional-keys closed-schema)}))

(mr/set-default-registry!
 (merge (m/default-schemas) (mu/schemas) fields schema (apply merge actions) (apply merge requests) responses))
