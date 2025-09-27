package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityCategoriesBinding
import android.media.MediaPlayer


class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var username: String

    private var mediaPlayer: MediaPlayer? = null  //


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el nombre de usuario pasado desde MainActivity
        username = intent.getStringExtra("USERNAME") ?: "Usuario"

        // Configurar la interfaz
        setupUI()
        setupButtons()
    }

    private fun setupUI() {
        // Personalizar saludo con el nombre del usuario
        binding.welcomeText.text = "¡Bienvenido $username!"
    }

    private fun setupButtons() {
        // Categoría ESTRATEGIA - Tres en Raya
        binding.btnStrategy.setOnClickListener {
            playButtonSound()  //
            navigateToGame("TRES_EN_RAYA", "Tres en Raya")
        }

        // Categoría CARTAS - 21 BlackJack
        binding.btnCards.setOnClickListener {
            playButtonSound()  //
            navigateToGame("BLACKJACK", "21 BlackJack")
        }

        // Categoría ACCIÓN - Snake
        binding.btnAction.setOnClickListener {
            playButtonSound()  //
            navigateToGame("SNAKE", "Snake")
        }

        // Botón de Cerrar Sesión
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun playButtonSound() {
        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(this, R.raw.openbtn)

            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }

            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun navigateToGame(gameType: String, gameName: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_TYPE", gameType)
        intent.putExtra("GAME_NAME", gameName)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
    }

    private fun logout() {
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        prefs.edit().apply {
            remove("Session_TTT_PlayerScore")
            remove("Session_TTT_AiScore")
            remove("Session_Blackjack_Chips")
            remove("Session_Blackjack_Bet")
            remove("Session_Snake_Score")
            apply()
        }
        // Crear intent para regresar al login
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Cierra esta activity
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}