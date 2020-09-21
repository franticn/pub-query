package com.frantic.pubquery

import com.frantic.pubquery.api.BASE_PRIVATE_URL
import com.frantic.pubquery.api.BASE_PUB_URL
import com.frantic.pubquery.api.PublicApi
import com.frantic.pubquery.api.SEARCH_BY_KEYWORD
import com.frantic.pubquery.model.BaseResponse
import com.frantic.pubquery.model.PackagesResponse
import com.frantic.pubquery.model.QueryVersionsResponse
import com.frantic.pubquery.model.SearchNameResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


fun main() {
    val keyword = "tuya"
    val searchResult = search(keyword)
}

/**
 * 根据关键字搜索pub库
 */
@Throws(java.lang.Exception::class)
fun search(keyword: String): BaseResponse<PackagesResponse>? = (BASE_PRIVATE_URL + SEARCH_BY_KEYWORD + keyword).request<BaseResponse<PackagesResponse>>()

/**
 * 根据关键字搜索库中对应的pub库的名字
 */
@Throws(java.lang.Exception::class)
fun searchPackageNames(keyword: String) : SearchNameResponse? = (PublicApi.SEARCH_BY_KEYWORD + keyword).request<SearchNameResponse>()

@Throws(java.lang.Exception::class)
fun queryVersionsForPackage(packageName:String): QueryVersionsResponse? = (PublicApi.QUERY_VERSIONS + packageName).request<QueryVersionsResponse>()

@Throws(java.lang.Exception::class)
inline fun <reified R> String.request(): R? = this.getResponse(1)?.let {
    getObjectWithType<R>(it)
}

/**
 * 获取响应报文，重试[retryCount]次
 */
@Throws(java.lang.Exception::class)
fun String.getResponse(retryCount: Int): String? {
    var currentRetryCount = 0
    var exception: Exception? = null
    while (currentRetryCount <= retryCount) {
        try {
            (URL(this).openConnection() as HttpURLConnection).run {
                connectTimeout = 5000
                readTimeout = 5000
                if (responseCode == 200) {
                    val result = inputStream.readBytes().toString(Charsets.UTF_8).apply {
                        println("getResponse result => $this")
                    }
                    return result
                } else {
                    currentRetryCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exception = e
            if (e is SocketTimeoutException) {
                currentRetryCount++
            } else throw  e
        }
    }
    exception?.let {
        throw  exception
    }
    return null
}

private fun InputStream.readBytes(): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available()))
    copyTo(buffer)
    return buffer.toByteArray()
}


inline fun <reified T> getObjectWithType(json: String) = Gson().fromJson<T>(json, genericType<T>())

inline fun <reified T> genericType() = object : TypeToken<T>() {}.type

inline fun <T, R> T.runSafely(block: (T) -> R) = try {
    block(this)
} catch (e: Exception) {
    e.printStackTrace()
    null
}


