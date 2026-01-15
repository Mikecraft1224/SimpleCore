package com.github.mikecraft1224

import org.slf4j.LoggerFactory

object Logger {
    val logger = LoggerFactory.getLogger(BuildConfig.MOD_ID)

    fun info(msg: String) {
        logger.info(msg)
    }

    fun warn(msg: String) {
        logger.warn(msg)
    }

    fun debug(msg: String) {
        logger.debug(msg)
    }

    fun error(msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.error(msg, throwable)
        } else {
            logger.error(msg)
        }
    }
}