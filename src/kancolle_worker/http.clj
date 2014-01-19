(ns kancolle-worker.http
  (:require [clj-http.client :as http-client]
            [kancolle-worker.setting :as setting]
            [cheshire.core :as json]))

(defn post
  [path params]
  (let [params (merge
                 {:api_verno setting/verno
                  :api_token setting/token}
                 params)]
    (println (format "post: %s" path))
    (clojure.pprint/pprint params)
    (http-client/request
      {:scheme "http"
       :server-name setting/server-host
       :server-port (Integer. setting/server-port)
       :method :post
       :uri path
       :headers {"User-Agent" setting/client-agent
                 "Referer" (format "http://%s/kcs/mainD2.swf?api_token=%s&api_starttime=%s"
                                   setting/server-host
                                   setting/token
                                 setting/starttime)
                 "Origin" (format "http://%s" setting/server-host)
                 "Accept-Encoding" "gzip, deflate"}
       :form-params params})))

(defn key-trans
  [k-str]
  (let [k-str (if (.startsWith k-str "api_")
                (.substring k-str (.length "api_"))
                k-str)]
    (keyword
      (.replace k-str "_" "-"))))

(defn key-trans-r
  [k]
  (->> k
    (name)
    (#(.replace % "-" "_"))
    (str "api_")))

(defn kcs-post
  [kcs-path kcs-params]
  (let [path (str "/kcsapi/api_" (.substring kcs-path (.length "~")))
        params (into {}
                    (for [[k v] kcs-params]
                      [(key-trans-r k) v]))
        resp (let [s (:body
                       (post path params))
                   idx (.indexOf s "svdata=" 0)]
               (.substring s (+ idx (.length "svdata="))))]
    (json/parse-string resp key-trans)))

(defmacro defkcs-api
  [api-name path params & body]
  (let [param-keys (vec (map name params))
        body-empty? (empty? body)]
    `(defn ~api-name
       ~params
       (let [params# (zipmap ~param-keys [~@params])
             ~'result (kcs-post ~path params#)]
         (if ~body-empty?
           (:data ~'result)
           ~@body)))))
