(ns scicloj.ml.ug-utils
  (:require [clojure.string :as str]
            [notespace.kinds :as kind]
            [notespace.view :as view]
            [scicloj.ml.core]
            [tablecloth.api :as tc]
            [libpython-clj2.python :as py]
            ))

(def doc->markdown (py/import-module "docstring_to_markdown"))



(def model-keys
  (keys @scicloj.ml.core/model-definitions*))

(def model-options
  (map
   :options
   (vals @scicloj.ml.core/model-definitions*)))

(defn dataset->md-hiccup [mds]
  (let [height (* 46 (- (count (str/split-lines (str mds))) 2))
        height-limit (min height 800)]
    [:div {:class "table table-striped table-hover table-condensed table-responsive"
           ;; :style {:height (str height-limit "px")}
           }
     (view/markdowns->hiccup mds)]))

(defmethod kind/kind->behaviour ::dataset-nocode
  [_]
  {:render-src?   false
   :value->hiccup #'dataset->md-hiccup})

(defn docu-options [model-key]
  (kind/override
   (->
    (tc/dataset
     (or
      (get-in @scicloj.ml.core/model-definitions* [model-key :options]  )
      {:name [] :type [] :default []}
      ))
    (tc/reorder-columns :name :type :default)
    )
   ::dataset-nocode
   )
  )


;; (->
;;  (tc/dataset
;;   (get-in @scicloj.ml.core/model-definitions* [:corenlp/crf :options] ))
;; (tc/reorder-columns :name :type :default)
;;  )

(defn text->hiccup
  "Convert newlines to [:br]'s."
  [text]
  (->> (str/split text #"\n")
       (interpose [:br])
       (map #(if (string? %)
               %
               (with-meta % {:key (gensym "br-")})))))

(defn docu-doc-string [model-key]
  (try
    (view/markdowns->hiccup
     (py/py. doc->markdown convert
             (or
              (get-in @scicloj.ml.core/model-definitions* [model-key :documentation :doc-string] ) "")))
    (catch Exception e "")))


(defn anchor-or-nothing [x text]
  (if (empty? x)
    [:div ""]
    [:div
     [:a {:href x} text]]
    )
  )

(defn render-key-info [prefix]
  (->> @scicloj.ml.core/model-definitions*
       (sort-by first)
       (filter #(str/starts-with? (first %) prefix ))
       (map
        (fn [[key definition]]
          [:div
           [:h3 (str key)]
           (anchor-or-nothing (:javadoc (:documentation definition)) "javadoc")
           (anchor-or-nothing (:user-guide (:documentation definition)) "user guide")


           [:span
            (dataset->md-hiccup (docu-options key) )]

           [:span
            (docu-doc-string key)]

           [:hr]
           [:div "Example:"]
           [:div
            [:p/code {:code (str
                             (get-in definition [:documentation :code-example]
                                     "" ))
                      :bg-class "bg-light"}]]

           [:hr]
           ]))))

(defn remove-deep [key-set data]
  (clojure.walk/prewalk (fn [node] (if (map? node)
                                    (apply dissoc node key-set)
                                    node))
                        data))
