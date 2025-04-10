(ns migrations.2025-04-07-add-disclaimer-translation
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (sm/build-translations-payload conn
                                            {"behaveplus:disclaimer-paragraph-1" "The USFS Fire Sciences Laboratory makes no warranties or guarantees, either expressed or implied as to
the completeness, accuracy, or correctness of the data portrayed in this product, no accepts any liability,
arising from any incorrect, incomplete or misleading information contained therein. All information,
data and databases, are provided “As is” with no warranty, expressed or implied, including but not
limited to, fitness for a particular purpose."
                                             "behaveplus:disclaimer-paragraph-2" "By downloading, accessing or using this application and/or data contained within the databases, you
hereby release The Missoula Fire Sciences Laboratory, its employees, agents, contractors, and suppliers
from any and all responsibility and liability associated with its use. In no event shall The USFS Fire
Sciences Laboratory or its officers, employees, agents, contractors, and suppliers be liable for any
damages arising in any way out of the use of the application or use of the information contained in the
databases herein including, but not limited to the Behave products.
"
                                             "behaveplus:disclaimer-paragraph-3" "The USFS Fire Sciences Laboratory makes no warranties, either expressed or implied, concerning the
accuracy, completeness, reliability, or suitability of the information provided herein. Nor does The USFS
Fire Sciences Laboratory warrant that the use of this information is free of any claims of copyright
infringement. THE INFORMATION/APPLICATION IS BEING PROVIDED \"AS IS\" AND WITHOUT WARRANTY
OF ANY KIND EITHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING BUT NOT LIMITED TO THE IMPLIED
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND INFRINGEMENT. IN NO
EVENT SHALL THE AUTHOR OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE APPLICATION, SOFTWARE OR DATA OR THE USE OF OTHER DEALINGS IN THE
APPLICATION."}))



;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
