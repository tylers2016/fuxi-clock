package com.fuxizhong.app.data

import com.google.gson.annotations.SerializedName

data class PentadItem(
    @SerializedName("id")
    val id: Int = 1,

    @SerializedName("filename")
    val filename: String = "hou_01.webp",

    @SerializedName(value = "solarTerm", alternate = ["solar_term"])
    val solarTerm: String = "立春",

    @SerializedName(value = "houOrder", alternate = ["hou_order"])
    val houOrder: String = "初候",

    @SerializedName(value = "houName", alternate = ["hou_name"])
    val houName: String = "东风解冻",

    @SerializedName(value = "fullTitle", alternate = ["full_title"])
    val fullTitle: String = "立春 初候 · 东风解冻"
)
