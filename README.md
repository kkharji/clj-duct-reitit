# Duct module.reitit


A [Duct][] module that sets [reitit][] as the application router with ring as a handler.

[duct]: https://github.com/duct-framework/duct
[reitit]: https://github.com/metosin/reitit

## Installation

To install, add the following to your project `:dependencies`:

    [duct/module.reitit "0.1.0"]

## Usage

### Basic

To add this module to your configuration, add the `:duct.module/reitit` key.

Full configuration demo:
```edn
{:duct.module/reitit {}
   :duct.module/logging {:set-root-config? true}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database            [{:author "tami5"}]
    :foo/index-path          "resources/index.html"
    :foo.handler/exceptions  {}

    :duct.logger/timbre      {:set-root-config? true :level :trace}

    :duct.reitit/routes     [["/" :index]
                             ["/author" :get-author]
                             ["/ping" {:get {:handler :ping}}]
                             ["/plus" {:post :plus/with-body
                                       :get 'plus/with-query}]
                             ["/divide" {:get :divide}]]

    ;; Registry to find handlers and local and global middleware
    :duct.reitit/registry  {:index {:path  (ig/ref :foo/index-path)} ;; init foo.handler/index with {:path}
                            :ping  {:message "pong"} ;; init foo.handler/ping with {:message}
                            :plus/with-body {} ;; init foo.handler.plus/with-body
                            :get-author {} ;; init foo.handler/get-author
                            :divide {}} ;; init foo.handler/divide

    ;; Logger to be used in reitit module.
    :duct.reitit/logger      #ig/ref :duct/logger

    ;; Whether to use muuntaja for formatting. default true, can be a modified instance of muuntaja.
    :duct.reitit/muuntaja   true

    ;; Keywords to be injected in requests for convenience.
    :duct.reitit/environment  {:db #ig/ref :foo/database}

    ;; Global middleware to be injected. expected registry key only
    :duct.reitit/middleware   []

    ;; Exception handling configuration
    :duct.reitit/exception  {:handlers #ig/ref :foo.handler/exceptions
                             :log? true ;; default true.
                             :pretty? true} ;; default in dev.

    ;; Coercion configuration
    :duct.reitit/coercion   {:enable true
                             :coercer 'spec ; Coercer to be used
                             :pretty? true  ; Whether to pretty print coercion errors
                             :formater nil} ; Function that takes spec validation error map and format it

    ;; Cross-origin configuration, the following defaults in for dev profile
    :duct.reitit/cross-origin {:origin [#".*"] ;; What origin to allow.
                               :methods [:get :post :delete :options]}}}
```

### Configuration Keys

#### `:duct.reitit/routes`

See the [reitit syntax][] for more information. Keywords within the routes are
replaced later with matching registry key or a valid symbol. like with
`plus/with-query` function symbol.

[reitit syntax]: https://cljdoc.org/d/metosin/reitit/0.5.5/doc/basics/route-syntax

#### `:duct.reitit/registry`

A map of handler and middleware keys and their integrant initialization
arguments.

Reitit module will take care of injecting it in duct configuration,
without requiring the user to define them outside the registry.

```javascript
<project-ns>.<handler-ns>[.<result key namespace>]/<result key name>
```

#### `:duct.reitit/logger`

Logger to be used in logging stuff. e.g. `duct/logger`. default nil

#### `:duct.reitit/muuntaja `

Muuntaja for formatting request and responses.

if provided value is boolean true then the default muuntaja instance will be used,
otherwise an instance can be set. default true

#### `:duct.reitit/environment`

Keys to be injected to be available in reitit handlers for convenience. Default empty map.

####  `:duct.reitit/middleware`

Middleware to be injected in reitit `{:data {:middleware []}` along with
other default configurable ones like `:duct.reitit/coercion` and `:duct.reitit/exception`.

#### `:duct.reitit/coercion`

coercion configuration, default nil.

- `:coercer`: either 'malli 'spec 'schema or a value for custom coercor. default nil
- `:pretty?` whether to pretty print coercion spec errors. default in dev
- `:formater` custom function to format the return body. default nil

#### `:duct.reitit/exception`

Exception Handling configuration

- `:handlers`: basic wrapper around [ring-reitit-exception-middleware].
  It expects a map of exception classes or
  `reitit.ring.middleware.exception` keys like wrap or default, and a
  function that takes `[exception request]`.
- `:log?` whether to log exceptions. default true. If
  `duct.reitit/logger`, then it will be used to log exceptions,
  otherwise it would use pretty print.
- `:pretty?` whether to make log exceptions easier to read.

[ring-reitit-exception-middleware]: https://cljdoc.org/d/metosin/reitit/0.5.15/doc/ring/exception-handling-with-ring#exceptioncreate-exception-middleware

#### `duct.reitit/cross-origin`

Cross-origin resource sharing configuration, In development, the origin
will always be a wildcard as the example above.
valid keys: `:headers, :origin, :methods`

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
