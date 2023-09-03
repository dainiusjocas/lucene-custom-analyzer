(ns lucene.custom.analyzer
  (:import (java.util HashMap Map)
           (java.io File)
           (java.nio.file Path)
           (org.apache.lucene.analysis.custom CustomAnalyzer CustomAnalyzer$Builder)
           (org.apache.lucene.analysis Analyzer CharFilterFactory TokenFilterFactory TokenizerFactory)))

(set! *warn-on-reflection* true)

(defn- prepare-params
  "Converts associative collection into a HashMap<String, String> as expected by Lucene.
  HashMap because the map must be modifiable."
  [params]
  (reduce-kv (fn [^Map hashmap k v]
               (doto hashmap (.put (name k) (str v))))
             (HashMap.) params))

(defn tokenizer-factories
  "Returns a map of available tokenizer factories: <String, Class>"
  []
  (reduce (fn tokenizer-name->class [acc ^String tokenizer-name]
            (assoc acc tokenizer-name (TokenizerFactory/lookupClass tokenizer-name)))
          {} (TokenizerFactory/availableTokenizers)))

(defn char-filter-factories
  "Returns a map of available char filter factories: <String, Class>"
  []
  (reduce (fn char-filter-name->class [acc ^String char-filter-name]
            (assoc acc char-filter-name (CharFilterFactory/lookupClass char-filter-name)))
          {} (CharFilterFactory/availableCharFilters)))

(defn token-filter-factories
  "Returns a map of available token filter factories: <String, Class>"
  []
  (reduce (fn token-filter->class [acc ^String token-filter-name]
            (assoc acc token-filter-name (TokenFilterFactory/lookupClass token-filter-name)))
          {} (TokenFilterFactory/availableTokenFilters)))

(def DEFAULT_TOKENIZER_NAME "standard")

(defn- config-dir->path ^Path [config-dir]
  (let [^String dir (or config-dir ".")]
    (.toPath (File. dir))))

(defn get-component-or-exception [factories component-name component-type namify-fn]
  (or (get factories (namify-fn (name component-name)))
      (throw
        (Exception.
          (format "%s '%s' is not available. Choose one of: %s"
                  component-type
                  component-name
                  (sort (keys factories)))))))

(defn handle-short-notation [analysis-component]
  (first (if (or (string? analysis-component) (keyword? analysis-component))
           {analysis-component nil}
           analysis-component)))

(defn create
  "Constructs a Lucene Analyzer using the CustomAnalyzer builder.
   Under the hood it uses the factory classes TokenizerFactory, TokenFilterFactory, and CharFilterFactory.
   The factories are loaded with java.util.ServiceLoader.

   Analysis component description is of shape:
   `
   {ComponentNameKeywordOrString MapOfParams}
   `
   Or when the MapOfParams is empty then only ComponentNameKeywordOrString can be passed.

   If needed factories can be passed as arguments in shape:
   `
   {STRING CLASS}
   `

   Example:
   `
   {:tokenizer {\"standard\" {:maxTokenLength 4}}
    :char-filters [{\"patternReplace\" {:pattern \"foo\", :replacement \"foo\"}}]
    :token-filters [{\"uppercase\" nil} {\"reverseString\" nil}]
    :config-dir \".\"}
   `

   Short notation:
   `
   {:tokenizer :standard
    :char-filters [:htmlStrip]
    :token-filters [:uppercase]}
   `

   `opts` map can specify these keys:
     - config-dir: path to directory from which resources will be loaded, default '.'
     - classpath-resources: when true and :config-dir is nil then loads resources from classpath
     - char-filters: list of char filter descriptions
     - tokenizer: tokenizer description, default 'standard' tokenizer
     - token-filters: list of token filter descriptions
     - position-increment-gap: specify position increment gap
     - offset-gap: specify offset gap
     - namify-fn: function that changes the string identifier of the service name, e.g. str/lowercase, default: identity"
  (^Analyzer [opts] (create opts (char-filter-factories) (tokenizer-factories) (token-filter-factories)))
  (^Analyzer [{:keys [config-dir char-filters tokenizer token-filters namify-fn
                      position-increment-gap offset-gap classpath-resources]}
              char-filter-factories tokenizer-factories token-filter-factories]
   (let [namify-fn (or namify-fn identity)
         ^CustomAnalyzer$Builder builder (if (and (true? classpath-resources) (nil? config-dir))
                                           (CustomAnalyzer/builder)
                                           (CustomAnalyzer/builder ^Path (config-dir->path config-dir)))]
     (assert (or (nil? char-filters) (sequential? char-filters))
             (format "Character filters should be a list, was '%s'" char-filters))
     (assert (or (nil? token-filters) (sequential? token-filters))
             (format "Token filters should be a list, was '%s'" token-filters))

     (assert (or (nil? tokenizer) (map? tokenizer) (string? tokenizer) (keyword? tokenizer))
             (format "Tokenizer must have 'name' and optional 'params', but was '%s'" tokenizer))
     (let [[tokenizer-name params] (handle-short-notation tokenizer)]
       (.withTokenizer builder
                       ^Class (get-component-or-exception tokenizer-factories
                                                          (or tokenizer-name DEFAULT_TOKENIZER_NAME)
                                                          "Tokenizer"
                                                          namify-fn)
                       ^Map (prepare-params params)))

     (doseq [char-filter char-filters]
       (let [[char-filter-name params] (handle-short-notation char-filter)]
         (assert (and (not (nil? char-filter)) (or (nil? params) (map? params)))
                 (format "Character filter must have 'name' and optional 'params', but was '%s'" char-filter))
         (.addCharFilter builder
                         ^Class (get-component-or-exception char-filter-factories
                                                            char-filter-name
                                                            "Char filter"
                                                            namify-fn)
                         ^Map (prepare-params params))))

     (doseq [token-filter token-filters]
       (let [[token-filter-name params] (handle-short-notation token-filter)]
         (assert (and (not (nil? token-filter)) (or (nil? params) (map? params)))
                 (format "Token filter must have 'name' and optional 'params', but was '%s'" token-filter))
         (.addTokenFilter builder
                          ^Class (get-component-or-exception token-filter-factories
                                                             token-filter-name
                                                             "Token filter"
                                                             namify-fn)
                          ^Map (prepare-params params))))

     (when position-increment-gap
       (.withPositionIncrementGap builder position-increment-gap))
     (when offset-gap
       (.withOffsetGap builder offset-gap))

     (.build builder))))

(comment
  (lucene.custom.analyzer/create
    {:tokenizer     {:standard {:maxTokenLength 4}}
     :char-filters  [{:patternReplace {:pattern     "foo"
                                       :replacement "foo"}}]
     :token-filters [{:uppercase nil}
                     {:reverseString nil}]})

  (lucene.custom.analyzer/create
    {:tokenizer     {:standard {:maxTokenLength 4}}
     :char-filters  [{:patternReplace {:pattern     "foo"
                                       :replacement "foo"}}]
     :token-filters [{:uppercase nil}
                     :reverseString]}))
