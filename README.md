# Duct module.reitit


A [Duct][] module that sets [reitit][] as the application router with ring as a handler.

[duct]: https://github.com/duct-framework/duct
[reitit]: https://github.com/metosin/reitit

## Installation

To install, add the following to your project `:dependencies`:

    [duct/module.reitit "0.0.1"]

## Usage

### Basic

To add this module to your configuration, add the `:duct.module/reitit` key.

Full configuration demo:
```edn
{:duct.module/reitit {}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database [{:author "tami5"}]
    :foo/index-path "resources/index.html"

    :duct.reitit/routes
    [["/" :index]
     ["/author" :get-author]
     ["/ping" {:get {:handler :ping}}]
     ["/plus" {:post :plus/with-body
               :get 'plus/with-query}]]

    :duct.reitit/registry
    {:index {:path (ig/ref :foo/index-path)}
     :ping  {:message "pong"}
     :plus/with-body {}
     :get-author {}}

    :duct.reitit/options
    {:muuntaja true ; default true, can be a modified instance of muuntaja.
     :coercion ;; coercion configuration, default nil.
     {:coercer 'spec ; coercer to be used
      :pretty-coercion? true ; whether to pretty print coercion errors
      :error-formater nil} ; function that takes spec validation error map and format it
     :environment ;; Keywords to be injected in requests for convenience.
     {:db (ig/ref :foo/database)}
     :middleware [] ;; Global middleware to be injected. expected registry key only
     :cross-origin ;; cross-origin configuration, the following defaults in for dev and local profile
     {:origin [#".*"] ;; What origin to allow
      :methods [:get :post :delete :options]}}}}
```
### Keys

#### `:duct.reitit/routes`

See the [reitit syntax][] for more information. Keywords within the routes are
replaced later with matching registry key or a valid symbol. like with
`plus/with-query` function symbol.

[reitit syntax]: https://cljdoc.org/d/metosin/reitit/0.5.5/doc/basics/route-syntax

#### `:duct.reitit/registry`

A map of handler and middleware keys and their integrant initialization
arguments.

#### `:duct.reitit/options`

Extra reitit and ring options
  - `:muuntaja`: if boolean true then the default muuntaja instance will be
    used, otherwise the value of `:muuntaja`.
  - `:coercion`: coercion configuration, default nil.
    - `:coercer`: either 'malli 'spec 'schema or a value for custom coercor.
    - `:pretty-coercion?` whether to pretty print coercion spec errors
    - `:error-formater` custom function to format the return body.
  - `:environment`: environment variables to be injected to handlers.
  - `:middlewares`: global middleware to be passed to reitit middleware key with the default once.
  - `:cross-origin` cross-origin resource sharing configuration, In development, the origin
    will always be a wildcard as the example above. valid keys: `:headers, :origin, :methods`

### Overview

`duct.module/reitit` needs the following keys to resolve registry entries or inline symbols:

- `:duct.core/project-ns`
- `:duct.core/handler-ns`
- `:duct.core/middleware-ns`

Defaults `handler-ns = handler, middleware-ns = middleware`

```javascript
<project-ns>.<handler-ns>[.<result key namespace>]/<result key name>
```

#### Example

Project namespace is `foo`, the handler namespace is `handler` and registry key is `:index`

- Integrant key `:foo.handler/index`. If it's an integrant key integrant would be used to initialize it
- Function symbol `foo.handler/index`. if it's an existing symbol then the it's value would be used

If the result key was `:webiste/index` instead, then the Integrant key
would be `:foo.handler.website/index`. or symbol `foo.handler.website/index`

- Integrant key `:foo.handler/index`
- Function symbol `foo.handler/index`

middleware works in similar fashion.

## License

Copyright © 2022 tami5

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
