package com.crmbl.thesafe

import com.beust.klaxon.Json

data class File(
    @Json(name = "OriginName")
    var originName : String = "",
    @Json(name = "UpdatedName")
    var updatedName : String = "",
    @Json(ignored = true)
    var type : String = "",
    @Json(ignored = true)
    var width : Int = 0,
    @Json(ignored = true)
    var height : Int = 0,
    @Json(ignored = true)
    var path : String = ""
)