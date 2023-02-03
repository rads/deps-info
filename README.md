# deps-info

Functions to get info about `tools.deps` coordinates.

## `rads.deps-info.infer`

Infer `tools.deps` coordinates from a lib name, a partial set of coordinates, or both.

```
$ bbin install io.github.rads/deps-info
{:lib io.github.rads/deps-info,
 :coords
 {:git/url "https://github.com/rads/deps-info",
  :git/tag "v0.0.1",
  :git/sha "cf7a85377fd05070135c0c5dcf606793de5d0560"}}
Cloning: https://github.com/rads/deps-info
Checking out: https://github.com/rads/deps-info at cf7a85377fd05070135c0c5dcf606793de5d0560

$ deps-info-infer --lib io.github.rads/deps-info
#:io.github.rads{deps-info #:git{:url "https://github.com/rads/deps-info", :tag "v0.0.1", :sha "cf7a85377fd05070135c0c5dcf606793de5d0560"}}

$ deps-info-infer --lib io.github.rads/deps-info --latest-sha
#:io.github.rads{deps-info #:git{:url "https://github.com/rads/deps-info", :sha "cf7a85377fd05070135c0c5dcf606793de5d0560"}}
```

This namespace can be be added to a `deps.edn` project as a standalone dependency using `:deps/root`:

```
{:deps {io.github.rads/deps-info {:git/tag "..." :git/sha "..."
                                  :deps/root "infer"}}}
```

## `rads.deps-info.summary`

Get procurer and artifact info for coordinates.

```clojure
(require '[rads.deps-info.summary :as summary])
(summary/summary {:script/lib "io.github.rads/deps-info"})
```
