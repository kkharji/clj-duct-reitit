# 🎉 [0.3.0](https://github.com/tami5/clj-duct-reitit/tree/0.3.0) - 2022-01-09


### Dev


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/663a8de930f1322f80cce4320fff53f348947339"><tt>663a8de</tt></a> ✨ Feature: Pretty print exceptions</summary><br />Wow I didn't except such an improvement
  </details></dd></dl>

### General


- <a href="https://github.com/tami5/clj-duct-reitit/commit/6a8e8afc93157b1db44a82e8a4ba334dd21c03b8"><tt>6a8e8af</tt></a> ♻️ Refactor: Duct module structure
### Logging


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/4a693a1bbd06ba65bfbae5a13c3a88a7bbd3643e"><tt>4a693a1</tt></a> ♻️ Refactor: Total rewrite</summary><br />Unlike before where the user needs to specify a list of stuff to log,
now each item need to be set to boolean individually. Not totally sure
I'd keep this but at least tests are passing.
  </details></dd></dl>


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/e6772edb20af1babf4c71630336c9e34d453d19b"><tt>e6772ed</tt></a> ✨ Feature: Merge logging config into one submodule</summary><br />Mostly refactoring to make all logging configuration handled in a single
submodule.<br /><br /><b>BREAKING</b>: Change in how coercion and exception logging is
handled. Now to enable logging for exception or coercion, it should be
passed in `#duct.reitit/logging{:types []}`
  </details></dd></dl>


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/5bd4d6bdac76583bdee0e22b1b8e43093b8570da"><tt>5bd4d6b</tt></a> 🌱 Enhancement: Skip logging with coercion errors</summary><br />With in coercion handler there is logging. However, this feels wrong
because logging shouldn't be done there.
  </details></dd></dl>

### Module


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/398ae33c103b14b8c255f6cd5024e9877dc3fdd3"><tt>398ae33</tt></a> ♻️ Refactor: Total rewrite and decoupling</summary><br />- rename main router function key to `duct.reitit/router` instead of
  `duct.router/reitit`.
- rename main handler function key to `duct.reitit/handler` instead of
  `duct.handler/root`.
- create separate initializer for `duct.reitit/routes`. It seems to go
  along the lines of decoupling processing steps.
- refactor `duct.module/reitit` and make more readable and easy to
  reason with.
- move default config along with development and production profile
  mutations to `duct/reitit/defaults.clj`.
- refactor reitit module initializer logic to somewhat general purpose
  module initializer.

  ~~~clojure
  (module/init
         {:root  :duct.reitit
          :config config
          :extra [(registry-tree registry)]
          :store  {:namespaces namespaces :routes routes}
          :schema {::registry (registry-references registry)
                   ::routes   [:routes :namespaces ::registry]
                   ::router   [::routes ::options ::log]
                   ::log      ::options
                   ::handler  [::router ::options ::log]}})
  ~~~
  This make create modules similar duct.reitit easier.
  TODO: move to external library.
- change tests to reflect new changes
- remove many redundant files.
  </details></dd></dl>

### Readme


- <a href="https://github.com/tami5/clj-duct-reitit/commit/80c62d7463d49743f229b436d9c8437abd9566d1"><tt>80c62d7</tt></a> 📚 Documentation: Update
### Tests


- <a href="https://github.com/tami5/clj-duct-reitit/commit/daa0a83d15f68a3a7d13cca6789b44bfb9786c90"><tt>daa0a83</tt></a> ♻️ Refactor: Clean up tests


# 🎉 [0.2.0](https://github.com/tami5/clj-duct-reitit/tree/0.2.0) - 2022-01-03


### Exceptions


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/0c491bca137461ef69f218de920d863d8ced70a9"><tt>0c491bc</tt></a> ✨ Feature: Response customization based on class or `:type`</summary><br />To further understand how this work. checkout https://cljdoc.org/d/metosin/reitit/0.5.15/doc/ring/exception-handling-with-ring#exceptioncreate-exception-middleware
  </details></dd></dl>

<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/9e9a021df255530083e1eab499ab4f5196f64d29"><tt>9e9a021</tt></a> ✨ Feature: Log exceptions + pretty print.</summary><br />Example output with `pretty?`

~~~clj
; (err) ERROR [duct.reitit.middleware.exception:52] -
; (err)
; (err) {:message "Divide by zero",
; (err)  :uri "/divide",
; (err)  :method :get,
; (err)  :params {:body {:y 0, :x 0}},
; (err)  :trace
; (err)  [{:file-name "Numbers.java", :line-number 188}
; (err)   {:file-name "handler.clj", :line-number 17}
; (err)   {:file-name "exception.clj", :line-number 49}
; (err)   {:file-name "middleware.clj", :line-number 73}
; (err)   {:file-name "middleware.clj", :line-number 12}]}
; (err)
~~~
  </details></dd></dl>

<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/f966f834fa710e930adf8a5a3316020ce66ce653"><tt>f966f83</tt></a> ✨ Feature: Optional duct/logger</summary><br />if no logger provided in options, just use pretty print for logging
  </details></dd></dl>

### General


- <a href="https://github.com/tami5/clj-duct-reitit/commit/833195483b5bffd2b3979f0fe695c1c12f1f26d9"><tt>8331954</tt></a> 👷 Misc: Update outdated keys
### Middleware


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/d8d1d2feaca7c62f8e2f1a319855dda8c40ce4f7"><tt>d8d1d2f</tt></a> ✨ Feature: Inject keys instead of injecting `environment`</summary><br />This was originally the intended behavior. but it was ignored in last
release.
  </details></dd></dl>

### Module


- <a href="https://github.com/tami5/clj-duct-reitit/commit/6f0cf240f34f5a0b70982d9c73949ae31c76564f"><tt>6f0cf24</tt></a> ✅ Test: Detailed tests for configuration processing

- <a href="https://github.com/tami5/clj-duct-reitit/commit/fb9f033571559c07df2e0f2caac928f401704399"><tt>fb9f033</tt></a> ✨ Feature: Apply defaults + change schema

- <a href="https://github.com/tami5/clj-duct-reitit/commit/74eacfd309e2ec7f1999e2dbd6e373b0b3a381e1"><tt>74eacfd</tt></a> 📚 Documentation: Update description and spec in readme.
### Readme


- <a href="https://github.com/tami5/clj-duct-reitit/commit/e69b1af17bd8b1ee244acda9185b0a919169458e"><tt>e69b1af</tt></a> 👷 Misc: Format configuration example


# 🎉 [0.1.0](https://github.com/tami5/clj-duct-reitit/tree/0.1.0) - 2022-01-02


### General


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/b48a45b5b967db7fd8e197861da0a4c6d8b09952"><tt>b48a45b</tt></a> ♻️ Refactor: Move files and change configuration keys</summary><br /><br /><b>BREAKING</b>: rename `:duct.module.reitit/key` => `:duct.reitit/key`
  </details></dd></dl>


- <a href="https://github.com/tami5/clj-duct-reitit/commit/dbbb99ea3af718a5175321ce3ad442d73d7724c9"><tt>dbbb99e</tt></a> ✅ Test: General update
### Handler


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/c610a773a6136556f8a192aec684a88355638889"><tt>c610a77</tt></a> ✨ Feature: Add ring-handler</summary><br />Following module https://github.com/duct-framework/module.web.

Everything will come done to duct.handler/root
  </details></dd></dl>

### Middleware


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/10f44a3a47941b2e4507d3edec31372c6510ccdc"><tt>10f44a3</tt></a> ♻️ Refactor: Create macro for defining middlewares</summary><br />a macro to abstract the complexity of creating reitit middleware.
  </details></dd></dl>

<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/9e8a2214817e70e0875fe7b18287339f58df60de"><tt>9e8a221</tt></a> ♻️ Refactor: Move exception handling</summary><br />Create a new file under reitit.middleware to process and create
exception middleware.
  </details></dd></dl>


- <a href="https://github.com/tami5/clj-duct-reitit/commit/d80f2b460ee7a3aa60ec64253be3c271facf7d3a"><tt>d80f2b4</tt></a> ✅ Test: Coercion
- <a href="https://github.com/tami5/clj-duct-reitit/commit/f46c883b2a0eacadfac1f1aa64946f08098ba32f"><tt>f46c883</tt></a> ✅ Test: Parameters validation

- <a href="https://github.com/tami5/clj-duct-reitit/commit/f2406d02710c78cfc463a24ad05b8141960f21d7"><tt>f2406d0</tt></a> ✨ Feature: Access environment key within handlers
- <a href="https://github.com/tami5/clj-duct-reitit/commit/eb7eaa0ea5e86605285aa83ca6b8c03e56990c6f"><tt>eb7eaa0</tt></a> ✨ Feature: Pretty print coercion errors.
### Module


<dl><dd><details><summary><a href="https://github.com/tami5/clj-duct-reitit/commit/2bbc180150e2b51192f1d4b5f3d50da16ffa1181"><tt>2bbc180</tt></a> ♻️ Refactor: Change require configuration schema.</summary><br />It's a bit weird how duct works, I don't like the fact that modules are
outside the base profile. Here I took the same approach as existing
duct modules and kept module initialization with empty map.
  </details></dd></dl>

### Router


- <a href="https://github.com/tami5/clj-duct-reitit/commit/6bc18676f013391801607b66ea0605fa30ca43f0"><tt>6bc1867</tt></a> ♻️ Refactor: Decouple middleware processing

- <a href="https://github.com/tami5/clj-duct-reitit/commit/3fe33fb730885871a7ececc13af9b9eb304046ca"><tt>3fe33fb</tt></a> ✨ Feature: Implement router key


# 🎉 [0.0.1](https://github.com/tami5/clj-duct-reitit/tree/0.0.1) - 2021-12-29


### Dev


- <a href="https://github.com/tami5/clj-duct-reitit/commit/dbddfa9ad5d533525c94f9e95bb7795bccf304d8"><tt>dbddfa9</tt></a> ✨ Feature: Add clj-dev
### Module


- <a href="https://github.com/tami5/clj-duct-reitit/commit/87afe9fec9a2ac6651a5f713c31745f00a4fac07"><tt>87afe9f</tt></a> ♻️ Refactor: Cleanup & readability

- <a href="https://github.com/tami5/clj-duct-reitit/commit/3abcd06d0dd65597bc0c53c6d5c077ed5b2a8811"><tt>3abcd06</tt></a> ✅ Test: Read module configuration

- <a href="https://github.com/tami5/clj-duct-reitit/commit/829cbaae4290c0de9d4dd51fd6ead1efdac530a6"><tt>829cbaa</tt></a> ✨ Feature: Process registry and merge to config
- <a href="https://github.com/tami5/clj-duct-reitit/commit/7c74c8be0e6ef9e2a59e08e03444cc67298a8bad"><tt>7c74c8b</tt></a> ✨ Feature: Use module.registry for passing functions
### Readme


- <a href="https://github.com/tami5/clj-duct-reitit/commit/df77cb073e8ed312bc59525d702866704e8cdd54"><tt>df77cb0</tt></a> 📚 Documentation: Explain how duct.module/reitit should work
- <a href="https://github.com/tami5/clj-duct-reitit/commit/4f1a09ae045d0ee652aed13156c5b074f6bbe6f1"><tt>4f1a09a</tt></a> 📚 Documentation: Rename reitit/options -> reitit/opts
- <a href="https://github.com/tami5/clj-duct-reitit/commit/9cec7b8d8a9e0fd5457f9587781ce869bb742362"><tt>9cec7b8</tt></a> 📚 Documentation: Introduce malli as coercion option


