import glob,os
from svgelements import SVG,Shape,Path,Color
D=os.path.dirname(os.path.abspath(__file__))+"/"
A=Color("#F5A623"); out=[]
for f in sorted(glob.glob(D+"*.svg")):
    k=os.path.basename(f)[:-4]; tr=[]
    for e in SVG.parse(f).elements():
        if not isinstance(e,Shape) or isinstance(e,SVG): continue
        if e.values.get('r')=='46': continue
        p=Path(e); p.reify(); d=p.d()
        if not d: continue
        w=e.implicit_stroke_width
        fill=e.fill
        rel=0 if (fill is None or fill.value is None) else (2 if fill==A else 1)
        tr.append(f'T("{d}", {w:.2f}f, {rel})')
    out.append(f'        "{k}" to listOf(\n            '+",\n            ".join(tr)+",\n        ),")
kt='''package com.cosmica.cafeteria.ui

// Archivo generado a partir de los SVG de los dibujos (viewBox 48x48). No editar a mano.
// relleno: 0 = sin relleno, 1 = color de la tarjeta (tapa lo de atrás), 2 = color del trazo.

internal class T(val d: String, val ancho: Float, val relleno: Int)

internal val TRAZOS_DIBUJOS: Map<String, List<T>> = mapOf(
'''+"\n".join(out)+"\n)\n"
open(D+"../app/src/main/java/com/cosmica/cafeteria/ui/DibujosDatos.kt","w").write(kt)
