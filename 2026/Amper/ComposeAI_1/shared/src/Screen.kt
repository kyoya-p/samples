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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Address(val name: String, val mail: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen() {
    val initialAddresses = listOf(
        Address("Value 1", "Value 2"),
        Address("Value 4", "Value 5"),
        Address("Value 7", "Value 8"),
        Address("Value 10", "Value 11")
    )
    
    var addresses by remember { mutableStateOf(initialAddresses) }
    var newName by remember { mutableStateOf("") }
    var newMail by remember { mutableStateOf("") }

    // Colors from SVG
    val brandColor = Color(0xFFA7C942)
    val stripeColor = Color(0xFFEAF2D3)
    val surfaceColor = Color.White
    
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = brandColor,
            onPrimary = Color.White,
            secondary = brandColor,
            surface = surfaceColor,
            surfaceVariant = stripeColor,
            background = Color(0xFFF8FAFC) // Slate-50 like Tailmater light bg
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Address Book Management", 
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.shadow(elevation = 4.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Table Section
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeader(text = "Name", weight = 1f)
                            TableHeader(text = "Mail", weight = 1.5f)
                            TableHeader(text = "Action", weight = 0.5f, alignment = Alignment.CenterHorizontally)
                        }

                        // Table Content
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(addresses) { index, address ->
                                val backgroundColor = if (index % 2 == 1) stripeColor else Color.White
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = address.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = address.mail,
                                        modifier = Modifier.weight(1.5f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                    Box(
                                        modifier = Modifier.weight(0.5f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Delete Button (Text styled as button)
                                        Text(
                                            text = "✕",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    addresses = addresses.filterIndexed { i, _ -> i != index }
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                                if (index < addresses.size - 1) {
                                    HorizontalDivider(
                                        thickness = 1.dp, 
                                        color = Color(0xFFE2E8F0) // Slate-200
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Section
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add New Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B), // Slate-800
                            modifier = Modifier.padding(bottom = 12.dp)
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
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1) // Slate-300
                                )
                            )
                            OutlinedTextField(
                                value = newMail,
                                onValueChange = { newMail = it },
                                label = { Text("Mail") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newMail.isNotBlank()) {
                                        addresses = addresses + Address(newName, newMail)
                                        newName = ""
                                        newMail = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float, alignment: Alignment.Horizontal = Alignment.Start) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
    )
}

// Wait, I don't need to define shadow, it is in androidx.compose.ui.draw.shadow. 
// I will rely on standard import.
