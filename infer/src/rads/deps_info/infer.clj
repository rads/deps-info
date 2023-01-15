(ns rads.deps-info.infer
  (:require [rads.deps-info.git :as git]
            [cheshire.core :as json]
            [clojure.tools.gitlibs.impl :as gitlibs-impl]
            [clojure.edn :as edn]
            [clojure.set :as set]))

(def lib-opts->template-deps-fn
  "A map to define valid CLI options.

  - Each key is a sequence of valid combinations of CLI opts.
  - Each value is a function which returns a tools.deps lib map."
  {[#{:local/root}]
   (fn [_ lib-sym lib-opts]
     {lib-sym (select-keys lib-opts [:local/root])})

   [#{} #{:git/url}]
   (fn [client lib-sym lib-opts]
     (let [url (or (:git/url lib-opts) (git/github-repo-ssh-url lib-sym))
           {:keys [name commit]} (git/latest-git-tag client url)]
       {lib-sym {:git/url url :git/tag name :git/sha (:sha commit)}}))

   [#{:git/tag} #{:git/url :git/tag}]
   (fn [client lib-sym lib-opts]
     (let [url (or (:git/url lib-opts) (git/github-repo-ssh-url lib-sym))
           tag (:git/tag lib-opts)
           {:keys [commit]} (git/find-git-tag client url tag)]
       {lib-sym {:git/url url :git/tag tag :git/sha (:sha commit)}}))

   [#{:git/sha} #{:git/url :git/sha}]
   (fn [_ lib-sym lib-opts]
     (let [url (or (:git/url lib-opts) (git/github-repo-ssh-url lib-sym))
           sha (:git/sha lib-opts)]
       {lib-sym {:git/url url :git/sha sha}}))

   [#{:latest-sha} #{:git/url :latest-sha}]
   (fn [client lib-sym lib-opts]
     (let [url (or (:git/url lib-opts) (git/github-repo-ssh-url lib-sym))
           sha (git/latest-git-sha client url)]
       {lib-sym {:git/url url :git/sha sha}}))

   [#{:git/url :git/tag :git/sha}]
   (fn [_ lib-sym lib-opts]
     {lib-sym (select-keys lib-opts [:git/url :git/tag :git/sha])})})

(def valid-lib-opts
  "The set of all valid combinations of CLI opts."
  (into #{} cat (keys lib-opts->template-deps-fn)))

(defn- cli-opts->lib-opts
  "Returns parsed lib opts from raw CLI opts."
  [cli-opts]
  (-> cli-opts
      (set/rename-keys {:sha :git/sha})
      (select-keys (into #{} cat valid-lib-opts))))

(defn- find-template-deps-fn
  "Returns a template-deps-fn given lib-opts parsed from raw CLI opts."
  [lib-opts]
  (some (fn [[k v]] (and (contains? (set k) (set (keys lib-opts))) v))
        lib-opts->template-deps-fn))

(defn- invalid-lib-opts-error [provided-lib-opts]
  (ex-info (str "Provided invalid combination of CLI options")
           {:provided-opts (set (keys provided-lib-opts))
            :valid-combinations valid-lib-opts}))

(def ^:private default-deps-info-client
  {:ensure-git-dir gitlibs-impl/ensure-git-dir})

(defn infer
  "Returns a tools.deps lib map for the given CLI opts."
  ([cli-opts] (infer default-deps-info-client cli-opts))
  ([client cli-opts]
   (let [lib-opts (cli-opts->lib-opts cli-opts)
         lib-sym (edn/read-string (:lib cli-opts))
         template-deps-fn (find-template-deps-fn lib-opts)]
     (if-not template-deps-fn
       (throw (invalid-lib-opts-error lib-opts))
       (template-deps-fn client lib-sym lib-opts)))))
