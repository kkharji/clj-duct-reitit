(ns conduit.spec
  "Namespace to register and validate request/response based on
  https://github.com/gothinkster/realworld/blob/main/api/openapi.yml
  Using clojure.spec"
  (:require [clojure.spec.alpha :as s]))

;; User Resource, Request and Response.
(s/def :user/username string?)
(s/def :user/email string?)
(s/def :user/password string?)
(s/def :user/bio string?)
(s/def :user/token string?)
(s/def :user/image string?)

(s/def ::user
  (s/keys :req-un
          [:user/email
           :user/bio
           :user/password
           :user/username
           :user/image
           :user/token]))

(s/def :user/login
  (s/keys :req-un [:user/email :user/password]))

(s/def :user/new
  (s/keys :req-un [:user/email :user/password :user/username]))

#_(s/valid? :user/new   {:username "tami" :email "tami@gmail.com" :password "12345567"})
#_(s/valid? :user/login {:email "tami@gmail.com" :password "12345567"})
#_(s/valid? ::user      {:username "" :email "tami@gmail.com" :password "12345567" :image "" :token ""})

;; Profile Resource, Request and Response.
(s/def :profile/bio :user/bio)
(s/def :profile/following boolean?)
(s/def :profile/username :user/username)
(s/def :profile/image :user/image)
(s/def ::profile
  (s/keys :req-un [:profile/bio :profile/following :profile/username :profile/image]))

;; Article Resource, Request and Response.
(s/def :article/slug string?)
(s/def :article/title string?)
(s/def :article/description string?)
(s/def :article/body string?)
(s/def :article/author ::profile)
(s/def :article/tag-list (s/coll-of string? :kind vector))
(s/def :article/created-at string?)
(s/def :article/updated-at string?)
(s/def :article/favorited boolean?)
(s/def :article/favorites-count int?)
(s/def ::article
  (s/keys :req-un [:article/slug
                   :article/title
                   :article/description
                   :article/body
                   :article/author
                   :article/tag-list
                   :article/created-at
                   :article/updated-at
                   :article/favorited
                   :article/favorites-count]))

(s/def :articles/count int?)

(s/def :response/article
  (s/keys :req-un [::article]))

(s/def ::articles
  (s/coll-of ::article))

(s/def :article/new
  (s/keys :req-un [:article/title :article/body :article/description]
          :opt-un [:article/tag-list]))

(s/def :article/update
  (s/keys :opt-un [:article/title :article/description :article/body :article/tag-list]))

(s/def :response/article
  (s/keys :req-un [::article]))

(s/def :response/articles
  (s/keys :req-un [::articles :articles/count]))

(s/def :request/new-article :article/new)
(s/def :request/new-article :article/update)

;; Comment Resource, Request and Response.
(s/def :comment/id int?)
(s/def :comment/author ::profile)
(s/def :comment/body string?)
(s/def :comment/created-at string?)
(s/def :comment/updated-at string?)

(s/def ::comment
  (s/keys :opt-un [:comment/id :comment/author :comment/body :comment/created-at :comment/updated-at]))

(s/def ::comments
  (s/coll-of ::comment))

(s/def :response/comment
  (s/keys :req-un [::comment]))

(s/def :response/comments
  (s/keys :req-un [::comments]))

(s/def :comment/new
  (s/keys :req-un [:comment/body]))

(s/def :request/new-comment
  (s/keys :req-un [:comment/body]))
