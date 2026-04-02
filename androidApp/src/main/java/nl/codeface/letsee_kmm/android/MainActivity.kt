package nl.codeface.letsee_kmm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import io.github.letsee.Configuration
import io.github.letsee.DefaultLetSee
import io.github.letsee.ui.LetSeeOverlay
import io.github.letsee.ui.initLetSee
import kotlinx.coroutines.launch
import nl.codeface.letsee_kmm.android.interceptor.addLetSee
import okhttp3.OkHttpClient
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private val letSee = DefaultLetSee.letSee

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addLetSee(letSee)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLetSee()

        letSee.setMocks(path = "Mocks")
        letSee.setConfigurations(Configuration.default.copy(isMockEnabled = true))

        val catApiService = CatApiService(okHttpClient)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LetSeeOverlay(letSee) {
                        CatApp(catApiService)
                    }
                }
            }
        }
    }
}

@Composable
fun CatApp(service: CatApiService) {
    val navController = rememberNavController()
    var breeds by remember { mutableStateOf<List<Breed>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val loadBreeds: () -> Unit = {
        scope.launch {
            isLoading = true
            error = null
            try {
                breeds = service.getBreeds()
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadBreeds() }

    NavHost(navController = navController, startDestination = "breeds") {
        composable("breeds") {
            BreedListScreen(
                breeds = breeds,
                isLoading = isLoading,
                error = error,
                onRefresh = loadBreeds,
                onBreedClick = { breed ->
                    val encoded = URLEncoder.encode(breed.id, "UTF-8")
                    navController.navigate("breed/$encoded")
                },
            )
        }
        composable("breed/{breedId}") { backStackEntry ->
            val breedId = backStackEntry.arguments?.getString("breedId")
                ?.let { URLDecoder.decode(it, "UTF-8") }
            val breed = breeds.find { it.id == breedId }
            if (breed != null) {
                BreedDetailScreen(breed = breed, navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedListScreen(
    breeds: List<Breed>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onBreedClick: (Breed) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cat Breeds") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isLoading && breeds.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null && breeds.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Failed to load breeds",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        items(breeds, key = { it.id }) { breed ->
                            BreedListItem(breed = breed, onClick = { onBreedClick(breed) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreedListItem(breed: Breed, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = breed.image?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = breed.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(16.dp))
            } else {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = breed.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = breed.origin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (breed.temperament.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = breed.temperament,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedDetailScreen(breed: Breed, navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(breed.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            breed.image?.url?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = breed.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = breed.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (breed.origin.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = breed.origin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (breed.description.isNotEmpty()) {
                    SectionHeader("About")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = breed.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (breed.temperament.isNotEmpty()) {
                    SectionHeader("Temperament")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = breed.temperament,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                breed.lifeSpan?.let { lifeSpan ->
                    SectionHeader("Life Span")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$lifeSpan years",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                breed.weight?.let { weight ->
                    SectionHeader("Weight")
                    Spacer(Modifier.height(4.dp))
                    val weightText = buildString {
                        weight.metric?.let { append("$it kg") }
                        if (weight.imperial != null && weight.metric != null) append(" / ")
                        weight.imperial?.let { append("$it lbs") }
                    }
                    if (weightText.isNotEmpty()) {
                        Text(
                            text = weightText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}
