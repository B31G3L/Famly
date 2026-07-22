package com.beigel.famly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.beigel.famly.di.AppContainer
import com.beigel.famly.ui.navigation.FamlyNavHost
import com.beigel.famly.ui.theme.FamlyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamlyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FamlyNavHost(repository = AppContainer.familyRepository)
                }
            }
        }
    }
}
