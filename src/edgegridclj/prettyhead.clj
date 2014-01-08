(ns edgegridclj.prettyhead
  (:import (com.google.api.client.http HttpRequest)))

;; pretty printing

;; truncate long header values. Some are sequences instead of strings
;; so just print the first (usually only) string in the seq
(defn trunc [s n]
  (let [s (if (= (type s) java.lang.String) s (first s))]
  (if (< n (count s))
    (str (subs s 0 (max (- n 3) 0)) "...")
    s)))

(defn pretty-header-table [headers n]
  (let [ht (map #(hash-map :keys (key %) :values (trunc (val %) n))
                headers)]
    (clojure.pprint/print-table ht)))

(defn pretty-auth-headers [ah]
  (let [toks (clojure.string/split ah #";")
        f (clojure.string/split (first toks) #" ")
        alg (first f)
        ct (second f)]
    (prn (str "Algorithm: " alg))
    (prn ct)
    (doseq [t (rest toks)] (prn t))
    "-------------------end-------------------------"))

(defn pretty-headers [h]
  (println \newline "Request Headers:")
  (pretty-header-table h 40)
  (println \newline "Authorization Header:" \newline )
  (pretty-auth-headers (.getAuthorization h)))
