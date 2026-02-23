import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

@Composable
fun App() {
    var lifeCores by remember { mutableStateOf(5) }
    var reserveCores by remember { mutableStateOf(4) }
    var soulCoreInReserve by remember { mutableStateOf(true) }
    var trashCores by remember { mutableStateOf(0) }

    val handCards = remember { mutableStateListOf<SearchCard>() }
    val fieldCards = remember { mutableStateListOf<SearchCard>() }

    val client = remember {
        HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val cards: List<SearchCard> = client.get("/api/cards").body()
            handCards.clear()
            handCards.addAll(cards)
        } catch (e: Exception) {
            println("Error fetching cards: ${e.message}")
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top: Opponent Area (Placeholder)
                Box(modifier = Modifier.weight(0.5f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("Opponent Area (Life: 5)", modifier = Modifier.align(Alignment.Center))
                }

                // Middle: Playmat Area
                Row(modifier = Modifier.weight(3f).fillMaxWidth()) {
                    // LEFT COLUMN: Burst, Life, Reserve
                    Column(modifier = Modifier.width(120.dp).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        ZoneBox("Burst", modifier = Modifier.weight(1f).padding(bottom = 8.dp))
                        ZoneBox("Life: $lifeCores", modifier = Modifier.weight(1.5f).padding(bottom = 8.dp), color = Color(0xFF6200EE))
                        ZoneBox(
                            label = "Reserve: $reserveCores" + if (soulCoreInReserve) "\n(+Soul Core)" else "",
                            modifier = Modifier.weight(2f),
                            color = Color(0xFF2C2C2C)
                        )
                    }

                    // CENTER COLUMN: Field
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp).background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.medium)) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Text("Field", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fieldCards.forEach { card ->
                                    CardPlaceholder(card.name, modifier = Modifier.height(120.dp))
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Deck, Trash(Card), Trash(Core)
                    Column(modifier = Modifier.width(120.dp).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        ZoneBox("Deck", modifier = Modifier.weight(1f).padding(bottom = 8.dp))
                        ZoneBox("Trash (Cards)", modifier = Modifier.weight(1f).padding(bottom = 8.dp), color = Color(0xFF333333))
                        ZoneBox(
                            label = "Trash (Cores): $trashCores" + if (!soulCoreInReserve) "\n(+Soul Core)" else "",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF333333)
                        )
                    }
                }

                // Bottom: Player Hand
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hand", style = MaterialTheme.typography.labelMedium)
                        Button(onClick = { soulCoreInReserve = !soulCoreInReserve }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Use/Recover S-Core", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row(
                        modifier = Modifier.height(100.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        handCards.forEach { card ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable {
                                        fieldCards.add(card)
                                        handCards.remove(card)
                                    }
                            ) {
                                CardPlaceholder(card.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneBox(label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun CardPlaceholder(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(70.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Text(name, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
