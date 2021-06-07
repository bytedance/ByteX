package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.google.common.base.Joiner
import com.google.common.base.MoreObjects
import java.io.File

class JarInputImpl(name: String,
                   file: File,
                   private val status: Status,
                   contentTypes: Set<QualifiedContent.ContentType>,
                   scopes: MutableSet<in QualifiedContent.Scope>) : QualifiedContentImpl(name, file, contentTypes, scopes), JarInput {


    constructor(qualifiedContent: QualifiedContent,
                status: Status) : this(qualifiedContent.name,qualifiedContent.file,status, qualifiedContent.contentTypes, qualifiedContent.scopes)

    override fun getStatus(): Status {
        return status
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("file", file)
                .add("contentTypes", Joiner.on(',').join(contentTypes))
                .add("scopes", Joiner.on(',').join(scopes))
                .add("status", status)
                .toString()
    }
}
