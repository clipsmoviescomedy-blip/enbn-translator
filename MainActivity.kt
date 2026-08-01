package com.example.enbntranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.enbntranslator.ui.TranslatorScreen
import com.example.enbntranslator.ui.theme.EnBnTranslatorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnBnTranslatorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TranslatorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
