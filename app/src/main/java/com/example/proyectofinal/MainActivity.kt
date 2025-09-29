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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // VERIFICAR SI HAY UNA SESIÓN ACTIVA AL INICIAR LA APP
        checkExistingSession()

        // Inicializar View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // LIMPIAR SESIÓN DE JUEGOS (pero no la del usuario)
        clearGameSession()

        // Configurar los botones
        setupButtons()
    }

    // NUEVO MeTODO: Verificar si ya hay una sesión activa
    private fun checkExistingSession() {
        val sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sessionPrefs.getBoolean("isLoggedIn", false)
        val username = sessionPrefs.getString("username", "")

        if (isLoggedIn && !username.isNullOrEmpty()) {
            // Si hay sesión activa, ir directamente al menú principal
            navigateToMainMenu(username, true) // true = reingreso
        }
        // Si no hay sesión, continuar mostrando el login
    }

    // NUEVO MeTODO: Guardar sesión cuando el usuario inicia sesión
    private fun saveUserSession(username: String) {
        val sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        sessionPrefs.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("username", username)
            apply()
        }
    }

    // NUEVO MeTODO: Para cerrar sesión (llamado desde otras actividades)
    fun logoutUser() {
        val sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        sessionPrefs.edit().apply {
            putBoolean("isLoggedIn", false)
            remove("username")
            apply()
        }
    }

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
                binding.editTextPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.editTextPassword.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
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
        val sharedPref = getSharedPreferences("GameLibraryUsers", MODE_PRIVATE)
        val savedPassword = sharedPref.getString(username, null)

        if (savedPassword == null) {
            Toast.makeText(this, "Usuario no encontrado. Por favor regístrate primero.", Toast.LENGTH_LONG).show()
        } else if (savedPassword == password) {
            Toast.makeText(this, "¡Bienvenido $username!", Toast.LENGTH_SHORT).show()

            // GUARDAR LA SESIÓN DEL USUARIO
            saveUserSession(username)

            navigateToMainMenu(username, false) // false = primer ingreso
        } else {
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser(username: String, password: String) {
        val sharedPref = getSharedPreferences("GameLibraryUsers", MODE_PRIVATE)

        if (sharedPref.contains(username)) {
            Toast.makeText(this, "El usuario ya existe. Usa 'INICIAR SESIÓN'", Toast.LENGTH_LONG).show()
            return
        }

        with(sharedPref.edit()) {
            putString(username, password)
            apply()
        }

        Toast.makeText(this, "¡Usuario $username registrado exitosamente!", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Ahora puedes iniciar sesión", Toast.LENGTH_LONG).show()
    }

    //MODIFICADO: Ahora recibe un parámetro para saber si es reingreso
    private fun navigateToMainMenu(username: String, isReturningUser: Boolean) {
        val intent = Intent(this, CategoriesActivity::class.java)
        intent.putExtra("USERNAME", username)
        intent.putExtra("IS_RETURNING_USER", isReturningUser) // NUEVO EXTRA
        startActivity(intent)
        finish() // Cierra la pantalla de login
    }
}