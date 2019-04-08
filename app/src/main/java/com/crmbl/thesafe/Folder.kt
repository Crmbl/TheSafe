package com.crmbl.thesafe

data class Folder(
    var name : String,
    var fullPath : String,
    var files : MutableList<File>
)