package nl.codeface.letsee_kmm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.letsee.Configuration
import io.github.letsee.DefaultLetSee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.codeface.letsee_kmm.android.interceptor.addLetSee
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Sample activity demonstrating OkHttp interception with LetSee.
 *
 * The [OkHttpClient] is wired with [addLetSee] so every request passes through
 * [nl.codeface.letsee_kmm.android.interceptor.LetSeeInterceptor]. When mocks are enabled,
 * requests appear in the KMM request stack and the interceptor suspends until the user
 * selects a mock or "Live".
 */
class MainActivity : ComponentActivity() {

    private val letSee = DefaultLetSee.letSee

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addLetSee(letSee)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        letSee.setMocks(path = applicationContext.filesDir.absolutePath + "/Mocks")
        letSee.setConfigurations(Configuration.default.copy(isMockEnabled = true))

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LetSeeDemoScreen(onSendRequest = ::sendSampleRequest)
                }
            }
        }
    }

    private fun sendSampleRequest(onResult: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = try {
                val request = Request.Builder()
                    .url("https://jsonplaceholder.typicode.com/todos/1")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                "HTTP ${response.code} — ${response.body?.string()?.take(120) ?: "(empty)"}"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }
}

@Composable
fun LetSeeDemoScreen(onSendRequest: ((String) -> Unit) -> Unit) {
    var statusText by remember { mutableStateOf("Tap the button to send a request.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = {
            statusText = "Waiting for mock selection…"
            onSendRequest { result -> statusText = result }
        }) {
            Text("Send Request via LetSee")
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        GreetingView("Hello, Android!")
    }
}
