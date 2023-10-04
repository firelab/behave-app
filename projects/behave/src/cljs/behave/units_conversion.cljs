(ns behave.units-conversion)

(defn to-feet
  "Convert value from specidfied units to feet"
  [value units]
  (case units
    "ft" value ;Already base unit, do nothing
    "in" (* value 0.08333333333333)
    "mm" (* value 0.003280839895)
    "cm" (* value 0.03280839895)
    "m"  (* value 3.2808398950131)
    "ch" (* value 66.0)
    "mi" (* value 5280.0)
    "km" (* value 3280.8398950131)))

(defn from-feet
  "Convert units from feet to specified units"
  [value units]
  (case units
    "ft" value ;Already base unit, do nothing
    "in" (* value 12)
    "mm" (* value 304.8)
    "cm" (* value 30.480)
    "m"  (* value 0.3048)
    "ch" (* value 0.0151515151515)
    "mi" (* value 0.0001893939393939394)
    "km" (* value 0.0003048)))


(defn to-map-units
  "Convert a value and units convert to map units in the specified map-units and map-representative
  fraction. (1:X)

  i.e.

  To convert 1,000 ft to Map units of in with a map representative fraction of 1:35,000

  Inputs should be
  value: 1,000
  units: ft
  map-units: in
  map-representative-fraction: 3500

  1,000 * 12 in/ft * 1/35,000 = 0.34 in.
  "
  [value units map-units map-representative-fraction]
  (-> value
      (to-feet units)
      (from-feet map-units)
      (* (/ 1 map-representative-fraction))))
