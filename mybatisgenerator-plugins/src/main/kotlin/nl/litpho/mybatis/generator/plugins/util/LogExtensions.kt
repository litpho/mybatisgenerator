package nl.litpho.mybatis.generator.plugins.util

import org.mybatis.generator.logging.Log
import org.mybatis.generator.logging.LogFactory

inline fun <reified T> createLogger(): Log = LogFactory.getLog(T::class.java)
