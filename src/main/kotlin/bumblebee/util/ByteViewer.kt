package bumblebee.util

import bumblebee.util.Converter.Companion.byteToHex
import java.awt.Component
import java.awt.Dialog
import java.awt.Font
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn


class ByteViewer(val byteArray : ByteArray) : JFrame() {

    private lateinit var table : JTable
    private var row = 0
    private var col = 0

    init {
        build()
        title = "Byte Viewer"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        setSize(800, 600)
        setDefaultLookAndFeelDecorated(true)
        isVisible = true
    }

    private fun build() {

        val menuBar = buildMenuBar()

        val header = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E")
        val contents = extract()
        table = object : JTable(contents, header){
            override fun isCellEditable(rowIndex: Int, colIndex: Int): Boolean {
                return false
            }
        }
        val rowTable =  RowNumberTable(table)
        val scrollPane = JScrollPane(table)
        scrollPane.setRowHeaderView(rowTable)

        this.jMenuBar = menuBar
        this.add(scrollPane)

        find("FF")
    }

    private fun buildMenuBar() : JMenuBar{
        val menuBar = JMenuBar()

        val findDialog = buildDialog()

        val fileMenu = JMenu("File")
        val toolMenu = JMenu("Tool")
        val aboutMenu = JMenu("About")

        val openMenuItem = JMenuItem("open")
        val findMenuItem = JMenuItem("find")
        findMenuItem.addActionListener {
            findDialog.isVisible = true
        }
        val contactMenuItem = JMenuItem("contact")

        fileMenu.add(openMenuItem)
        toolMenu.add(findMenuItem)
        aboutMenu.add(contactMenuItem)

        menuBar.add(fileMenu)
        menuBar.add(toolMenu)
        menuBar.add(aboutMenu)

        return menuBar
    }

    private fun buildDialog() : JDialog{
        val dialog = JDialog()
        dialog.setSize(40, 20)

        return dialog
    }

    private fun extract() : Array<Array<String>>  {

        row = byteArray.size / 16 + 1
        col = 16

        val array = Array(row) { Array(col) { "" } }

        array.forEachIndexed { index, strings ->
            strings.forEachIndexed { idx, _ ->
                strings[idx] = if(index * 16 + idx < byteArray.size) {
                    byteToHex(byteArray[index * 16 + idx])
                }else{
                     ""
                }
            }
        }

        return array
    }

    // Reference from http://www.camick.com/java/source/RowNumberTable.java
    class RowNumberTable(private val main: JTable) : JTable(), ChangeListener, PropertyChangeListener,
        TableModelListener {
        init {
            main.addPropertyChangeListener(this)
            main.model.addTableModelListener(this)
            isFocusable = false
            setAutoCreateColumnsFromModel(false)
            setSelectionModel(main.selectionModel)
            val column = TableColumn()
            column.headerValue = " "
            addColumn(column)
            column.cellRenderer = RowNumberRenderer()
            getColumnModel().getColumn(0).preferredWidth = 50
            preferredScrollableViewportSize = preferredSize
        }

        override fun addNotify() {
            super.addNotify()
            val c: Component = parent

            //  Keep scrolling of the row table in sync with the main table.
            if (c is JViewport) {
                val viewport: JViewport = c
                viewport.addChangeListener(this)
            }
        }

        /*
	 *  Delegate method to main table
	 */
        override fun getRowCount(): Int {
            return main.rowCount
        }

        override fun getRowHeight(row: Int): Int {
            val rowHeight = main.getRowHeight(row)
            if (rowHeight != super.getRowHeight(row)) {
                super.setRowHeight(row, rowHeight)
            }
            return rowHeight
        }

        /*
	 *  No model is being used for this table so just use the row number
	 *  as the value of the cell.
	 */
        override fun getValueAt(row: Int, column: Int): Any {
            return (row + 1).toString()
        }

        /*
	 *  Don't edit data in the main TableModel by mistake
	 */
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }

        /*
	 *  Do nothing since the table ignores the model
	 */
        override fun setValueAt(value: Any, row: Int, column: Int) {}

        //
        //  Implement the ChangeListener
        //
        override fun stateChanged(e: ChangeEvent) {
            //  Keep the scrolling of the row table in sync with main table
            val viewport: JViewport = e.source as JViewport
            val scrollPane = viewport.parent as JScrollPane
            scrollPane.verticalScrollBar.value = viewport.viewPosition.y
        }

        //
        //  Implement the PropertyChangeListener
        //
        override fun propertyChange(e: PropertyChangeEvent) {
            //  Keep the row table in sync with the main table
            if ("selectionModel" == e.propertyName) {
                setSelectionModel(main.selectionModel)
            }
            if ("rowHeight" == e.propertyName) {
                repaint()
            }
            if ("model" == e.propertyName) {
                main.model.addTableModelListener(this)
                revalidate()
            }
        }

        //
        //  Implement the TableModelListener
        //
        override fun tableChanged(e: TableModelEvent?) {
            revalidate()
        }

        /*
	 *  Attempt to mimic the table header renderer
	 */
        private class RowNumberRenderer : DefaultTableCellRenderer() {
            init {
                horizontalAlignment = JLabel.CENTER
            }

            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                if (table != null) {
                    val header: JTableHeader? = table.tableHeader
                    if (header != null) {
                        foreground = header.foreground
                        background = header.background
                        font = header.font
                    }
                }
                if (isSelected) {
                    font = font.deriveFont(Font.BOLD)
                }
                text = value?.toString() ?: ""
                border = UIManager.getBorder("TableHeader.cellBorder")
                return this
            }
        }
    }

    private fun find(hex : String){

        for(i : Int in 0 until row){
            for(j : Int in 0 until col){
                if(table.model.getValueAt(0,0).equals(hex)){
                    println("TRUE")
                }
            }
        }
    }

}