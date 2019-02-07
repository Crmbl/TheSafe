package com.crmbl.thesafe

data class jFolder(
    var name : String,
    var folders : MutableList<jFolder>,
    var files : MutableList<jFile>
)