package com.example.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // View Binding para acceder fácilmente a las vistas
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ⭐⭐ NUEVO: LIMPIAR SESIÓN DE JUEGOS AL ABRIR LA APP ⭐⭐
        clearGameSession()

        // Inicializar View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar los botones
        setupButtons()
    }

    // ⭐⭐ NUEVO MÉTODO: Limpiar progresos de juegos ⭐⭐
    private fun clearGameSession() {
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        prefs.edit().apply {
            remove("Session_TTT_PlayerScore")
            remove("Session_TTT_AiScore")
            remove("Session_Blackjack_Chips")
            remove("Session_Blackjack_Bet")
            apply()
        }
    }

    private fun setupButtons() {
        // Botón de Login
        binding.btnLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (validateInput(username, password)) {
                loginUser(username, password)
            }
        }

        // Botón para mostrar/ocultar contraseña
        var passwordVisible = false
        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                // Mostrar texto normal
                binding.editTextPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Ocultar texto (modo contraseña)
                binding.editTextPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Mover el cursor al final para no perder posición
            binding.editTextPassword.setSelection(binding.editTextPassword.text.length)
        }

        // Botón de Registro
        binding.btnRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (validateInput(username, password)) {
                registerUser(username, password)
            }
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un usuario", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa una contraseña", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 4) {
            Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loginUser(username: String, password: String) {
        // Usar SharedPreferences para verificar si el usuario existe
        val sharedPref = getSharedPreferences("GameLibraryUsers", MODE_PRIVATE)
        val savedPassword = sharedPref.getString(username, null)

        if (savedPassword == null) {
            Toast.makeText(this, "Usuario no encontrado. Por favor regístrate primero.", Toast.LENGTH_LONG).show()
        } else if (savedPassword == password) {
            Toast.makeText(this, "¡Bienvenido $username!", Toast.LENGTH_SHORT).show()
            // Aquí navegaremos a la pantalla principal (próximo paso)
            navigateToMainMenu(username)
        } else {
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser(username: String, password: String) {
        // Usar SharedPreferences para guardar el usuario
        val sharedPref = getSharedPreferences("GameLibraryUsers", MODE_PRIVATE)

        // Verificar si el usuario ya existe
        if (sharedPref.contains(username)) {
            Toast.makeText(this, "El usuario ya existe. Usa 'INICIAR SESIÓN'", Toast.LENGTH_LONG).show()
            return
        }

        // Guardar el nuevo usuario
        with(sharedPref.edit()) {
            putString(username, password)
            apply()
        }

        Toast.makeText(this, "¡Usuario $username registrado exitosamente!", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Ahora puedes iniciar sesión", Toast.LENGTH_LONG).show()
    }

    private fun navigateToMainMenu(username: String) {
        val intent = Intent(this, CategoriesActivity::class.java)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
        finish() // Cierra la pantalla de login
    }
}