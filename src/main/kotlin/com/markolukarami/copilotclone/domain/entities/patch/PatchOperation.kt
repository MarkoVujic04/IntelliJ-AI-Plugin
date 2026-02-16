package com.markolukarami.copilotclone.domain.entities.patch

data class PatchOperation(
    val type: String,

    val oldName: String? = null,
    val newName: String? = null,

    val methodName: String? = null,

    val methodSource: String? = null,

    val search: String? = null,
    val replace: String? = null,

    val newContent: String? = null,

    val position: String? = null,
    val afterText: String? = null,
    val statement: String? = null
)
