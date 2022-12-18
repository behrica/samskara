(ns scicloj.ml.core
  ;;Autogenerated from scicloj.ml.template.core-- DO NOT EDIT
  "Core functions for machine learninig and pipeline execution.

  Requiring this namesspace registers as well the model in:

  * scicloj.ml.smile.classification
  * scicloj.ml.smile.regression
  * scicloj.ml.xgboost


  Functions are re-exported from:



  * scicloj.metamorph.ml.*
  * scicloj.metamorph.core

  "
  (:require [scicloj.ml.template.core]
            [scicloj.metamorph.core]
            [scicloj.metamorph.ml]
            [scicloj.metamorph.ml.classification]
            [scicloj.metamorph.ml.gridsearch]
            [scicloj.metamorph.ml.loss]))

(defn ->pipeline
  "Create pipeline from declarative description."
  ([ops]
  (scicloj.metamorph.core/->pipeline ops))
  ([config ops]
  (scicloj.metamorph.core/->pipeline config ops)))


(defn categorical
  "Given a vector a categorical values create a gridsearch definition."
  ([value-vec]
  (scicloj.metamorph.ml.gridsearch/categorical value-vec)))


(defn classification-accuracy
  "correct/total.
Model output is a sequence of probability distributions.
label-seq is a sequence of values.  The answer is considered correct
if the key highest probability in the model output entry matches
that label."
  (^{:tag double} [lhs rhs]
  (scicloj.metamorph.ml.loss/classification-accuracy lhs rhs)))


(defn classification-loss
  "1.0 - classification-accuracy."
  (^{:tag double} [lhs rhs]
  (scicloj.metamorph.ml.loss/classification-loss lhs rhs)))


(defn confusion-map
  ([predicted-labels labels normalize]
  (scicloj.metamorph.ml.classification/confusion-map predicted-labels labels normalize))
  ([predicted-labels labels]
  (scicloj.metamorph.ml.classification/confusion-map predicted-labels labels)))


(defn confusion-map->ds
  ([conf-matrix-map normalize]
  (scicloj.metamorph.ml.classification/confusion-map->ds conf-matrix-map normalize))
  ([conf-matrix-map]
  (scicloj.metamorph.ml.classification/confusion-map->ds conf-matrix-map)))


(defmacro def-ctx
  "Convenience macro for defining pipelined operations that
   bind the current value of the context to a var, for simple
   debugging purposes."
  ([varname]
  `(scicloj.ml.template.core/def-ctx ~varname)))


(defn default-loss-fn
  "Given a datset which must have exactly 1 inference target column return a default
  loss fn. If column is categorical, loss is tech.v3.ml.loss/classification-loss, else
  the loss is tech.v3.ml.loss/mae (mean average error)."
  ([dataset]
  (scicloj.metamorph.ml/default-loss-fn dataset)))


(defn default-result-dissoc-in-fn
  ([result]
  (scicloj.metamorph.ml/default-result-dissoc-in-fn result)))


(def default-result-dissoc-in-seq scicloj.metamorph.ml/default-result-dissoc-in-seq)
(defn define-model!
  "Create a model definition.  An ml model is a function that takes a dataset and an
  options map and returns a model.  A model is something that, combined with a dataset,
  produces a inferred dataset."
  ([model-kwd train-fn predict-fn opts]
  (scicloj.metamorph.ml/define-model! model-kwd train-fn predict-fn opts)))


(defn do-ctx
  "Apply f:: ctx -> any, ignore the result, leaving
   pipeline unaffected.  Akin to using doseq for side-effecting
   operations like printing, visualization, or binding to vars
   for debugging."
  ([f]
  (scicloj.metamorph.core/do-ctx f)))


(defn ensemble-pipe
  ([pipes]
  (scicloj.metamorph.ml/ensemble-pipe pipes)))


(defn evaluate-pipelines
  "Evaluates the performance of a seq of metamorph pipelines, which are suposed to have a model as last step under key :model,
  which behaves correctly  in mode :fit and  :transform. The function `scicloj.metamorph.ml/model` is such function behaving correctly.
  
   This function calculates the accuracy or loss, given as `metric-fn` of each pipeline in `pipeline-fn-seq` using all the train-test splits
  given in  `train-test-split-seq`.

   It runs the pipelines  in mode  :fit and in mode :transform for each pipeline-fn in `pipe-fn-seq` for each split in `train-test-split-seq`.

   The function returns a seq of seqs of evaluation results per pipe-fn per train-test split.
   Each of teh evaluation results is a context map, which is specified in the malli schema attached to this function. 

   * `pipe-fn-or-decl-seq` need to be  sequence of pipeline functions or pipline declarations which follow the metamorph approach.
      These type of functions get produced typically by calling `scicloj.metamorph/pipeline`. Documentation is here:

   * `train-test-split-seq` need to be a sequence of maps containing the  train and test dataset (being tech.ml.dataset) at keys :train and :test.
    `tableclot.api/split->seq` produces such splits. Supervised models require both keys (:train and :test), while unsupervised models only use :train

   * `metric-fn` Metric function to use. Typically comming from `tech.v3.ml.loss`. For supervised models the metric-fn receives the trueth
      and predicted vales as double arrays and should return a single double number.  For unsupervised models he function receives the fitted ctx
      and should return a singel double number as well. This metric will be used to sort and eventualy filter the result, depending on the options
      (:return-best-pipeline-only   and :return-best-crossvalidation-only). The notion of `best` comes from metric-fn combined with loss-and-accuracy
  

   * `loss-or-accuracy` If the metric-fn is a loss or accuracy calculation. Can be :loss or :accuracy. Decided the notion of `best` model.
      In case of :loss pipelines with lower metric are better, in case of :accuracy pipelines with higher value are better.

  * `options` map controls some mainly performance related parameters. These function can potentialy result in a large ammount of data,
    able to bring the JVM into out-of-memory. We can control how many details the function returns by the following parameter: 
     The default are quite aggresive in removing details, and this can be tweaked further into more or less details via:
     


       * `:return-best-pipeline-only` - Only return information of the best performing pipeline. Default is true.
       * `:return-best-crossvalidation-only` - Only return information of the best crossvalidation (per pipeline returned). Default is true.
       * `:map-fn` - Controls parallelism, so if we use map (:map) , pmap (:pmap) or :mapv to map over different pipelines. Default :pmap
       * `:evaluation-handler-fn` - Gets called once with the complete result of an individual pipeline evaluation.
           It can be used to adapt the data returned for each evaluation and / or to make side effects using
           the evaluatio data.
           The result of this function is taken as evaluation result. It need to  contain as a minumum this 2 key paths:
           [:train-transform :metric]
           [:test-transform :metric]
           All other evalution data can be removed, if desired.

           It can be used for side effects as well, like experiment tracking on disk.
           The passed in evaluation result is a map with all information on the current evaluation, including the datasets used.

           The default handler function is:  `scicloj.metamorph.ml/default-result-dissoc--in-fn` which removes the often large
           model object and the training data.
           `identity` can be use to get all evaluation data.
           `scicloj.metamorph.ml/result-dissoc-in-seq--all` reduces even more agressively.


  
       * `:other-metrices` Specifies other metrices to be calculated during evaluation

   This function expects as well the ground truth of the target variable into
   a specific key in the context at key `:model :scicloj.metamorph.ml/target-ds`
   See here for the simplest way to set this up: https://github.com/behrica/metamorph.ml/blob/main/README.md
   The function [[scicloj.ml.metamorph/model]] does this correctly.
  "
  ([pipe-fn-or-decl-seq train-test-split-seq metric-fn loss-or-accuracy options]
  (scicloj.metamorph.ml/evaluate-pipelines pipe-fn-or-decl-seq train-test-split-seq metric-fn loss-or-accuracy options))
  ([pipe-fn-seq train-test-split-seq metric-fn loss-or-accuracy]
  (scicloj.metamorph.ml/evaluate-pipelines pipe-fn-seq train-test-split-seq metric-fn loss-or-accuracy)))


(defn explain
  "Explain (if possible) an ml model.  A model explanation is a model-specific map
  of data that usually indicates some level of mapping between features and importance"
  ([model & args]
  (apply scicloj.metamorph.ml/explain model args)))


(defn fit
  "Helper function which executes pipeline op(s) in mode :fit on the given data and returns the fitted ctx.

  Main use is for cases in which the pipeline gets executed ones and no model is part of the pipeline."
  ([data & args]
  (apply scicloj.metamorph.core/fit data args)))


(defn fit-pipe
  "Helper function which executes pipeline op(s) in mode :fit on the given data and returns the fitted ctx.

  Main use is for cases in which the pipeline gets executed ones and no model is part of the pipeline."
  ([data pipe-fn]
  (scicloj.metamorph.core/fit-pipe data pipe-fn)))


(defn format-fn-sources
  ([fn-sources]
  (scicloj.metamorph.ml/format-fn-sources fn-sources)))


(defn get-nice-source-info
  ([pipeline-decl pipe-fns-ns pipe-fns-source-file]
  (scicloj.metamorph.ml/get-nice-source-info pipeline-decl pipe-fns-ns pipe-fns-source-file)))


(defn hyperparameters
  "Get the hyperparameters for this model definition"
  ([model-kwd]
  (scicloj.metamorph.ml/hyperparameters model-kwd)))


(defn lift
  "Create context aware version of the given `op` function. `:metamorph/data` will be used as a first parameter.

  Result of the `op` function will be stored under `:metamorph/data`"
  ([op & args]
  (apply scicloj.metamorph.core/lift op args)))


(defn linear
  "Create a gridsearch definition which does a linear search.

  * res-dtype-or-space map be either a datatype keyword or a vector
    of categorical values."
  ([start end n-steps res-dtype-or-space]
  (scicloj.metamorph.ml.gridsearch/linear start end n-steps res-dtype-or-space))
  ([start end n-steps]
  (scicloj.metamorph.ml.gridsearch/linear start end n-steps))
  ([start end]
  (scicloj.metamorph.ml.gridsearch/linear start end)))


(defn lookup-tables-consistent?
  ([train-lookup-table prediction-lookup-table]
  (scicloj.metamorph.ml/lookup-tables-consistent? train-lookup-table prediction-lookup-table)))


(defn mae
  "mean absolute error"
  (^{:tag double} [predictions labels]
  (scicloj.metamorph.ml.loss/mae predictions labels)))


(defn model-definition-names
  "Return a list of all registered model defintion names."
  ([]
  (scicloj.metamorph.ml/model-definition-names )))


(def model-definitions* scicloj.metamorph.ml/model-definitions*)
(defn mse
  "mean squared error"
  (^{:tag double} [predictions labels]
  (scicloj.metamorph.ml.loss/mse predictions labels)))


(defn options->model-def
  "Return the model definition that corresponse to the :model-type option"
  (^{:pre [(contains? options :model-type)]} [options]
  (scicloj.metamorph.ml/options->model-def options)))


(defn pipe-it
  "Takes a data objects, executes the pipeline op(s) with it in :metamorph/data
  in mode :fit and returns content of :metamorph/data.
  Usefull to use execute a pipeline of pure data->data functions on some data"
  ([data & args]
  (apply scicloj.metamorph.core/pipe-it data args)))


(defn pipeline
  "Create a metamorph pipeline function out of operators.

  `ops` are metamorph compliant functions (basicaly fn, which takle a ctx as first argument)

  This function returns a function, whcih can ve execute with a ctx as parameter.
  "
  ([& args]
  (apply scicloj.metamorph.core/pipeline args)))


(defn predict
  "Predict returns a dataset with only the predictions in it.

  * For regression, a single column dataset is returned with the column named after the
    target
  * For classification, a dataset is returned with a float64 column for each target
    value and values that describe the probability distribution."
  ([dataset model]
  (scicloj.metamorph.ml/predict dataset model)))


(defn probability-distributions->labels
  ([prob-dists]
  (scicloj.metamorph.ml.classification/probability-distributions->labels prob-dists)))


(defn reduce-result
  ([r result-dissoc-in-seq]
  (scicloj.metamorph.ml/reduce-result r result-dissoc-in-seq)))


(def result-dissoc-in-seq--all scicloj.metamorph.ml/result-dissoc-in-seq--all)
(defn result-dissoc-in-seq--all-fn
  ([result]
  (scicloj.metamorph.ml/result-dissoc-in-seq--all-fn result)))


(def result-dissoc-in-seq--ctxs scicloj.metamorph.ml/result-dissoc-in-seq--ctxs)
(defn result-dissoc-in-seq-ctx-fn
  ([result]
  (scicloj.metamorph.ml/result-dissoc-in-seq-ctx-fn result)))


(defn rmse
  "root mean squared error"
  (^{:tag double} [predictions labels]
  (scicloj.metamorph.ml.loss/rmse predictions labels)))


(defn sobol-gridsearch
  "Given an map of key->values where some of the values are gridsearch definitions
  produce a sequence of fully defined maps.


```clojure
user> (require '[tech.v3.ml.gridsearch :as ml-gs])
nil
user> (def opt-map  {:a (ml-gs/categorical [:a :b :c])
                     :b (ml-gs/linear 0.01 1 10)
                     :c :not-searched})
user> opt-map
{:a
 {:tech.v3.ml.gridsearch/type :linear,
  :start 0.0,
  :end 2.0,
  :n-steps 3,
  :result-space [:a :b :c]}
  ...

user> (ml-gs/sobol-gridsearch opt-map)
({:a :b, :b 0.56, :c :not-searched}
 {:a :c, :b 0.22999999999999998, :c :not-searched}
 {:a :b, :b 0.78, :c :not-searched}
...
```
  "
  ([opt-map start-idx]
  (scicloj.metamorph.ml.gridsearch/sobol-gridsearch opt-map start-idx))
  ([opt-map]
  (scicloj.metamorph.ml.gridsearch/sobol-gridsearch opt-map)))


(defn thaw-model
  "Thaw a model.  Model's returned from train may be 'frozen' meaning a 'thaw'
  operation is needed in order to use the model.  This happens for you during predict
  but you may also cached the 'thawed' model on the model map under the
  ':thawed-model'  keyword in order to do fast predictions on small datasets."
  ([model opts]
  (scicloj.metamorph.ml/thaw-model model opts))
  ([model]
  (scicloj.metamorph.ml/thaw-model model)))


(defn train
  "Given a dataset and an options map produce a model.  The model-type keyword in the
  options map selects which model definition to use to train the model.  Returns a map
  containing at least:


  * `:model-data` - the result of that definitions's train-fn.
  * `:options` - the options passed in.
  * `:id` - new randomly generated UUID.
  * `:feature-columns` - vector of column names.
  * `:target-columns` - vector of column names."
  ([dataset options]
  (scicloj.metamorph.ml/train dataset options)))


(defn transform-pipe
  "Helper functions which execute the passed `pipe-fn` on the given `data` in mode :transform.
  It merges the data into the provided `ctx` while doing so."
  ([data pipe-fn ctx]
  (scicloj.metamorph.core/transform-pipe data pipe-fn ctx)))


(defn validate-lookup-tables
  ([model predict-ds-classification target-col]
  (scicloj.metamorph.ml/validate-lookup-tables model predict-ds-classification target-col)))


