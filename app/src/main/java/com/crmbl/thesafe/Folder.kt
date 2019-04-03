package com.crmbl.thesafe

import com.beust.klaxon.Json

data class Folder(
    @Json(name = "Name")
    var name : String,
    @Json(name = "FullPath")
    var fullPath : String,
    @Json(name = "Files")
    var files : MutableList<File>
)