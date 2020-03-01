package com.ss.android.ugc.bytex.shrinkR.source

import com.ss.android.ugc.bytex.common.utils.Utils
import com.ss.android.ugc.bytex.shrinkR.source.RFilesRewriter.BrewResult.Companion.CODE_NOT_FOUND
import com.ss.android.ugc.bytex.shrinkR.source.RFilesRewriter.BrewResult.Companion.CODE_SIZE_LIMIT
import com.ss.android.ugc.bytex.shrinkR.source.RFilesRewriter.BrewResult.Companion.CODE_SUCCEED
import com.ss.android.ugc.bytex.shrinkR.source.code.brewJava
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

/**
 * Created by yangzhiqian on 2020-02-11<br/>
 */

internal const val LIMITS = 10000

fun main(args: Array<String>) {
    if (args.size < 4) {
        println("Usage:java -jar xxx.jar \$R.java \$newClassName \$limit \$AssignType(value:inherit)")
        exitProcess(1)
    }
    val startTime = System.currentTimeMillis()
    File(args[0]).apply {
        File(parent, "${args[1]}.java").writeText(brewJava(readLines(), args[1], Integer.parseInt(args[2]), true, parseFormString(args[3])))
    }
    println("Run Time:[${System.currentTimeMillis() - startTime}]")
}

internal class RFilesRewriter : Callable<RFilesRewriter.BrewResult> {
    var rFileDir: File? = null
    var rBackupDir: File? = null
    var packageName: String? = null
    var className: String? = null
    var limit = LIMITS
    var verifyParse = false
    var assignType = AssignType.AssignValue
    var whiteList: RFileWhiteList? = null

    override fun call(): BrewResult {
        return BrewResult(rFileDir!!.path,
                getRJavaFile().absolutePath,
                getRJavaBackUpFile().absolutePath,
                System.currentTimeMillis(),
                brewJava(),
                System.currentTimeMillis())
    }

    fun brewJava(): Int {
        val rFile = getRJavaFile()
        if (!rFile.exists()) {
            println("RFilesRewriter[Skip(File Not Found})]:${rFile.absolutePath}")
            return CODE_NOT_FOUND
        }
        assert(rFile.isFile)
        val lines = rFile.readLines()
        if (lines.size < limit) {
            println("RFilesRewriter[Skip(${lines.size})]:${rFile.absolutePath}")
            return CODE_SIZE_LIMIT
        }
        rFile.copyTo(getRJavaBackUpFile(), true)
        rFile.writeText(brewJava(lines, className!!, limit, verifyParse, assignType, whiteList))
        rFile.copyTo(getBrewedRJavaFile(), true)
        return CODE_SUCCEED
    }

    private fun getRJavaFile(): File = File(rFileDir, "${Utils.replaceDot2Slash(packageName)}/$className.java")
    private fun getRJavaBackUpFile(): File = File(rBackupDir, "${Utils.replaceDot2Slash(packageName)}/$className.java.backup")
    private fun getBrewedRJavaFile(): File = File(rBackupDir, "${Utils.replaceDot2Slash(packageName)}/$className.java")

    class BrewResult(val fFileDirPath: String,
                     val rFilePath: String,
                     val backupRFilePath: String?,
                     val startTime: Long,
                     val resultCode: Int,
                     val endTime: Long) {
        companion object {
            const val CODE_SUCCEED = 0
            const val CODE_NOT_FOUND = 1
            const val CODE_SIZE_LIMIT = 2
        }
    }
}

