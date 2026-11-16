package com.example.bibliya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bibliya.ui.BibliyaTheme
import com.example.bibliya.ui.ReaderScreen
import com.example.bibliya.ui.PickerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: BibleViewModel = viewModel()
            val state = vm.state
            BibliyaTheme(settings = state.settings) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    val nav = rememberNavController()
                    BackHandler(
                        enabled = state.pickerOpen && nav.currentDestination?.route == "picker"
                    ) {
                        nav.popBackStack()
                    }
                    NavHost(navController = nav, startDestination = "reader") {
                        composable("reader") {
                            ReaderRoot(state, vm) { nav.navigate("picker") }
                        }
                        composable("picker") {
                            PickerScreen(state, vm)
                        }
                    }
                    if (state.pickerOpen && nav.currentDestination?.route != "picker") {
                        nav.navigate("picker")
                    }
                    if (!state.pickerOpen && nav.currentDestination?.route == "picker") {
                        nav.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderRoot(state: AppState, vm: BibleViewModel, onOpenPicker: () -> Unit) {
    when {
        state.isLoading -> LoadingScreen()
        state.error != null -> ErrorScreen(state.error!!, vm::retry)
        else -> ReaderScreen(state, vm, onOpenPicker)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.loading), fontSize = 18.sp)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.next)) }
        }
    }
}

@Preview
@Composable
private fun PreviewLoading() = LoadingScreen()
