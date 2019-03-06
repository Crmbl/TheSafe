package com.crmbl.thesafe

import com.beust.klaxon.Json

data class File(
    @Json(name = "OriginName")
    var originName : String = "",
    @Json(name = "UpdatedName")
    var updatedName : String = "",
    @Json(name = "Width")
    var width : String = "",
    @Json(name = "Height")
    var height : String = "",
    @Json(ignored = true)
    var type : String = "",
    @Json(ignored = true)
    var path : String = "",
    @Json(ignored = true)
    var frozen : Boolean = false
)