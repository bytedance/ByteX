package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.google.common.base.Joiner
import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableMap
import java.io.File

class DirectoryInputImpl(
        name: String,
        file: File,
        contentTypes: Set<QualifiedContent.ContentType>,
        scopes: MutableSet<in QualifiedContent.Scope>,
        changedFiles: Map<File, Status> = emptyMap()) : QualifiedContentImpl(name, file, contentTypes, scopes), DirectoryInput {
    private val changedFiles: Map<File, Status>
    override fun getChangedFiles(): Map<File, Status> {
        return changedFiles
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("file", file)
                .add("contentTypes", Joiner.on(',').join(contentTypes))
                .add("scopes", Joiner.on(',').join(scopes))
                .add("changedFiles", changedFiles)
                .toString()
    }

    init {
        this.changedFiles = ImmutableMap.copyOf(changedFiles)
    }
}
