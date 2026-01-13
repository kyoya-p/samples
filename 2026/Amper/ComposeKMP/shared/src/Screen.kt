import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Asset(val name: String, val mail: String)

@Composable
fun Screen() {
    val initialAssets = listOf(
        Asset("Value 1", "Value 2"),
        Asset("Value 4", "Value 5"),
        Asset("Value 7", "Value 8"),
        Asset("Value 10", "Value 11")
    )
    
    var assets by remember { mutableStateOf(initialAssets) }
    var newName by remember { mutableStateOf("") }
    var newMail by remember { mutableStateOf("") }

    // Tailmater-inspired color palette (Material 3 based)
    val customPrimary = Color(0xFFA7C942) // From the SVG
    val customSurfaceVariant = Color(0xFFEAF2D3) // Zebra stripe color
    
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = customPrimary,
            surfaceVariant = customSurfaceVariant,
            onSurfaceVariant = Color.Black
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Address Book",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Main Table Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeaderCell(text = "Name", weight = 1f)
                            TableHeaderCell(text = "Mail", weight = 1.5f)
                            TableHeaderCell(text = "Action", weight = 0.5f)
                        }

                        // Data
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(assets) { index, asset ->
                                val rowColor = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(rowColor)
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = asset.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Text(text = asset.mail, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                        
                                        // Delete Button
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .clickable {
                                                    assets = assets.filterIndexed { i, _ -> i != index }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✕",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    if (index < assets.size - 1) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Input Section Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add New Asset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = newMail,
                                onValueChange = { newMail = it },
                                label = { Text("Mail") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newMail.isNotBlank()) {
                                        assets = assets + Asset(newName, newMail)
                                        newName = ""
                                        newMail = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
    )
}