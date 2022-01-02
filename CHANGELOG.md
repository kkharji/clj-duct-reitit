# 🔥 [Unreleased](https://github.com/tami5/clj-duct-reitit)


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


