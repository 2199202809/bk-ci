package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

import com.fasterxml.jackson.annotation.JsonProperty

data class AlternatePackage(
    @JsonProperty("id")
    val packageId: String,
    val range: String? = null
)
