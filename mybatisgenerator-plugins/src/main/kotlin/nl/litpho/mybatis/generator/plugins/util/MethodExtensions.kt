package nl.litpho.mybatis.generator.plugins.util

import org.mybatis.generator.api.dom.java.Method

fun Method.isGetter(): Boolean = name.startsWith("get") || name.startsWith("is")

fun Method.isSetter(): Boolean = name.startsWith("set")
