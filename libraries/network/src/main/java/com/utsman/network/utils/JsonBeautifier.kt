/*
 * Created by Muhammad Utsman on 28/11/20 3:54 PM
 * Copyright (c) 2020 . All rights reserved.
 */

package com.utsman.network.utils

import com.squareup.moshi.JsonReader
import com.squareup.moshi.Types
import com.utsman.abstraction.dimanual.moduleOf
import com.utsman.network.di.moshi
import okio.Buffer

class JsonBeautifier {

    private val moshiModule by moduleOf(moshi)
    private val buffer = Buffer()
    private val adapter = moshiModule.adapter(Any::class.java).indent("    ")

    fun fromString(source: String): String {
        val reader = JsonReader.of(buffer.writeUtf8(source))
            .apply {
                isLenient = true
            }
        val data = reader.readJsonValue()
        return adapter.toJson(data)
    }

    fun <T: Any>fromAny(source: Any, type: Class<T>): String {
        val anyAdapter = moshiModule.adapter(type)
        val beautyResult = anyAdapter.toJson(source as T)
        return fromString(beautyResult)
    }

    fun <T: Any>toAny(string: String, type: Class<T>): T? {
        val moshiAdapter = moshiModule.adapter(type)
        return moshiAdapter.fromJson(string)
    }

    fun <T: Any>toAnyList(string: String, type: Class<T>): List<T>? {
        val typeParam = Types.newParameterizedType(List::class.java, type)
        val adapter = moshiModule.adapter<List<T>>(typeParam)
        return adapter.fromJson(string)
    }

    fun <T: Any>fromAnyList(source: List<T>, type: Class<T>): String {
        val typeParam = Types.newParameterizedType(List::class.java, type)
        val adapter = moshiModule.adapter<List<T>>(typeParam)
        return adapter.toJson(source)
    }

}