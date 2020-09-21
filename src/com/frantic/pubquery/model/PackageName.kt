package com.frantic.pubquery.model

import com.google.gson.annotations.SerializedName

data class PackageName(
        @SerializedName("package")
        val packageName: String
)