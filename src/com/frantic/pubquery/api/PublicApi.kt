package com.frantic.pubquery.api

// 默认的官方pub地址
const val BASE_PUB_URL = "https://pub.flutter-io.cn/"

object PublicApi {

    // 根据关键字搜索简要的信息
    const val SEARCH_BY_KEYWORD = BASE_PUB_URL + "api/search?q="

    //查询某一个package对应的version集合
    const val QUERY_VERSIONS = "${BASE_PUB_URL}api/documentation/"
}