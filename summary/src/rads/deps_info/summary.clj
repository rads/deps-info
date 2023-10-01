(ns rads.deps-info.summary
  (:require [clojure.set :as set]
            [babashka.fs :as fs]))

(def ^:private symbol-regex
  (re-pattern
    (str "(?i)^"
         "(?:((?:[a-z0-9-]+\\.)*[a-z0-9-]+)/)?"
         "((?:[a-z0-9-]+\\.)*[a-z0-9-]+)"
         "$")))

(defn- lib-str? [x]
  (boolean (and (string? x) (re-seq symbol-regex x))))

(defn- local-script-path? [x]
  (boolean (and (string? x) (fs/exists? x))))

(defn- http-url? [x]
  (boolean (and (string? x) (re-seq #"^https?://" x))))

(defn- git-ssh-url? [x]
  (boolean (and (string? x) (re-seq #"^.+@.+:.+\.git$" x))))

(defn- git-http-url? [x]
  (boolean (and (string? x) (re-seq #"^https?://.+\.git$" x))))

(defn git-repo-url? [s]
  (or (git-http-url? s) (git-ssh-url? s)))

(def ^:private deps-types
  [{:lib lib-str?
    :coords #{:local/root}
    :procurer :local}

   {:lib lib-str?
    :coords #{:mvn/version}
    :procurer :maven}

   {:lib local-script-path?
    :coords #{:bbin/url}
    :procurer :local}

   {:lib #(or (git-http-url? %) (git-ssh-url? %))
    :coords #{:bbin/url}
    :procurer :git}

   {:lib http-url?
    :coords #{:bbin/url}
    :procurer :http}

   {:lib lib-str?
    :coords #{:git/sha :git/url :git/tag}
    :procurer :git}

   {:lib local-script-path?
    :coords #{}
    :procurer :local}

   {:lib #(or (git-http-url? %) (git-ssh-url? %) (lib-str? %))
    :coords #{}
    :procurer :git}

   {:lib http-url?
    :coords #{}
    :procurer :http}])


(defn- deps-type-match? [cli-opts deps-type]
  (and ((:lib deps-type) (:script/lib cli-opts))
       (or (empty? (:coords deps-type))
           (seq (set/intersection (:coords deps-type) (set (keys cli-opts)))))
       deps-type))

(defn- match-deps-type [cli-opts]
  (or (some #(deps-type-match? cli-opts %) deps-types)
      {:procurer :unknown-procurer}))

(defn- match-artifact [cli-opts procurer]
  (cond
    (or (and (#{:local} procurer) (re-seq #"\.(clj|bb)$" (:script/lib cli-opts)))
        (and (#{:http} procurer) (re-seq #"\.(clj|bb)$" (:script/lib cli-opts))))
    :file

    (or (#{:maven} procurer)
        (and (#{:local} procurer)
             (or (and (:script/lib cli-opts) (re-seq #"\.jar$" (:script/lib cli-opts)))
                 (and (:local/root cli-opts) (re-seq #"\.jar$" (:local/root cli-opts)))))
        (and (#{:http} procurer) (re-seq #"\.jar$" (:script/lib cli-opts))))
    :jar

    (or (#{:git} procurer)
        (and (#{:local} procurer)
             (or (and (:script/lib cli-opts) (fs/directory? (:script/lib cli-opts)))
                 (and (:local/root cli-opts) (fs/directory? (:local/root cli-opts)))))
        (and (#{:http} procurer) (re-seq #"\.git$" (:script/lib cli-opts))))
    :dir

    :else :unknown-artifact))

(defn summary [cli-opts]
  (let [{:keys [procurer]} (match-deps-type cli-opts)
        artifact (match-artifact cli-opts procurer)]
    {:procurer procurer
     :artifact artifact}))
