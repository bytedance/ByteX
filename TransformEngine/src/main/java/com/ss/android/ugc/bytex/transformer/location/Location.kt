package com.ss.android.ugc.bytex.transformer.location

/**
 * Created by yangzhiqian on 2019-12-10<br/>
 */
sealed class Location {
    abstract fun getLocation(): String
    abstract fun getType(): Int

    open class FileLocation(val path: String, val parentLocation: FileLocation? = null) : Location() {
        override fun getLocation(): String = if (parentLocation == null) path else "${parentLocation.getLocation()}/$path"
        override fun getType(): Int = TYPE_FILE
    }

    class ZipEntryLocation(val fileLocation: Location, path: String) : FileLocation(path) {
        override fun getLocation(): String = "${fileLocation.getLocation()}!${super.getLocation()}"
        override fun getType(): Int = TYPE_ZIP_ENTRY
    }

    class ClassLocation(val fileLocation: FileLocation, val className: String, val sourceFile: String) : Location() {
        override fun getLocation(): String = "${fileLocation.getLocation()}:$className"
        override fun getType(): Int = TYPE_CLASS
    }

    class SuperClassLocation(val classLocation: ClassLocation, val className: String) : Location() {
        override fun getLocation(): String = "${classLocation.getLocation()}:$className"
        override fun getType(): Int = TYPE_CLASS_SUPER_CLASS
    }

    class ClassAnnotationLocation(val classLocation: ClassLocation, val annotationType: String) : Location() {
        override fun getLocation(): String = "${classLocation.getLocation()}@$annotationType"
        override fun getType(): Int = TYPE_CLASS_ANNOTATION
    }

    class ClassFieldLocation(val classLocation: ClassLocation, val fieldName: String, val fieldType: String) : Location() {
        override fun getLocation(): String = "${classLocation.getLocation()} $fieldType $fieldName"
        override fun getType(): Int = TYPE_CLASS_FIELD
    }

    class ClassFieldAnnotationLocation(val classFieldLocation: ClassFieldLocation, val annotationType: String) : Location() {
        override fun getLocation(): String = "${classFieldLocation.getLocation()}@$annotationType"
        override fun getType(): Int = TYPE_CLASS_FIELD_ANNOTATION
    }

    class ClassMethodLocation(val classLocation: ClassLocation, val methoddName: String, val methodDesc: String) : Location() {
        override fun getLocation(): String = "${classLocation.getLocation()} $methoddName$methodDesc"
        override fun getType(): Int = TYPE_CLASS_METHOD
    }

    class ClassMethodAnnotationLocation(val classMethodLocation: ClassMethodLocation, val annotationType: String) : Location() {
        override fun getLocation(): String = "${classMethodLocation.getLocation()}@$annotationType"
        override fun getType(): Int = TYPE_CLASS_METHOD_ANNOTATION
    }

    class ClassMethodInstructionLocation(val classMethodLocation: ClassMethodLocation, val instruction: String, val linenumber: Int) : Location() {
        override fun getLocation(): String = "${classMethodLocation.getLocation()}(${classMethodLocation.classLocation.sourceFile}:$linenumber) $instruction"
        override fun getType(): Int = TYPE_CLASS_METHOD_INSTRUCTION
    }

    companion object {
        val ROOT = FileLocation("")
        const val TYPE_FILE = 1
        const val TYPE_ZIP_ENTRY = 2

        const val TYPE_CLASS = 100
        const val TYPE_CLASS_SUPER_CLASS = 101
        const val TYPE_CLASS_ANNOTATION = 102

        const val TYPE_CLASS_FIELD = 200
        const val TYPE_CLASS_FIELD_ANNOTATION = 201


        const val TYPE_CLASS_METHOD = 300
        const val TYPE_CLASS_METHOD_ANNOTATION = 301
        const val TYPE_CLASS_METHOD_INSTRUCTION = 302
    }
}