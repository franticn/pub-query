package com.frantic.pubquery.view

import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW


class VersionSelectorDialog(dataList: DefaultListModel<String>, block: (item: String) -> Unit) : JDialog() {
    private lateinit var contentPane: JPanel
    private lateinit var buttonCancel: JButton
    private lateinit var list: JList<String>
    private lateinit var progressBar:JProgressBar
    init {
        setContentPane(contentPane)
        isModal = true
        rootPane.defaultButton = buttonCancel


        list.model = dataList
        list.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                block(list.selectedValue)
                dispose()
            }
        }

        buttonCancel.addActionListener {
            dispose()
        }
//        progressBar.isVisible = true

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                dispose()
            }
        })

        rootPane.registerKeyboardAction({ dispose() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW)
    }

    companion object {
        @JvmStatic
        fun show(dataList: DefaultListModel<String>, block: (item: String) -> Unit) {
            EventQueue.invokeLater {
                val dialog = VersionSelectorDialog(dataList, block)
                dialog.pack()
                val screenSize: Dimension = Toolkit.getDefaultToolkit().getScreenSize()
                val x: Int = (screenSize.width - dialog.width) / 2
                val y: Int = (screenSize.height - dialog.height) / 2
                dialog.setLocation(x, y)
                dialog.isVisible = true
            }
        }
    }

}