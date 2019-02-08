package com.crmbl.thesafe

import com.beust.klaxon.Json

data class File(
    @Json(name = "OriginName")
    var originName : String,
    @Json(name = "UpdatedName")
    var updatedName : String
)