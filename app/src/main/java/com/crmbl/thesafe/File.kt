package com.crmbl.thesafe

data class File(
    var originName : String = "",
    var updatedName : String = "",
    var width : String = "",
    var height : String = "",
    var type : String = "",
    var path : String = "",
    var frozen : Boolean = false
)