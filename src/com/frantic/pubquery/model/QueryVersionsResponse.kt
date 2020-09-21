package com.frantic.pubquery.model

data class QueryVersionsResponse(
        val name:String,
        val latestStableVersion:String,
        val versions:List<PackageVersionDetail>
)