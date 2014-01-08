(ns edgegridclj.core
  (:require [edgegridclj.prettyhead :refer [pretty-headers]]
            [edgegridclj.sign       :refer [aka-open-sign!]])
  (:import (com.google.api.client.http GenericUrl
                                       HttpRequest
                                       HttpRequestFactory
                                       HttpRequestInitializer
                                       HttpTransport)
           (com.google.api.client.http.apache ApacheHttpTransport)))


;; EXAMPLE translated from https://github.com/akamai-open/edgegrid-auth-java

;; Note - this is a work in progress. It's unknown whether the signing
;; and canonicalization routines are right yet. Until they are,
;; signing requests will not authenticate correctly to Akamai OPEN APIs.

(defn make-get-request
  "Generate a basic GET request using google http client api"
  [proto host path]
  (let [uri (java.net.URI. proto host path nil nil)
        HTTP_TRANSPORT (ApacheHttpTransport.)
        requestFactory (.createRequestFactory HTTP_TRANSPORT)
        request (.buildGetRequest requestFactory (GenericUrl. uri))
        headers (.getHeaders request)
        _ (.set headers "Host", host)] ; mutate headers
    request))  

;; host and credential. Obtain these values from the client registration step
(def exampleHost "akaa-u5x3btzf44hplb4q-6jrzwnvo7llch3po.luna.akamaiapis.net")
(def exampleCredential  {:clientToken "akaa-nev5k66unzize2gx-5uz4svbszp4ko5wq"
                         :accessToken  "akaa-ublu6mqdcqkjw5lz-542a56pcogddddow"
                         :clientSecret "SOMESECRET"})

;; build a request object
(def exampleRequest (make-get-request "https"
                                      exampleHost
                                      "/billing-usage/v1/reportSources"))

;; sign the request using the credential
(def sigreq (aka-open-sign! exampleRequest exampleCredential))

;; look at the resulting signed request
(.toURI (.getUrl sigreq))  ;;show the URI
(.getHeaders sigreq)       ;;show the request headers
(.getAuthorization (.getHeaders sigreq))  ;;auth header

;; dump the request headers in a readable way
(pretty-headers (.getHeaders sigreq))
