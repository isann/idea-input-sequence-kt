package xyz.zono.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import java.util.*

class InputSequenceAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val startIndexStr = Messages.showInputDialog(
            project,
            "Input start number.",
            "InputSequence",
            null
        )
        if (startIndexStr != null && startIndexStr.isEmpty()) {
            return
        }
        val startIndex: Int = try {
            startIndexStr!!.toInt()
        } catch (e1: NumberFormatException) {
            return
        }
        val digit = startIndexStr.length

        val editor =
            FileEditorManager.getInstance(project).selectedTextEditor ?: return

        // カレントのキャレットから連番にする
        // カレントキャレットの位置を取得する
        val allCarets = editor.caretModel.allCarets
        val map: HashMap<String, Int> = HashMap<String, Int>()
        for (caret in allCarets) {
            val currentCaret = caret.offset
            map[currentCaret.toString()] = currentCaret
        }
        val document = editor.document
        val documentText = document.text
        val textLines = documentText.split("\r\n").toTypedArray()

        // ドキュメントを読み込み、文字列を StringBuffer で組み立てる
        val contents: String = try {
            val sb = StringBuilder()
            var counter = 0
            var editNum = startIndex
            for (currentLine in textLines) {
                val chars = currentLine.toCharArray()
                // 文字列を検索し、挿入位置を特定しその前に番号をいれていく
                for (character in chars) {
                    // zero padding
                    if (map.containsKey(counter.toString())) {
                        if (editNum.toString().length < digit) {
                            val paddingCnt =
                                digit - editNum.toString().length
                            for (i in 0 until paddingCnt) {
                                sb.append(0)
                            }
                        }
                        sb.append(editNum)
                        sb.append(character)
                        editNum++
                    } else {
                        sb.append(character)
                    }
                    counter++
                }
                // zero padding
                if (map.containsKey(counter.toString())) {
                    if (editNum.toString().length < digit) {
                        val paddingCnt =
                            digit - editNum.toString().length
                        for (i in 0 until paddingCnt) {
                            sb.append(0)
                        }
                    }
                    sb.append(editNum)
                    editNum++
                }
                sb.append("\n")
                counter++
            }
            sb.toString()
        } catch (e1: Exception) {
            return
        }
        val readRunner = Runnable { document.setText(contents) }
        ApplicationManager.getApplication().invokeLater {
            CommandProcessor.getInstance()
                .executeCommand(
                    project,
                    {
                        ApplicationManager.getApplication()
                            .runWriteAction(readRunner)
                    }, "DiskRead", null
                )
        }
    }
}
