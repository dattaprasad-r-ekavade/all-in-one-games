package com.datathecodie.witgrid.engine.core

/** Zero-based row-major cell index; the owning [Grid] checks its upper bound. */
@JvmInline
value class Cell(val index: Int) {
    init {
        require(index >= 0) { "Cell index must not be negative" }
    }
}
