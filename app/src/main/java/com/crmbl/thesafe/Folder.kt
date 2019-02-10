package com.crmbl.thesafe

import com.beust.klaxon.Json

data class Folder(
    @Json(name = "Name")
    var name : String,
    @Json(name = "Folders")
    var folders : MutableList<Folder>,
    @Json(name = "Files")
    var files : MutableList<File>,
    @Json(ignored = true)
    var previous : Folder? = null
)