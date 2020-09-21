package com.frantic.pubquery

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.util.PlatformIcons
import java.awt.EventQueue
import java.io.StringReader
import java.util.concurrent.Executors
import java.util.concurrent.Future

class YamlCompletionContributor : CompletionContributor() {
    //是否显示结果
    private var showQueryResult = false

    //可用的文件类型
    private val enableFileType = arrayOf(".yaml", ".yml")

    //线程池
    private val threadPool = Executors.newSingleThreadExecutor()

    //元素列表
    private var elementList = ArrayList<Pair<String, String>>()
    private var runningQueryThread: Future<*>? = null
    private var lastCurrentLineContent = ""

    //当前要插入的插件名称
    private var injectPackageName = ""

    override fun beforeCompletion(context: CompletionInitializationContext) {
        showQueryResult = enableFileType.any { context.file.name.endsWith(it) }
        if (showQueryResult) {
            showQueryResult = isInDependenciesBlock(context)
        } else {
            cancelQuery()
        }
    }

    override fun duringCompletion(context: CompletionInitializationContext) {
        val lineStartPosition = context.editor.caretModel.visualLineStart
        val lineEndPosition = context.editor.caretModel.visualLineEnd
        val currentLineStr = context.editor.document.getText(TextRange(lineStartPosition, lineEndPosition)).replace("\n".toRegex(), "").trim()
        super.duringCompletion(context)
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (showQueryResult) {
            val lineStartPosition = parameters.editor.caretModel.visualLineStart
            val lineEndPosition = parameters.editor.caretModel.visualLineEnd
            val currentLineContent = parameters.editor.document.getText(TextRange(lineStartPosition, lineEndPosition)).replace("\n".toRegex(), "").trim()
            println("fillCompletionVariants currentLineContent = $currentLineContent")
            if (elementList.isNotEmpty()) {
                if (currentLineContent.contains(lastCurrentLineContent)) {
                    elementList.forEach { e -> result.addElement(e.toLookupElement(currentLineContent.contains(":"))) }
                } else {
                    cancelQuery()
                    result.stopHere()
                }
                elementList.clear()
                showQueryResult = false
            } else {
                if (currentLineContent.length < 3 || currentLineContent == lastCurrentLineContent) {
                    return
                }
                lastCurrentLineContent = currentLineContent
                startQuery(parameters.editor)
            }
        } else {
            if (elementList.isNotEmpty()) elementList.clear()
        }
        super.fillCompletionVariants(parameters, result)
    }


    // 当前位置是否在dependencies当中
    private fun isInDependenciesBlock(context: CompletionInitializationContext): Boolean {
        val lines = StringReader(context.editor.document.text).readLines()
        var currentLineNumber = context.editor.run { document.getLineNumber(caretModel.offset) }
        var braceStartCount = 0
        while (currentLineNumber >= 0 && currentLineNumber < lines.size) {
            val currentLine = lines[currentLineNumber]
            braceStartCount += currentLine.count { it == ':' }
            if (currentLine.let { it.contains("dependencies:") || it.contains("dev_dependencies:") }) {
                return true
            }
            currentLineNumber--
        }
        return false
    }

    // 查询
    private fun startQuery(editor: Editor) {
        lastCurrentLineContent.split("\\s+".toRegex()).let { lineContent ->
            if (lineContent.isNotEmpty()) {
                val keyword = lineContent.run { if (size == 1) first() else last() }
                runningQueryThread?.cancel(true)
                var task: Future<*>? = null
                task = threadPool.submit {
                    task?.let { task ->
                        val result = searchPackageNames(keyword)?.packages?.map {
                            Pair<String, String>(it.packageName, "")
                        } ?: arrayListOf()
                        if (!task.isCancelled) {
                            elementList.clear()
                            elementList.addAll(result)
                            EventQueue.invokeLater {
                                editor.project?.let { project ->
                                    AutoPopupController.getInstance(project)
                                            .autoPopupMemberLookup(editor, null)
                                }
                            }
                            runningQueryThread = null
                        }
                    }
                }.also { runningQueryThread = it }
            }
        }
    }

    private fun queryVersion(packageName: String, editor: Editor) {
        packageName.trim().let {
            if (it.isNotEmpty()) {
                val keyword = packageName
                runningQueryThread?.cancel(true)
                var task: Future<*>? = null
                task = threadPool.submit {
                    task?.let { task ->
                        val result = queryVersionsForPackage(keyword)?.versions?.map {
                            Pair<String, String>(it.version, "")
                        } ?: arrayListOf()
                        if (!task.isCancelled) {
                            elementList.clear()
                            elementList.addAll(result)
                            EventQueue.invokeLater {
                                editor.project?.let { project ->
                                    AutoPopupController.getInstance(project)
                                            .autoPopupMemberLookup(editor, null)
                                }
                            }
                            runningQueryThread = null
                        }
                    }
                }.also { runningQueryThread = it }
            }
        }
    }

    // 取消查询
    private fun cancelQuery() {
        runningQueryThread?.let { if (!it.isCancelled && !it.isDone) it.cancel(true) }
        elementList.clear()
        showQueryResult = false
    }

    private fun Pair<String, String>.toLookupElement(contains: Boolean) = LookupElementBuilder.create((if (contains) first else "$first: ")).bold()
            .withIcon(PlatformIcons.PACKAGE_ICON).withInsertHandler { context, item ->
                val lineStartPosition = context.editor.caretModel.visualLineStart
                val lineEndPosition = context.editor.caretModel.visualLineEnd
                var currentLineContent = context.editor.document.getText(TextRange(lineStartPosition, lineEndPosition)).replace("\n".toRegex(), "")
                currentLineContent = currentLineContent.trimStart()
                var additionalIndex = 0
                run {
                    currentLineContent.forEachIndexed { index, char ->
                        if(!char.isWhitespace()){
                            additionalIndex = index
                            return@run
                        }
                    }
                }
                WriteCommandAction.runWriteCommandAction(context.project) {
                    println("additionalIndex  = $additionalIndex lineStartPosition = $lineStartPosition lineEndPosition = $lineEndPosition")
                    println("currentLineContent  = $currentLineContent")
                    // 替换
                    context.document.replaceString(lineStartPosition + 2 , lineEndPosition - 1, currentLineContent)
                    // 再次查询版本号
                    if (currentLineContent.endsWith(": ")) {
                        queryVersion(first, context.editor)
                    }
                }
            }
}