package com.datathecodie.witgrid.domain.config

interface Config {
    fun boolean(key: String): Boolean

    fun int(key: String): Int
}

object DefaultConfig : Config {
    override fun boolean(key: String): Boolean = ConfigKeys.booleanDefault(key)

    override fun int(key: String): Int = ConfigKeys.intDefault(key)
}
