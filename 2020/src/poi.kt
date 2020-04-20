import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.extractor.XSSFExcelExtractor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory
import java.io.File

fun main(args: Array<String>) {
    File(args[0]).walk().filter { it.name.endsWith(".xlsx") }.forEach { file ->
        println("Check: ${file.name}")
        excelBook(file).use {
            it.sheetAt(0).map { row ->
                row.filterIndexed { ci, _ -> ci == 2 || ci == 3 }.forEach { cell ->
                    val v = cell.stringCellValue
                    val c = ('A'..'Z').toList()[cell.address.column]
                    val r = cell.address.row + 1
                    if (v.matches("(?i).*(${args[1]}).*".toRegex())) println("${file.name} $c$r $v")
                }
            }
        }
    }
}

// シート全体をテキストに
fun getText() {
    val workbook = XSSFWorkbookFactory.createWorkbook(File("test/test.xlsx"), true)
    val excel = XSSFExcelExtractor(workbook)
    System.out.println(excel.getText())
    excel.close()
}

// セルへのアクセス
fun readCell() {
    val workbook = XSSFWorkbookFactory.createWorkbook(File("test/test.xlsx"), true)
    val excel = XSSFExcelExtractor(workbook)

    // val sheet = workbook.getSheet("Sheet1")
    val sheet = workbook.getSheetAt(0) // 0 origin
    val row = sheet.getRow(0) // 0 origin
    val cell = row.getCell(0) // 0 origin

    val v = when (cell.cellType) { //型を集約! 便利
        CellType.NUMERIC -> cell.numericCellValue
        CellType.STRING -> cell.stringCellValue
        else -> "Unknown Type"
    }
    println(v)
}

fun excelBook(file: File, readOnly: Boolean = true) = XSSFWorkbookFactory.createWorkbook(file, readOnly)!!
fun XSSFWorkbook.sheetAt(sheetNo: Int) = getSheetAt(sheetNo)!!


