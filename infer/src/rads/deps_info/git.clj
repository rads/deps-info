(ns rads.deps-info.git
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.tools.gitlibs.impl :as gitlibs-impl]
            [babashka.fs :as fs]
            [babashka.process :refer [sh]]))

(defn default-branch [_client git-url]
  (let [lib-dir (gitlibs-impl/ensure-git-dir git-url)
        remote-info (sh "git remote show origin" {:dir lib-dir})
        [[_ branch]] (->> (:out remote-info)
                          str/split-lines
                          (some #(re-seq #"HEAD branch: (\w+)" %)))]
    branch))

(defn clean-github-lib [lib]
  (let [lib (str/replace lib "com.github." "")
        lib (str/replace lib "io.github." "")
        lib (symbol lib)]
    lib))

(defn latest-git-sha [client git-url]
  (let [lib-dir (gitlibs-impl/ensure-git-dir git-url)
        branch (default-branch client git-url)
        log-result (sh ["git" "log" "-n" "1" branch "--pretty=format:\"%H\""]
                       {:dir lib-dir})]
    (edn/read-string (:out log-result))))

(defn find-git-tag [_client git-url tag]
  (let [lib-dir (gitlibs-impl/ensure-git-dir git-url)
        tag-result (sh ["git" "rev-parse" tag] {:dir lib-dir})]
    {:name (str tag)
     :commit {:sha (str/trim (:out tag-result))}}))

(defn latest-git-tag [client git-url]
  (let [lib-dir (gitlibs-impl/ensure-git-dir git-url)
        log-result (sh "git describe --tags --abbrev=0" {:dir lib-dir})
        tag (edn/read-string (:out log-result))]
    (find-git-tag client git-url tag)))
