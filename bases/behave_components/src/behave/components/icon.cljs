(ns behave.components.icon)

;; Modules
(def ^:private contain-icon
[:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 50 50" :width "50px" :height "50px"} [:circle {:cx "25" :cy "25" :r "25" :style {:fill "#fff"}}] [:g {} [:g {} [:line {:x1 "0.92" :y1 "25.28" :x2 "9.46" :y2 "25.28" :style {:fill "none" :stroke "#082233" :stroke-miterlimit "10"}}] [:line {:x1 "9.7" :y1 "35.12" :x2 "9.7" :y2 "15.4" :style {:fill "none" :stroke "#082233" :stroke-linecap "round" :stroke-miterlimit "10"}}]] [:g {} [:line {:x1 "49.08" :y1 "25.28" :x2 "40.54" :y2 "25.28" :style {:fill "none" :stroke "#082233" :stroke-miterlimit "10"}}] [:line {:x1 "40.3" :y1 "35.12" :x2 "40.3" :y2 "15.4" :style {:fill "none" :stroke "#082233" :stroke-linecap "round" :stroke-miterlimit "10"}}]] [:g {} [:line {:x1 "24.75" :y1 "1.34" :x2 "24.75" :y2 "9.88" :style {:fill "none" :stroke "#082233" :stroke-miterlimit "10"}}] [:line {:x1 "14.91" :y1 "10.12" :x2 "34.62" :y2 "10.12" :style {:fill "none" :stroke "#082233" :stroke-linecap "round" :stroke-miterlimit "10"}}]] [:g {} [:line {:x1 "24.75" :y1 "48.66" :x2 "24.75" :y2 "40.12" :style {:fill "none" :stroke "#082233" :stroke-miterlimit "10"}}] [:line {:x1 "14.91" :y1 "39.88" :x2 "34.62" :y2 "39.88" :style {:fill "none" :stroke "#082233" :stroke-linecap "round" :stroke-miterlimit "10"}}]]] [:g {} [:rect {:x "18" :y "18" :width "15" :height "14" :rx "2" :style {:fill "#ff5c5c"}}] [:rect {:x "19" :y "19" :width "13" :height "12" :rx "1" :style {:fill "none" :stroke "#000" :stroke-width "2px"}}]] [:circle {:cx "25" :cy "25" :r "25" :style {:fill "none"}}] [:circle {:cx "25" :cy "25" :r "24.5" :style {:fill "none" :stroke "#000"}}]])

(def ^:private crown-icon
[:svg {:xmlns "http://www.w3.org/2000/svg" :xmlns:xlink "http://www.w3.org/1999/xlink" :viewbox "0 0 51 57.69"}
 [:defs
  [:clippath {:id "ad82e2f1-64a4-4d13-8186-1429ce870922" :transform "translate(-0.5 -0.5)"}
   [:circle {:cx "26" :cy "27" :r "25" :style {:fill "none"}}]]]
 [:circle {:cx "25.5" :cy "25.5" :r "25" :style {:fill "#fff"}}]
 [:g {:style {:clip-path "url(#ad82e2f1-64a4-4d13-8186-1429ce870922)"}}
  [:path {:d "M25.17,15.93c.72.75,1.38,1.34,1.94,2a17.57,17.57,0,0,1,2.82,4.8,20.61,20.61,0,0,1,1.55,6.56c0,.8.2,1.6.2,2.39,0,1.06-.07,2.12-.16,3.18s-.24,2.27-.38,3.4c0,.23-.12.44-.16.66s-.08.6-.12.89L31,40a3.66,3.66,0,0,0,.44-.44,11.84,11.84,0,0,0,2-6.29c.3-.12.39.14.47.38.68,2.07,1.37,4.14,2,6.23a12.43,12.43,0,0,1,.35,2,19.68,19.68,0,0,1,0,6.08,12.83,12.83,0,0,1-4.15,7.69,22.64,22.64,0,0,1-2.49,1.74,4.25,4.25,0,0,1-.86.33c.09-.4.14-.69.22-1a6.05,6.05,0,0,0-.22-3.85,8.27,8.27,0,0,0-2.07-3.18,7.13,7.13,0,0,1-1.92-6,25.56,25.56,0,0,1,.78-4.26c0-.14.07-.27.1-.41a.89.89,0,0,0-.07-.28,7.46,7.46,0,0,0-.95.82,12.35,12.35,0,0,0-3,11.3A18.05,18.05,0,0,0,24,56.71c.16.26.29.55.5,1a7.52,7.52,0,0,1-2.76-1.14c-.32-.21-.63-.46-1-.69A10.85,10.85,0,0,1,16.72,50a20.08,20.08,0,0,1-.84-6.58,15.1,15.1,0,0,1,1.73-6.55,34.28,34.28,0,0,1,3.52-5.24,21.14,21.14,0,0,0,3.07-5,16,16,0,0,0,1.14-8.35c-.07-.56-.17-1.13-.24-1.69A4.93,4.93,0,0,1,25.17,15.93Z" :transform "translate(-0.5 -0.5)" :style {:fill "#ff9d71" :stroke "#292929" :stroke-miterlimit "10" :stroke-width "0.699999988079071px"}}]]
 [:circle {:cx "25.5" :cy "25.5" :r "25" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10"}}]])

(def ^:private mortality-icon
[:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 50 50" :width "50px" :height "50px"} [:circle {:cx "25" :cy "25" :r "25" :style {:fill "#fff"}}] [:path {:d "M36.3133.9l-4.75-6.21h2.56l-3.29-4.58h1.92l-3.2-4.38h2l-5.85-9V9.59l0.07-.05-.07v.14l-5.859h2l-3.24.38H20.5l-3.284.58h2.56L1533.9h9.06V44.79h3.38V33.9Z" :transform "translate(-1 -1)" :style {:fill "#80470f"}}] [:g {} [:circle {:cx "25" :cy "25" :r "25" :style {:fill "none"}}] [:circle {:cx "25" :cy "25" :r "24.5" :style {:fill "none" :stroke "#000"}}]]])

(def ^:private surface-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 51 51" :width "51px" :height "51px"} [:path {:d "M25.6432.52l.56.07a27.1227.120006.51-4.06c4.35-3.77.46-8.6911.8-12.4A27.0727.0700147.381424.524.50003.836.33a42.3242.3200117.78-4C2332.3224.332.3925.6432.52Z" :transform "translate(-0.5 -0.5)" :style {:fill "#fff"}}] [:path {:d "M50.2717.89c-.31.21-.62.41-.92.63h0C49.6518.35018.150.2717.89Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M33.4634h0a43.943.9000-5.78-1.19c2.24.425.31.075.31.07a41.341.30017.372.89l.16-.11A42.5342.5300033.4634Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M24.638.69A35.7335.730008.9240.87a383800112.89-2.28C22.7438.5923.6738.6224.638.69Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M35.5141.1c-.56-.24-1.12-.45-1.69-.66l-.080.110C34.4140.683540.8935.5141.1Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M2650.45q1.2402.46-.12H23.54Q24.7650.452650.45Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M47.8719.69c.48-.411-.81.48-1.17h0c-.51.37-1.76-1.481.17-4.343.71-7.458.7-11.812.4-.59.49-1.2.95-1.821.39.62-.441.23-.91.82-1.39C40.4228.3943.5323.4147.8719.69Z" :transform "translate(-0.5 -0.5)" :style {:fill "none"}}] [:path {:d "M34.2533.48c-.26.19-.52.38-.79.56h0C33.7333.863433.6734.2533.48Z" :transform "translate(-0.5 -0.5)" :style {:fill "#a6c422" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M40.3536.81A41.341.30003333.92s-3.06-.65-5.3-1.07l-1.48-.26-.56-.07c-1.34-.13-2.69-.2-4.06-.2a42.3242.32000-17.78424.6324.630003.335.24c.59-.251.19-.481.79-.7A35.7335.7300124.638.69a38.4238.420019.141.78l.080-.33-.13A28.1128.1100040.3536.81Z" :transform "translate(-0.5 -0.5)" :style {:fill "#bcd161" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M21.8144a32.2232.22000-10.241.7324.3324.33000124.59h4.92A24.5224.520003747.83333300021.8144Z" :transform "translate(-0.5 -0.5)" :style {:fill "#cedd8d" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M7.1341.57v0c.59-.261.18-.491.78-.72C8.3241.097.7241.327.1341.57Z" :transform "translate(-0.5 -0.5)" :style {:fill "#a6c422" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M42.4535.22c-.54.47-1.11.9-1.691.32l-.25.16-.16.11a28.1128.11001-6.863.5l.33.13c.57.211.13.421.69.66a36.736.70016.613.2924.4124.410008.34-17.13c0-.210-.430-.65C47.8729.5545.4432.6842.4535.22Z" :transform "translate(-0.5 -0.5)" :style {:fill "#cedd8d" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M35.5141.1c-.55-.21-1.1-.42-1.66-.6l-.110a38.4238.42000-9.14-1.78c-.93-.07-1.86-.1-2.79-.1A38380008.9240.87c-.6.23-1.19.46-1.78.72a24.6724.670004.194l.24.17A32.2232.2200121.814433330013747.83l.78-.41a24.6224.620004.09-2.83l.23-.2A36.736.700035.5141.1Z" :transform "translate(-0.5 -0.5)" :style {:fill "#a6c422" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:g {} [:path {:d "M2023.45a4.094.090114.09-4.09h0A4.14.10012023.45Z" :transform "translate(-0.5 -0.5)" :style {:fill "#ffda0d" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.5px"}}] [:g {} [:line {:x1 "19.51" :y1 "12.82" :x2 "19.51" :y2 "11.55" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "17.44" :y1 "13.19" :x2 "17.01" :y2 "11.99" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "15.63" :y1 "14.24" :x2 "14.81" :y2 "13.26" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "14.28" :y1 "15.84" :x2 "13.17" :y2 "15.21" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "13.56" :y1 "17.81" :x2 "12.3" :y2 "17.59" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "13.56" :y1 "19.91" :x2 "12.3" :y2 "20.13" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "14.28" :y1 "21.88" :x2 "13.17" :y2 "22.52" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "15.63" :y1 "23.49" :x2 "14.81" :y2 "24.47" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "17.44" :y1 "24.54" :x2 "17.01" :y2 "25.74" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "19.51" :y1 "24.9" :x2 "19.51" :y2 "26.18" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "21.57" :y1 "24.54" :x2 "22.01" :y2 "25.74" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "23.39" :y1 "23.49" :x2 "24.21" :y2 "24.47" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "24.74" :y1 "21.88" :x2 "25.84" :y2 "22.52" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "25.46" :y1 "19.91" :x2 "26.71" :y2 "20.13" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "25.45" :y1 "17.81" :x2 "26.71" :y2 "17.59" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "24.74" :y1 "15.84" :x2 "25.84" :y2 "15.21" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "23.39" :y1 "14.24" :x2 "24.21" :y2 "13.26" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}] [:line {:x1 "21.57" :y1 "13.19" :x2 "22.01" :y2 "11.99" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.600000023841858px"}}]]] [:path {:d "M40.5136.7l.25-.16c.58-.421.15-.851.69-1.323-2.545.42-5.678-8.610-.220-.440-.66a24.6324.63000-1.15-7.43c-.51.37-1.76-1.481.17-4.343.72-7.458.7-11.812.4-.59.49-1.2.95-1.821.39-.26.19-.52.38-.79.56h0A42.5342.5300140.5136.7Z" :transform "translate(-0.5 -0.5)" :style {:fill "#a6c422" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:path {:d "M32.7128.53a27.1227.12001-6.514.06l1.48.26A43.943.900133.4634c.27-.18.53-.37.79-.56.62-.441.23-.91.82-1.394.35-3.77.46-8.6911.8-12.4.48-.411-.81.48-1.17a24.7324.73000-2-4.5327.0727.07000-2.872.14C40.1719.8437.0624.8332.7128.53Z" :transform "translate(-0.5 -0.5)" :style {:fill "#cedd8d" :stroke "#000" :stroke-miterlimit "10" :stroke-width "0.300000011920929px"}}] [:circle {:cx "25.5" :cy "25.5" :r "25" :style {:fill "none" :stroke "#000" :stroke-miterlimit "10"}}]])

;; System Icons
(def ^:private arrow-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 8.6 7.94" :width "8px" :height "8px"} [:path {:d "M5.3,7.43a1,1,0,0,1-1.36.39,1,1,0,0,1-.39-.39L.25,1.49A1,1,0,0,1,.64.13.91.91,0,0,1,1.12,0h6.6a1,1,0,0,1,1,1,1,1,0,0,1-.12.49Z", :transform "translate(-0.12 0)"}]])


(def ^:private help-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns:xlink "http://www.w3.org/1999/xlink", :viewbox "0 0 31.73 30.82" :height "24px" :width "32px"} [:defs [:clippath {:id "a0d6102d-1ebf-4073-a1af-5993a879397c" :transform "translate(-0.64 -0.59)"} [:rect {:x "0.64", :y "0.59", :width "31.73", :height "30.82", :style {:fill "none"}}]]] [:g {} [:g {:style {:clip-path "url(#a0d6102d-1ebf-4073-a1af-5993a879397c)"}} [:g {} [:path {:d "M23.15,12.88a6.65,6.65,0,1,0-6.65,6.68,6.65,6.65,0,0,0,6.65-6.68m-5.66,4.34h-2V11.56h2ZM15.33,9.33a1.17,1.17,0,1,1,1.17,1.16,1.17,1.17,0,0,1-1.17-1.16", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M23.53,12.88a7,7,0,1,0-7,7.06,7,7,0,0,0,7-7.06h0m-13.69,0a6.65,6.65,0,1,1,6.65,6.68,6.65,6.65,0,0,1-6.65-6.68", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.76,23.68l-.07,0,.07,0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.63,23.72l-.06,0,.06,0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.35,23.93l0-.06,0,.06", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.51,23.78l-.06,0,.06,0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M28.58.59H4.42A3.79,3.79,0,0,0,.64,4.39V21.27a3.79,3.79,0,0,0,3.78,3.8h7.32l-.2-.3a.7.7,0,0,1,.12-1,.69.69,0,0,1,1,.13l0,0,.11.15a.69.69,0,0,0-.65-.45H4.42A2.39,2.39,0,0,1,2,21.27V4.39A2.39,2.39,0,0,1,4.42,2H28.58A2.4,2.4,0,0,1,31,4.39h0V21.27a2.4,2.4,0,0,1-2.39,2.4H20.84a.71.71,0,0,1,.45.12.7.7,0,0,1,.18,1h0l-.21.3h7.32a3.79,3.79,0,0,0,3.78-3.8V4.39A3.79,3.79,0,0,0,28.58.59", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.69,23.7l-.06,0,.06,0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.4,23.87s0,0,.05-.05,0,0-.05.05", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.84,23.67h0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.19,24.37a.7.7,0,0,1,.16-.44l0,0L16.5,29.48l-3.71-5.36a.7.7,0,0,1-.41.9.69.69,0,0,1-.24.05h-.4l4.19,6a.7.7,0,0,0,1,.17.69.69,0,0,0,.17-.17l4.19-6h-.37a.7.7,0,0,1-.7-.7", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M20.57,23.75l-.06,0,.06,0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M21.29,23.79a.71.71,0,0,0-.45-.12h-.08l-.07,0-.06,0-.06,0-.06,0-.06,0s0,0-.05.05l0,.06a.7.7,0,0,0,.1,1,.72.72,0,0,0,.44.16h.37l.2-.3a.69.69,0,0,0-.17-1h0", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}] [:path {:d "M12.84,24.37a1,1,0,0,0-.05-.25L12.68,24a.7.7,0,1,0-1.18.74l0,.06.2.3h.4a.71.71,0,0,0,.7-.7", :transform "translate(-0.64 -0.59)", :style {:fill "#000"}}]]]]])

(def ^:private manual-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns:xlink "http://www.w3.org/1999/xlink", :viewbox "0 0 23.51 18.47" :height "24px" :width "24px"} [:defs [:clippath {:id "e51e4db7-81f2-4d10-aeeb-5c4276e6b883" :transform "translate(0 -0.12)"} [:rect {:y "0.24", :width "24", :height "18.53", :style {:fill "none"}}]]] [:g {:style {:clip-path "url(#e51e4db7-81f2-4d10-aeeb-5c4276e6b883)"}} [:g {} [:path {:d "M11.75,16.9h-.11L2.82,14.37a.42.42,0,0,1-.3-.4V.53A.41.41,0,0,1,2.94.12h.11l8.7,2.49L20.45.13A.42.42,0,0,1,21,.42a.43.43,0,0,1,0,.11V14a.41.41,0,0,1-.3.4l-8.82,2.52-.11,0m-8.4-3.25,8.4,2.4,8.4-2.4V1.09L11.87,3.45a.3.3,0,0,1-.23,0L3.36,1.09Z", :transform "translate(0 -0.12)", :style {:fill "#000"}}] [:path {:d "M13,18.58H10.5a.44.44,0,0,1-.38-.23L9.8,17.7.35,16.06A.43.43,0,0,1,0,15.64V1.37A.42.42,0,0,1,.42,1H.48l.85.13a.42.42,0,0,1-.11.83h0L.84,1.85V15.29l9.31,1.62a.41.41,0,0,1,.3.23l.3.6h2l.3-.6a.41.41,0,0,1,.3-.23l9.31-1.62V1.85l-.37.06a.43.43,0,0,1-.48-.35.41.41,0,0,1,.35-.48h0L23,1a.42.42,0,0,1,.47.36.13.13,0,0,1,0,.06V15.64a.43.43,0,0,1-.35.42L13.71,17.7l-.32.65a.43.43,0,0,1-.38.23", :transform "translate(0 -0.12)", :style {:fill "#000"}}] [:path {:d "M11.75,16.9a.42.42,0,0,1-.42-.42V3.05a.43.43,0,0,1,.43-.42.41.41,0,0,1,.41.42V16.49a.41.41,0,0,1-.42.41", :transform "translate(0 -0.12)", :style {:fill "#000"}}]]]]
)

(def ^:private pdf-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 14 16" :width "14px" :height "16px"} [:path {:d "M13.38,3.39a2.15,2.15,0,0,1,.44.68,2.17,2.17,0,0,1,.18.79V15.14a.87.87,0,0,1-.87.86H.87A.86.86,0,0,1,0,15.15H0V.86A.86.86,0,0,1,.86,0H9a2.17,2.17,0,0,1,.8.18,2.1,2.1,0,0,1,.7.43ZM9.33,1.22V4.57h3.43a1,1,0,0,0-.2-.36L9.71,1.41a1.07,1.07,0,0,0-.38-.2Zm3.5,13.64V5.72H9a.86.86,0,0,1-.87-.85h0V1.15h-7v13.7H12.83ZM8.15,9.56a6.3,6.3,0,0,0,.76.51A7.69,7.69,0,0,1,10,10c.89,0,1.43.15,1.61.44a.42.42,0,0,1,0,.47h0l0,0h0c0,.23-.25.34-.64.34a3.73,3.73,0,0,1-1-.18,6.44,6.44,0,0,1-1.19-.48,17.43,17.43,0,0,0-3.57.74c-.93,1.56-1.66,2.34-2.2,2.34a.58.58,0,0,1-.26-.06l-.22-.11-.05,0a.37.37,0,0,1-.06-.32,1.89,1.89,0,0,1,.52-.81,4.12,4.12,0,0,1,1.2-.86.13.13,0,0,1,.19,0s0,0,0,0a.05.05,0,0,1,0,0c.32-.51.64-1.09,1-1.76a13.19,13.19,0,0,0,1-2.34,6.87,6.87,0,0,1-.28-1.42A3.12,3.12,0,0,1,6,4.93c.07-.24.19-.36.38-.36h.21a.43.43,0,0,1,.32.14.75.75,0,0,1,.08.6.16.16,0,0,1,0,.07.22.22,0,0,1,0,.08v.26a11,11,0,0,1-.13,1.72A4.49,4.49,0,0,0,8.15,9.56ZM2.9,13.23a4.13,4.13,0,0,0,1.25-1.41,4.74,4.74,0,0,0-.8.75A3,3,0,0,0,2.9,13.23ZM6.53,5a2.52,2.52,0,0,0,0,1.19s0-.17.06-.4c0,0,0-.14.06-.38a.16.16,0,0,1,0-.07v0h0A.5.5,0,0,0,6.54,5s0,0,0,0ZM5.4,10.91A14.41,14.41,0,0,1,8,10.19l-.12-.08L7.72,10A4.72,4.72,0,0,1,6.56,8.42a11,11,0,0,1-.75,1.76l-.41.74Zm5.89-.14A2.18,2.18,0,0,0,10,10.56a3.63,3.63,0,0,0,1.13.25h.16s0,0,0,0Z", :transform "translate(0 0)", :style {:fill "#224867"}}]])

(def ^:private print-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 23 23" :height "23px" :width "23px"} [:path {:d "M19.51,6.43H3.41A3.65,3.65,0,0,0,0,10.26v7.67h4.6V23h13.8V17.93H23V10.26A3.65,3.65,0,0,0,19.51,6.43ZM16.06,20.48H6.86V14.09h9.2Zm3.45-8.94a1.29,1.29,0,1,1,1.14-1.41.57.57,0,0,1,0,.13A1.22,1.22,0,0,1,19.51,11.54ZM18.36,0H4.56V5.15h13.8Z" :transform "translate(0.05 -0.04)" :style {:fill "#000"}}]])

(def ^:private save-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 23 23" :width "23px" :height "23px"} [:path {:d "M22.24,5.07,17.93.76A2.47,2.47,0,0,0,16.19,0H2.42A2.46,2.46,0,0,0,0,2.5H0V20.57A2.47,2.47,0,0,0,2.42,23H20.49A2.48,2.48,0,0,0,23,20.57h0V6.81a2.47,2.47,0,0,0-.72-1.74ZM11.46,19.75a3.29,3.29,0,1,1,3.28-3.28h0A3.28,3.28,0,0,1,11.46,19.75ZM16.39,4.12V9.28a.62.62,0,0,1-.62.62H3.86a.62.62,0,0,1-.62-.62h0V3.94a.62.62,0,0,1,.62-.62H15.59A.65.65,0,0,1,16,3.5l.18.18a.67.67,0,0,1,.18.44Z", :transform "translate(0.04 -0.04)"}]])


(def ^:private share-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 25 25" :width "25px" :height "25px"}
   [:g {}
    [:path {:d "M0,10V20.43A2.74,2.74,0,0,0,2.83,23H20.08A2.75,2.75,0,0,0,23,20.43V10" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M15.87,8.12,11.46,0,7.05,8.12" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M11.46,3v9.92" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]]])

(def ^:private settings-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 16 16" :width "16px" :height "16px"} [:path {:d "M6.09,16h0a8.16,8.16,0,0,1-2.38-1,2.05,2.05,0,0,0-1-2.71A2,2,0,0,0,1,12.3,8.16,8.16,0,0,1,0,9.92,2.08,2.08,0,0,0,1.18,8.74a2,2,0,0,0,0-1.57A2.07,2.07,0,0,0,0,6.08,8.28,8.28,0,0,1,1,3.7a2,2,0,0,0,.83.18h0a2,2,0,0,0,.83-.18A2.05,2.05,0,0,0,3.7,1h0A8,8,0,0,1,6.08,0,2.08,2.08,0,0,0,7.26,1.18,2.11,2.11,0,0,0,8,1.32,2.07,2.07,0,0,0,9.92,0,8,8,0,0,1,12.3,1a2,2,0,0,0-.18.84,2.05,2.05,0,0,0,2.06,2.05A2,2,0,0,0,15,3.7a7.9,7.9,0,0,1,1,2.38,2.06,2.06,0,0,0,0,3.84,8,8,0,0,1-1,2.38,2.05,2.05,0,0,0-2.71,1,2.08,2.08,0,0,0,0,1.66,8,8,0,0,1-2.38,1,2.05,2.05,0,0,0-2.65-1.18A2,2,0,0,0,6.09,16ZM7.94,5.49a2.42,2.42,0,0,0-.88.17A2.57,2.57,0,0,0,5.47,8,2.51,2.51,0,0,0,8,10.51h.08a2.51,2.51,0,0,0,0-5Z"}]])

(def ^:private system-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 22.68 18.24" :height "24px" :width "24px"}
   [:g {}
    [:g {}
     [:line {:x1 "7.27", :y1 "2.26", :x2 "22.18", :y2 "2.26", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:line {:x1 "0.5", :y1 "2.26", :x2 "3.74", :y2 "2.26", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:circle {:cx "5.51", :cy "2.26", :r "1.76", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:line {:x1 "15.41", :y1 "9.12", :x2 "0.5", :y2 "9.12", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:line {:x1 "22.18", :y1 "9.12", :x2 "18.94", :y2 "9.12", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:circle {:cx "17.17", :cy "9.12", :r "1.76", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:line {:x1 "11.63", :y1 "15.97", :x2 "22.18", :y2 "15.97", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:line {:x1 "0.5", :y1 "15.97", :x2 "8.1", :y2 "15.97", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]
     [:circle {:cx "9.86", :cy "15.97", :r "1.76", :style {:fill "none", :stroke "#000", :stroke-linecap "round", :stroke-linejoin "round"}}]]]])

(def ^:private tools-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 16 16" :width "16px" :height "16px"} [:g {} [:path {:d "M15.7,2.51A4.14,4.14,0,0,1,14.83,7a3.92,3.92,0,0,1-4.78.66L9,8.85l.79.8.47-.48a.59.59,0,0,1,.84,0l0,0,3.81,3.93a.64.64,0,0,1,0,.89l-1.74,1.78a.6.6,0,0,1-.85,0l0,0L8.47,11.84a.64.64,0,0,1,0-.89l.43-.44-.75-.76L2.87,15.63a1.21,1.21,0,0,1-1.71,0l0,0L.7,15.18a1.27,1.27,0,0,1,0-1.77l6-5.14-4-4.08H1.46L0,1.79,1.17.59l2.4,1.5,0,1.26,4,4.14,1.17-1A4.13,4.13,0,0,1,9.18,1.2,3.94,3.94,0,0,1,13.52.3L10.93,2.9l2.18,2.23ZM2.14,14a.61.61,0,0,0-.86,0h0a.64.64,0,0,0,0,.89.61.61,0,0,0,.86,0v0A.64.64,0,0,0,2.14,14Z", :transform "translate(0 0)"}]]])

(def ^:private worksheet-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :xmlns:xlink "http://www.w3.org/1999/xlink", :viewbox "0 0 64 64" :width "64px" :height "64px"} [:defs [:clippath {:id "#b0fcdf0d-e45b-4431-b127-7af1b7933738" :transform "translate(-0.08 -0.1)"} [:rect {:x "17.67", :y "21.22", :width "29.15", :height "22.31", :style {:fill "none"}}]]] [:circle {:cx "32", :cy "32", :r "32", :style {:fill "#e6efc2"}}] [:g {} [:g {} [:path {:d "M32.23,57.68a26.3,26.3,0,0,1-18.5-7.56,25.79,25.79,0,0,1-5.6-8.2,25.56,25.56,0,0,1,0-20.06A26,26,0,0,1,22.05,8.13a26.35,26.35,0,0,1,28.67,5.53,25.65,25.65,0,0,1,5.61,8.2,25.56,25.56,0,0,1,0,20.06,25.65,25.65,0,0,1-5.61,8.2,26.25,26.25,0,0,1-18.49,7.56Z", :transform "translate(-0.08 -0.1)", :style {:fill "#b3c757"}}] [:path {:d "M32.23,7.1a25.26,25.26,0,0,0-17.8,7.27,24.77,24.77,0,0,0-5.38,7.88,24.57,24.57,0,0,0,0,19.28,24.77,24.77,0,0,0,5.38,7.88,25.26,25.26,0,0,0,17.8,7.27,25.43,25.43,0,0,0,9.8-2,25.12,25.12,0,0,0,8-5.32,24.8,24.8,0,0,0,5.39-7.88,24.57,24.57,0,0,0,0-19.28A24.8,24.8,0,0,0,50,14.37a25.12,25.12,0,0,0-8-5.32,25.43,25.43,0,0,0-9.8-2m0-2A27,27,0,0,1,59.38,31.89,27,27,0,0,1,32.23,58.68,27,27,0,0,1,5.08,31.89,27,27,0,0,1,32.23,5.1Z", :transform "translate(-0.08 -0.1)", :style {:fill "#fff"}}]] [:g {} [:g {:style {:clip-path "url(#b0fcdf0d-e45b-4431-b127-7af1b7933738)"}} [:g {} [:path {:d "M40.93,43.53h0l-21.1,0a2.1,2.1,0,0,1-1.76-1,2.65,2.65,0,0,1-.27-2.21l3.73-11.68a2.21,2.21,0,0,1,2-1.61h0l21.1,0a2.08,2.08,0,0,1,1.77,1,2.68,2.68,0,0,1,.26,2.21L43,41.92a2.2,2.2,0,0,1-2,1.61m0-1.26h0a1.06,1.06,0,0,0,1-.77l3.73-11.68a1.3,1.3,0,0,0-.12-1.07,1,1,0,0,0-.86-.5l-21.09,0h0a1,1,0,0,0-1,.77L18.85,40.68A1.3,1.3,0,0,0,19,41.75a1,1,0,0,0,.85.5Z", :transform "translate(-0.08 -0.1)", :style {:fill "#fff"}}] [:path {:d "M18.79,41.13H17.67l0-17.48a2.59,2.59,0,0,1,.63-1.72,2.09,2.09,0,0,1,1.53-.71h5.29a2,2,0,0,1,1.48.67l.73.77a1,1,0,0,0,.71.32l14.08,0a2,2,0,0,1,1.53.71,2.54,2.54,0,0,1,.63,1.72v2.19H43.18V25.44a1.21,1.21,0,0,0-.3-.83,1,1,0,0,0-.73-.34l-14.09,0a2,2,0,0,1-1.48-.67l-.73-.77a1,1,0,0,0-.71-.32H19.85a1,1,0,0,0-.73.34,1.26,1.26,0,0,0-.31.83Z", :transform "translate(-0.08 -0.1)", :style {:fill "#fff"}}]]]]]]
)


(def ^:private zoom-in-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 25 25" :width "25px" :height "25px"}
   [:g {}
    [:path {:d "M19.93,10a10,10,0,1,1-10-10h0A10,10,0,0,1,19.93,10Z" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M23,23l-5.12-5.18" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M10.18,5.84v9.08" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M5.76,10.26h9.08" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]]
   [:g {}
    [:path {:d "M19.93,10a10,10,0,1,1-10-10h0A10,10,0,0,1,19.93,10Z" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M23,23l-5.12-5.18" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M10.18,5.84v9.08" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M5.76,10.26h9.08" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]]])


(def ^:private zoom-out-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :viewbox "0 0 25 25" :width "25px" :height "25px"}
   [:g {}
    [:path {:d "M19.93,10a10,10,0,1,1-10-10h0A10,10,0,0,1,19.93,10Z" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M23,23l-5-5.77" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]
    [:path {:d "M5.76,10.26h9.08" :transform "translate(1.04 0.96)" :style {:fill "none" :stroke "#000" :stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2px"}}]]])

(def ^:private icons
  {; Module Icons
   :crown crown-icon
   :contain contain-icon
   :mortality mortality-icon
   :surface surface-icon

   ; Objective Icons
   :worksheet worksheet-icon

   ; System Icons
   :arrow arrow-icon
   :help help-icon
   :manual manual-icon
   :print print-icon
   :pdf pdf-icon
   :save save-icon
   :share share-icon
   :settings settings-icon
   :system system-icon
   :tools tools-icon
   :zoom-in zoom-in-icon
   :zoom-out zoom-out-icon})

;; Public Component

(defn icon
  "Returns the corresponding icon. Can pass in a map, string, or keyword.

  Examples:
  ```clojure
  [icon :zoom-in]
  [icon \"zoom-in\"]
  [icon {:icon-name \"zoom-in\"}]
  ```"
  [icon-name]
  (if (map? icon-name)
    (icon (:icon-name icon-name))
    (get icons (cond
                 (keyword? icon-name)
                 icon-name

                 (string? icon-name)
                 (keyword icon-name)

                 :else
                 nil))))
