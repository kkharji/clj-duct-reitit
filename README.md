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
{:duct.module/reitit
  {:routes   [["/" :index]
              ["/ping" {:get {:handler :pong}}]
              ["/plus" {:get plus/with-query
                        :post :plus/with-body}]]
   :registry [[:index {:path #ig/ref index-path}]
              [:ping {:message "pong"}]
              [:plus/with-body]}}

  :duct.profile/base
  {:duct.core/project-ns foo
   :duct.core/handler-ns handler ; default value
   :duct.core/middleware-ns middleware ; default value

   :foo/database {}
   :foo/index-path "public/index.html"

   :duct.module.reitit/cors ;; defaults in for dev and local environment
   {:origin [#".*"]
    :methods [:get :post :delete :options]}

   :duct.module.reitit/opts
   {:coercion data-spec ; default nil
    :environment {:db #ig/ref :foo/database} ; default nil
    :middlewares []}}} ; default empty
```

### Keys

#### `:duct.module/reitit`
- `routes` See the [reitit syntax][] for more information.
- `registry`: map of handler and middleware keys and their integrant initialization arguments.

[reitit syntax]: https://cljdoc.org/d/metosin/reitit/0.5.5/doc/basics/route-syntax

#### `:duct.module.reitit/cors`

cross-origin resource sharing settings, In development, the origin will always
be a wildcard as the example above. valid keys: `:headers, :origin, :methods`

#### `:duct.module.reitit/opts`

Extra reitit and ring options
  - `:muuntaja`: if boolean true then the default muuntaja instance will be
    used, otherwise the value of `:munntaja`.
  - `:coercion`: if this value is nil then coercion won't be used. valid keys `[:malli :spec :schema]`
  - `:environment`: environment variables to be passed to be injected to handlers.
  - `:middlewares`: global middlewares to be passed to reitit middleware key with the default once.

### Registry

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
