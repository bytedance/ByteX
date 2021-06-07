package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.QualifiedContent
import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableSet
import java.io.File
import java.io.Serializable

 open class QualifiedContentImpl(private val name: String,
                                         private val file: File,
                                         contentTypes: Set<QualifiedContent.ContentType>,
                                         scopes: MutableSet<in QualifiedContent.Scope>) : QualifiedContent, Serializable {
    private val contentTypes: Set<QualifiedContent.ContentType> = ImmutableSet.copyOf(contentTypes)
    private val scopes: MutableSet<in QualifiedContent.Scope?> = ImmutableSet.copyOf(scopes)

    protected constructor(qualifiedContent: QualifiedContent) : this(qualifiedContent.name, qualifiedContent.file, qualifiedContent.contentTypes, qualifiedContent.scopes)

    override fun getName(): String {
        return name
    }

    override fun getFile(): File {
        return file
    }

    override fun getContentTypes(): Set<QualifiedContent.ContentType> {
        return contentTypes
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? {
        return scopes
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("file", file)
                .add("contentTypes", contentTypes)
                .add("scopes", scopes)
                .toString()
    }
}