package com.frantic.pubquery.model

/**
 * 列表页面package的大致信息
 */
data class PackageSnapshot(
        // 名称
        val name: String,
        // tar列表 eg.["flutter","web","other"]
        val tags: MutableList<String>,
        // 最新版本号
        val latest: String,
        // 描述
        val description: String?,
        // 最新更新时间
        val updatedAt: String,
)