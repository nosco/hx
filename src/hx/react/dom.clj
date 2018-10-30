(ns hx.react.dom)

(def dom-elements
  '[a
    abbr
    address
    area
    article
    aside
    audio
    b
    base
    bdi
    bdo
    big
    blockquote
    body
    br
    button
    canvas
    caption
    cite
    code
    col
    colgroup
    data
    datalist
    dd
    del
    dfn
    div
    dl
    dt
    em
    embed
    fieldset
    figcaption
    figure
    footer
    form
    input
    textarea
    option
    h1
    h2
    h3
    h4
    h5
    h6
    head
    header
    hr
    html
    i
    iframe
    img
    ins
    kbd
    keygen
    label
    legend
    li
    link
    main
    map
    mark
    marquee
    menu
    menuitem
    meta
    meter
    nav
    noscript
    object
    ol
    optgroup
    output
    p
    param
    pre
    progress
    q
    rp
    rt
    ruby
    s
    samp
    script
    section
    select
    small
    source
    span
    strong
    style
    sub
    summary
    sup
    table
    tbody
    td
    tfoot
    th
    thead
    time
    title
    tr
    track
    u
    ul
    var
    video
    wbr

    ;; svg
    circle
    ellipse
    g
    line
    path
    polyline
    rect
    svg
    text
    defs
    linearGradient
    polygon
    radialGradient
    stop
    tspan])

(defn make-dom-factory [sym]
  `(def ~sym (hx.react/factory ~(str sym))))

(defmacro make-factories []
  `(do ~@(for [el dom-elements]
           (make-dom-factory el))))

(defn interweave [c f]
  (reduce (fn [c' x]
            (conj c' x (f x))) [] c))

(defmacro open [& body]
  `(let ~(interweave dom-elements (fn [e] (symbol (str "hx.react.dom/" e))))
     ~@body))

#_(macroexpand '(open (dom "hi")))
