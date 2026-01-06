package com.example

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    println("--- Starting Sheets API Test ---")
    println("Current Working Directory: " + System.getProperty("user.dir"))
    val userId = "user"
    
    // 1. Create a spreadsheet
    println("1. Creating a test spreadsheet...")
    val title = "SheetMaster Test - ${System.currentTimeMillis()}"
    val spreadsheetId = GoogleSheetsService.createSpreadsheet(userId, title)
    println("Success! Created Spreadsheet ID: $spreadsheetId")
    println("URL: https://docs.google.com/spreadsheets/d/$spreadsheetId")
    GoogleSheetsService.openInBrowser(spreadsheetId)
    
    // 2. Update a cell
    println("2. Updating cell A1 with 'Hello Compose Desktop!'...")
    val range = "A1"
    val value = "Hello Compose Desktop!"
    GoogleSheetsService.updateCell(userId, spreadsheetId, range, value)
    println("Success! Cell updated.")
    
    println("--- Test Completed Successfully ---")
}
