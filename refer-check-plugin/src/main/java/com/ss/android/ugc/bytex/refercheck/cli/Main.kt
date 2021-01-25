package com.ss.android.ugc.bytex.refercheck.cli

import com.ss.android.ugc.bytex.common.exception.GlobalWhiteListManager
import com.ss.android.ugc.bytex.common.graph.GraphBuilder
import com.ss.android.ugc.bytex.common.utils.FileHandler
import com.ss.android.ugc.bytex.common.utils.MethodMatcher
import com.ss.android.ugc.bytex.common.visitor.GenerateGraphClassVisitor
import com.ss.android.ugc.bytex.common.white_list.WhiteList
import com.ss.android.ugc.bytex.refercheck.DefaultCheckIssueReceiver
import com.ss.android.ugc.bytex.refercheck.InaccessibleNode
import com.ss.android.ugc.bytex.refercheck.log.ErrorLogGenerator
import com.ss.android.ugc.bytex.refercheck.visitor.ReferCheckClassVisitor
import org.objectweb.asm.ClassReader
import java.io.File
import java.util.*
import java.util.function.Consumer

object Main {
    const val USAGE = ""


    @JvmStatic
    fun main(args: Array<String>) {
        val programInputs = listOf<File>()
        val libraryInputs = listOf<File>()
        val variantName: String = "variantName"

        val checkResult = checkReference(programInputs, libraryInputs, null, false, emptyList())
        println(ErrorLogGenerator(
                null,
                null,
                variantName,
                null,
                checkResult.keepByWhiteList
        ).generate())
        println("===============================")
        println(ErrorLogGenerator(
                null,
                null,
                variantName,
                null,
                checkResult.inaccessibleNodes
        ).generate())
    }

    @JvmStatic
    fun checkReference(programInputs: Collection<File>, libraryInputs: Collection<File>, whiteList: WhiteList?, checkInaccessOverrideMethodStrictly: Boolean, blockMethodList: List<String>): CheckResult {
        var startTime = System.currentTimeMillis()
        println("programInputs:\n\t${programInputs.map { it.absolutePath }.joinToString("\n\t")}")
        println("libraryInputs:\n\t${libraryInputs.map { it.absolutePath }.joinToString("\n\t")}")
        val graphBuilder = GraphBuilder()
        val classFileData = Collections.synchronizedList(LinkedList<FileHandler.FileData>())
        FileHandler(programInputs + libraryInputs).execute {
            if (it.name.endsWith(".class")) {
                classFileData.add(it)
            }
        }
        println("[QuickReferCheck] readInputs[${System.currentTimeMillis() - startTime}ms]")
        startTime = System.currentTimeMillis()
        classFileData.parallelStream().forEach {
            try {
                ClassReader(it.bytes).accept(GenerateGraphClassVisitor(it.file in libraryInputs, graphBuilder), 0)
            } catch (e: Exception) {
                e.printStackTrace()
                if (!GlobalWhiteListManager.INSTANCE.shouldIgnore(it.name)) {
                    throw RuntimeException(it.file.absolutePath + it.path, e)
                }
            }
        }
        val graph = graphBuilder.build()
        println("[QuickReferCheck] build graph[${System.currentTimeMillis() - startTime}ms]")
        startTime = System.currentTimeMillis()
        val keepByWhiteList = Collections.synchronizedList(LinkedList<InaccessibleNode>())
        val receiver = DefaultCheckIssueReceiver(whiteList, Consumer {
            keepByWhiteList.add(it)
        })
        val blockMethodListMatchers = blockMethodList.map { MethodMatcher(it) }.toList()
        classFileData.parallelStream().forEach {
            try {
                ClassReader(it.bytes).accept(ReferCheckClassVisitor(checkInaccessOverrideMethodStrictly, receiver, graph, blockMethodListMatchers), 0)
            } catch (e: Exception) {
                e.printStackTrace()
                if (!GlobalWhiteListManager.INSTANCE.shouldIgnore(it.name)) {
                    throw RuntimeException(it.file.absolutePath + it.path, e)
                }
            }
        }
        println("[QuickReferCheck] check reference[${System.currentTimeMillis() - startTime}ms]")
        return CheckResult(keepByWhiteList, receiver.inaccessibleNodes)
    }

    class CheckResult(val keepByWhiteList: List<InaccessibleNode>, val inaccessibleNodes: List<InaccessibleNode>)
}