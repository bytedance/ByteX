package com.ss.android.ugc.bytex.common.hook

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform

internal class FakeTransform(
        private val name: String,
        private val inputTypes: Set<QualifiedContent.ContentType>,
        private val scopes: MutableSet<in QualifiedContent.Scope>
) : Transform() {
    override fun getName(): String = name

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = inputTypes

    override fun isIncremental(): Boolean = false

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = scopes
}