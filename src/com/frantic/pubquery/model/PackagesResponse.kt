package com.frantic.pubquery.model

data class PackagesResponse(
        val count: Int,
        val packages: ArrayList<PackageSnapshot>
)