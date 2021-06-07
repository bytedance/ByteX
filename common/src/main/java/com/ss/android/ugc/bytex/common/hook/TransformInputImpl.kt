package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.google.common.base.MoreObjects

class TransformInputImpl(
        private val jarInputs: Collection<JarInput>,
        private val directoryInputs: Collection<DirectoryInput>) : TransformInput {
    override fun getJarInputs(): Collection<JarInput> {
        return jarInputs
    }

    override fun getDirectoryInputs(): Collection<DirectoryInput> {
        return directoryInputs
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("jarInputs", jarInputs)
                .add("folderInputs", directoryInputs)
                .toString()
    }

}