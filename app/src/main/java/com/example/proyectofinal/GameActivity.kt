package com.example.proyectofinal

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal.databinding.ActivityGameBinding
import android.media.MediaPlayer


class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var gameType: String
    private lateinit var gameName: String
    private lateinit var username: String



    // SharedPreferences y récords por juego
    private val PREFS_NAME = "GamePrefs"
    private val KEY_MAX_TIC_TAC_TOE = "MaxScore_TRES_EN_RAYA"
    private val KEY_MAX_BLACKJACK = "MaxScore_BLACKJACK"
    private val KEY_MAX_SNAKE = "MaxScore_SNAKE"
    private var maxScoreTicTacToe = 0
    private var maxScoreBlackjack = 0
    private var maxScoreSnake = 0

    // NUEVAS CLAVES para puntaje de sesión (se borran al logout)
    private val KEY_SESSION_TTT_PLAYER = "Session_TTT_PlayerScore"
    private val KEY_SESSION_TTT_AI = "Session_TTT_AiScore"
    private val KEY_SESSION_BLACKJACK_CHIPS = "Session_Blackjack_Chips"
    private val KEY_SESSION_BLACKJACK_BET = "Session_Blackjack_Bet" // Para guardar la apuesta actual

    // Variables para Tres en Raya
    private lateinit var ticTacToeButtons: Array<Array<Button>>
    private var currentPlayer = "X" // X empieza siempre
    private var gameBoard = Array(3) { Array(3) { "" } }
    private var gameActive = true
    private var playerScore = 0
    private var aiScore = 0
    private var aiDifficulty = "MEDIO" // FACIL, MEDIO, DIFICIL
    private var winSound: MediaPlayer? = null
    private var loseSound: MediaPlayer? = null
    private var playerSound: MediaPlayer? = null
    private var mediaPlayer: MediaPlayer? = null // Para el método playSound


    // Variables para BlackJack
    private val playerCards = mutableListOf<String>()
    private val dealerCards = mutableListOf<String>()
    private val deck = mutableListOf<String>()
    private var playerSum = 0
    private var dealerSum = 0
    private var playerAces = 0
    private var dealerAces = 0
    private lateinit var dealerTitle: TextView
    private lateinit var playerTitle: TextView
    private lateinit var dealerCardsLayout: android.widget.LinearLayout
    private lateinit var playerCardsLayout: android.widget.LinearLayout
    private lateinit var btnHit: Button
    private lateinit var btnStand: Button
    private lateinit var gameInfo: TextView
    private var currentBet = 0
    private var playerChips = 1000
    private var isBettingPhase = true
    private var canDoubleDown = false
    private var canSplit = false
    private var splitFinished = false
    private var isSplitHand = false
    private val splitCards = mutableListOf<String>()
    private var splitSum = 0
    private var splitAces = 0
    private lateinit var chipsText: TextView
    private lateinit var betText: TextView
    private lateinit var btnDeal: Button
    private lateinit var btnDouble: Button
    private lateinit var btnSplit: Button
    private lateinit var betButtonsContainer: android.widget.LinearLayout
    private var gameOverShown = false

    // Variables para Snake
    private lateinit var snakeGameView: View
    private lateinit var snakeHandler: android.os.Handler
    private lateinit var snakeRunnable: Runnable
    private var snakeRunning = false
    private val snakeBody = mutableListOf<Pair<Int, Int>>()
    private var food = Pair(0, 0)
    private var direction = "RIGHT"
    private var snakeScore = 0
    private val gridSize = 30
    private var gameWidth = 0
    private var gameHeight = 0
    private lateinit var snakeCanvas: android.graphics.Canvas
    private lateinit var snakeBitmap: android.graphics.Bitmap
    private lateinit var snakePaint: android.graphics.Paint
    private lateinit var snakeScoreText: TextView
    private var eatSound: MediaPlayer? = null
    private var deadSound: MediaPlayer? = null
    private lateinit var snakeControlsLayout: android.widget.LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos pasados desde CategoriesActivity
        gameType = intent.getStringExtra("GAME_TYPE") ?: ""
        gameName = intent.getStringExtra("GAME_NAME") ?: ""
        username = intent.getStringExtra("USERNAME") ?: "Usuario"

        loadAllMaxScores()
        loadSessionProgress() // ← NUEVO: Cargar progreso de sesión


        // Configurar la interfaz
        setupUI()
        setupButtons()

        // Inicializar el juego correspondiente
        initializeGame()
    }

    private fun loadSessionProgress() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        when (gameType) {
            "TRES_EN_RAYA" -> {
                playerScore = prefs.getInt(KEY_SESSION_TTT_PLAYER, 0)
                aiScore = prefs.getInt(KEY_SESSION_TTT_AI, 0)
            }
            "BLACKJACK" -> {
                playerChips = prefs.getInt(KEY_SESSION_BLACKJACK_CHIPS, 1000)
                currentBet = prefs.getInt(KEY_SESSION_BLACKJACK_BET, 0)
            }
        }
    }

    private fun saveSessionProgress() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        when (gameType) {
            "TRES_EN_RAYA" -> {
                editor.putInt(KEY_SESSION_TTT_PLAYER, playerScore)
                editor.putInt(KEY_SESSION_TTT_AI, aiScore)
            }
            "BLACKJACK" -> {
                editor.putInt(KEY_SESSION_BLACKJACK_CHIPS, playerChips)
                editor.putInt(KEY_SESSION_BLACKJACK_BET, currentBet)
            }
        }
        editor.apply()
    }

    // Método para limpiar progreso de sesión (cuando haces logout)
    fun clearSessionProgress() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        editor.remove(KEY_SESSION_TTT_PLAYER)
        editor.remove(KEY_SESSION_TTT_AI)
        editor.remove(KEY_SESSION_BLACKJACK_CHIPS)
        editor.remove(KEY_SESSION_BLACKJACK_BET)
        editor.apply()
    }

    private fun setupUI() {
        binding.gameTitle.text = gameName
        binding.playerName.text = "Jugador: $username"
        updateScore()
        updateMaxScoreUI()
    }

    private fun updateScore() {
        binding.gameScore.text = "Tú: $playerScore | IA: $aiScore"
    }

    private fun loadAllMaxScores() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        maxScoreTicTacToe = prefs.getInt(KEY_MAX_TIC_TAC_TOE, 0)
        maxScoreBlackjack = prefs.getInt(KEY_MAX_BLACKJACK, 0)
        maxScoreSnake = prefs.getInt(KEY_MAX_SNAKE, 0)
    }

    private fun updateMaxScoreUI() {
        // Muestra el récord correspondiente al juego activo en binding.maxScore
        val text = when (gameType) {
            "TRES_EN_RAYA" -> "Récord: $maxScoreTicTacToe"
            "BLACKJACK" -> "Récord: $maxScoreBlackjack"
            "SNAKE" -> "Récord: $maxScoreSnake"
            else -> "Récord: 0"
        }
        binding.maxScore.text = text
    }

    private fun saveIfHigherForCurrentGame(newScore: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        when (gameType) {
            "TRES_EN_RAYA" -> {
                if (newScore > maxScoreTicTacToe) {
                    maxScoreTicTacToe = newScore
                    prefs.edit().putInt(KEY_MAX_TIC_TAC_TOE, newScore).apply()
                    updateMaxScoreUI()
                    showCustomToast("¡Nuevo récord en Tres en Raya!")
                }
            }
            "BLACKJACK" -> {
                if (newScore > maxScoreBlackjack) {
                    maxScoreBlackjack = newScore
                    prefs.edit().putInt(KEY_MAX_BLACKJACK, newScore).apply()
                    updateMaxScoreUI()
                    showCustomToast("¡Nuevo récord en BlackJack!")
                }
            }
            "SNAKE" -> {
                if (newScore > maxScoreSnake) {
                    maxScoreSnake = newScore
                    prefs.edit().putInt(KEY_MAX_SNAKE, newScore).apply()
                    updateMaxScoreUI()
                    showCustomToast("¡Nuevo récord en Snake!")
                }
            }
        }
    }

    private fun showCustomToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        // Cancelar cualquier Toast anterior para evitar superposiciones
        Toast.makeText(this, message, duration).show()
    }


    private fun setupButtons() {
        // Botón para regresar al menú principal
        binding.btnBackToMenu.setOnClickListener {
            finish() // Regresa a CategoriesActivity
        }

        // Botón para reiniciar el juego actual
        binding.btnRestart.setOnClickListener {
            restartGame()
        }
    }

    private fun initializeGame(showToast: Boolean = true) {
        when (gameType) {
            "TRES_EN_RAYA" -> {
                if (showToast)
                    Toast.makeText(this, "¡Iniciando Tres en Raya! Tú eres X", Toast.LENGTH_SHORT).show()
                initializeTicTacToe()
            }
            "BLACKJACK" -> {
                if (showToast)
                    Toast.makeText(this, "Iniciando BlackJack...", Toast.LENGTH_SHORT).show()
                initializeBlackJack()
            }
            "SNAKE" -> {
                if (showToast)
                    Toast.makeText(this, "Iniciando Snake...", Toast.LENGTH_SHORT).show()
                initializeSnake()
            }
            else -> {
                if (showToast)
                    Toast.makeText(this, "Juego no encontrado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun restartGame() {
        // Detener y limpiar
        when (gameType) {
            "BLACKJACK" -> {
                playerCards.clear()
                dealerCards.clear()
                playerSum = 0
                dealerSum = 0
                playerAces = 0
                dealerAces = 0
                //Reiniciar variables del sistema de apuestas
                playerChips = 1000
                currentBet = 0
                playerChips = 1000 // Reiniciar fichas a 1000
                isBettingPhase = true
                gameActive = false
                canDoubleDown = false
                canSplit = false
                isSplitHand = false
                splitCards.clear()
                splitSum = 0
                splitAces = 0
                // Guardar el reinicio
                saveSessionProgress()
            }
            "SNAKE" -> {
                stopSnakeGame()
                snakeBody.clear()
                snakeScore = 0
            }
            "TRES_EN_RAYA" -> {
                // SOLO REINICIAR EL TABLERO, NO LOS PUNTAJES
                currentPlayer = "X"
                gameBoard = Array(3) { Array(3) { "" } }
                gameActive = true
                // NO resetear playerScore y aiScore aquí
            }
        }

        // Limpiar el área de juego
        binding.gameArea.removeAllViews()

        // Reinicializar el juego sin mostrar Toast
        initializeGame(showToast = false)

        // Actualizar la UI
        updateScore()
        updateMaxScoreUI()
    }


    private fun initializeTicTacToe() {
        // Resetear variables
        currentPlayer = "X"
        gameBoard = Array(3) { Array(3) { "" } }
        gameActive = true

        // Limpiar área de juego
        binding.gameArea.removeAllViews()

        // Crear contenedor principal
        val mainContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Título del selector de dificultad
        val difficultyTitle = TextView(this).apply {
            text = "DIFICULTAD"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        // Botones de dificultad MÁS LARGOS
        val buttonsRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 40 }
        }

        val btnEasy = Button(this).apply {
            text = "FÁCIL"
            textSize = 14f
            setTextColor(if (aiDifficulty == "FACIL") android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            setBackgroundColor(if (aiDifficulty == "FACIL") android.graphics.Color.GREEN else android.graphics.Color.GRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(180, 120).apply {  // MÁS LARGOS: 180 en lugar de 150
                setMargins(8, 0, 8, 0)
            }
            setOnClickListener { changeDifficulty("FACIL") }
        }

        val btnMedium = Button(this).apply {
            text = "MEDIO"
            textSize = 14f
            setTextColor(if (aiDifficulty == "MEDIO") android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            setBackgroundColor(if (aiDifficulty == "MEDIO") android.graphics.Color.YELLOW else android.graphics.Color.GRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(180, 120).apply {  // MÁS LARGOS: 180 en lugar de 150
                setMargins(8, 0, 8, 0)
            }
            setOnClickListener { changeDifficulty("MEDIO") }
        }

        val btnHard = Button(this).apply {
            text = "DIFÍCIL"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(if (aiDifficulty == "DIFICIL") android.graphics.Color.RED else android.graphics.Color.GRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(180, 120).apply {  // MÁS LARGOS: 180 en lugar de 150
                setMargins(8, 0, 8, 0)
            }
            setOnClickListener { changeDifficulty("DIFICIL") }
        }

        buttonsRow.addView(btnEasy)
        buttonsRow.addView(btnMedium)
        buttonsRow.addView(btnHard)

        // Grid del juego - CENTRADO
        val gridLayout = GridLayout(this).apply {
            rowCount = 3
            columnCount = 3
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = 20
                bottomMargin = 30
            }
        }

        // Crear matriz de botones
        ticTacToeButtons = Array(3) { Array(3) { Button(this) } }

        // Crear los 9 botones - X y O MÁS PEQUEÑAS PARA QUE SE VEAN COMPLETAS
        for (row in 0..2) {
            for (col in 0..2) {
                val button = Button(this).apply {
                    text = ""
                    textSize = 35f  // MÁS PEQUEÑO: 45f en lugar de 60f para que se vean completas
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.DKGRAY)

                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 180  // Botones siguen grandes
                        height = 180
                        setMargins(5, 5, 5, 5)
                    }

                    setOnClickListener {
                        onTicTacToeButtonClick(row, col)
                    }
                }

                ticTacToeButtons[row][col] = button
                gridLayout.addView(button)
            }
        }

        // Instrucciones
        val instructions = TextView(this).apply {
            text = "Tú: X | IA: O"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
        }

        // Agregar todo al contenedor principal
        mainContainer.addView(difficultyTitle)
        mainContainer.addView(buttonsRow)
        mainContainer.addView(gridLayout)
        mainContainer.addView(instructions)

        winSound = MediaPlayer.create(this, R.raw.win_ttt)
        loseSound = MediaPlayer.create(this, R.raw.lose_ttt)
        playerSound = MediaPlayer.create(this, R.raw.player_ttt)



        // Agregar contenedor al gameArea
        binding.gameArea.addView(mainContainer)


    }

    private fun changeDifficulty(newDifficulty: String) {
        aiDifficulty = newDifficulty
        Toast.makeText(this, "Dificultad: $newDifficulty", Toast.LENGTH_SHORT).show()
        initializeTicTacToe()  // Recargar para mostrar botón activo
    }

    private fun onTicTacToeButtonClick(row: Int, col: Int) {
        // Verificar si el juego está activo y la casilla está vacía
        if (!gameActive || gameBoard[row][col].isNotEmpty()) {
            return
        }

        // Movimiento del jugador (X)
        makeMove(row, col, "X")

        // Verificar si el jugador ganó
        if (checkWinner("X")) {
            gameActive = false
            playerScore++
            updateScore()
            playSound(R.raw.win_ttt)  // ✅ Usar método playSound
            // Guardar récord si aplica:
            saveIfHigherForCurrentGame(playerScore) // <-- aquí
            saveSessionProgress() // ← GUARDAR PROGRESO
            Toast.makeText(this, "¡Ganaste!", Toast.LENGTH_LONG).show()
            return
        }

        // Verificar empate
        if (isBoardFull()) {
            gameActive = false
            Toast.makeText(this, "¡Empate!", Toast.LENGTH_LONG).show()
            return
        }

        // Movimiento de la IA (O)
        makeAIMove()

        // Verificar si la IA ganó
        if (checkWinner("O")) {
            gameActive = false
            aiScore++
            updateScore()
            playSound(R.raw.lose_ttt)  // ✅ Usar método playSound
            saveSessionProgress() // ← GUARDAR PROGRESO
            Toast.makeText(this, "La IA ganó", Toast.LENGTH_LONG).show()
            return
        }

        // Verificar empate después del movimiento de la IA
        if (isBoardFull()) {
            gameActive = false
            Toast.makeText(this, "¡Empate!", Toast.LENGTH_LONG).show()
        }
    }

    private fun playSound(soundResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer?.start()
    }


    private fun makeMove(row: Int, col: Int, player: String) {
        gameBoard[row][col] = player
        ticTacToeButtons[row][col].text = player
        ticTacToeButtons[row][col].setTextColor(
            if (player == "X") resources.getColor(android.R.color.holo_green_light, theme)
            else resources.getColor(android.R.color.holo_red_light, theme)
        )
        if (player == "X") {
            playSound(R.raw.player_ttt)
        }
    }

    private fun makeAIMove() {
        when (aiDifficulty) {
            "FACIL" -> makeEasyAIMove()
            "MEDIO" -> makeMediumAIMove()
            "DIFICIL" -> makeHardAIMove()
        }
    }

    private fun makeEasyAIMove() {
        // IA fácil: movimiento completamente aleatorio
        val emptySpaces = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) {
            for (col in 0..2) {
                if (gameBoard[row][col].isEmpty()) {
                    emptySpaces.add(Pair(row, col))
                }
            }
        }
        if (emptySpaces.isNotEmpty()) {
            val randomMove = emptySpaces.random()
            makeMove(randomMove.first, randomMove.second, "O")
        }
    }

    private fun makeMediumAIMove() {
        // IA medio: 50% estratégico, 50% aleatorio
        if (kotlin.random.Random.nextBoolean()) {
            if (!makeStrategicMove()) {
                makeEasyAIMove()
            }
        } else {
            makeEasyAIMove()
        }
    }

    private fun makeHardAIMove() {
        // IA difícil: siempre estratégico
        if (!makeStrategicMove()) {
            makeEasyAIMove()
        }
    }

    private fun makeStrategicMove(): Boolean {
        // 1. Intentar ganar
        for (row in 0..2) {
            for (col in 0..2) {
                if (gameBoard[row][col].isEmpty()) {
                    gameBoard[row][col] = "O"
                    if (checkWinner("O")) {
                        gameBoard[row][col] = ""
                        makeMove(row, col, "O")
                        return true
                    }
                    gameBoard[row][col] = ""
                }
            }
        }

        // 2. Bloquear al jugador
        for (row in 0..2) {
            for (col in 0..2) {
                if (gameBoard[row][col].isEmpty()) {
                    gameBoard[row][col] = "X"
                    if (checkWinner("X")) {
                        gameBoard[row][col] = ""
                        makeMove(row, col, "O")
                        return true
                    }
                    gameBoard[row][col] = ""
                }
            }
        }

        // 3. Tomar centro si está disponible
        if (gameBoard[1][1].isEmpty()) {
            makeMove(1, 1, "O")
            return true
        }

        // 4. Tomar una esquina
        val corners = listOf(Pair(0,0), Pair(0,2), Pair(2,0), Pair(2,2))
        for (corner in corners) {
            if (gameBoard[corner.first][corner.second].isEmpty()) {
                makeMove(corner.first, corner.second, "O")
                return true
            }
        }

        return false
    }



    private fun checkWinner(player: String): Boolean {
        // Verificar filas
        for (row in 0..2) {
            if (gameBoard[row][0] == player &&
                gameBoard[row][1] == player &&
                gameBoard[row][2] == player) {
                return true
            }
        }

        // Verificar columnas
        for (col in 0..2) {
            if (gameBoard[0][col] == player &&
                gameBoard[1][col] == player &&
                gameBoard[2][col] == player) {
                return true
            }
        }

        // Verificar diagonales
        if (gameBoard[0][0] == player && gameBoard[1][1] == player && gameBoard[2][2] == player) {
            return true
        }
        if (gameBoard[0][2] == player && gameBoard[1][1] == player && gameBoard[2][0] == player) {
            return true
        }

        return false
    }

    private fun isBoardFull(): Boolean {
        for (row in 0..2) {
            for (col in 0..2) {
                if (gameBoard[row][col].isEmpty()) {
                    return false
                }
            }
        }
        return true
    }


    private fun initializeBlackJack() {
        // CANCELAR APUESTA AL INICIALIZAR - AGREGAR ESTO AL INICIO
        if (isBettingPhase && currentBet > 0) {
            playerChips += currentBet
            currentBet = 0
        }

        // Resetear variables del BlackJack (el resto del código se mantiene igual)
        playerCards.clear()
        dealerCards.clear()
        deck.clear()
        gameActive = false
        isBettingPhase = true
        currentBet = 0  // Ya se estableció en 0 arriba, pero por consistencia
        playerSum = 0
        dealerSum = 0
        playerAces = 0
        dealerAces = 0
        canDoubleDown = false
        canSplit = false
        isSplitHand = false
        splitCards.clear()
        splitSum = 0
        splitAces = 0
        gameOverShown = false


        // Limpiar área de juego
        binding.gameArea.removeAllViews()

        // Crear contenedor principal
        val mainContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Panel de fichas y apuesta
        val chipsContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        chipsText = TextView(this).apply {
            text = "FICHAS: $playerChips"
            textSize = 18f
            setTextColor(android.graphics.Color.YELLOW)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        betText = TextView(this).apply {
            text = "APUESTA: $currentBet"
            textSize = 18f
            setTextColor(android.graphics.Color.GREEN)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        chipsContainer.addView(chipsText)
        chipsContainer.addView(betText)

        // Botones de apuesta
        betButtonsContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }
        }

        val betAmounts = listOf(10, 25, 50, 100)
        for (amount in betAmounts) {
            val betButton = Button(this).apply {
                text = "$$amount"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#8BC34A"))
                layoutParams = android.widget.LinearLayout.LayoutParams(140, 130).apply {
                    setMargins(5, 0, 5, 0)
                }
                setOnClickListener { placeBet(amount) }
            }
            betButtonsContainer.addView(betButton)
        }

        // NUEVO: Botón para restar apuesta
        val minusBetButton = Button(this).apply {
            text = "- $10"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
            layoutParams = android.widget.LinearLayout.LayoutParams(140, 130).apply {
                setMargins(5, 0, 5, 0)
            }
            setOnClickListener { subtractBet(10) }
        }
        betButtonsContainer.addView(minusBetButton)

        // Botón DEAL
        btnDeal = Button(this).apply {
            text = "REPARTIR"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            layoutParams = android.widget.LinearLayout.LayoutParams(300, 120).apply {
                setMargins(10, 0, 10, 0)
            }
            setOnClickListener {
                if (currentBet > 0) {
                    startGame()
                } else {
                    Toast.makeText(this@GameActivity, "¡Selecciona un monto de apuesta primero!", Toast.LENGTH_SHORT).show()
                }
            }
            isEnabled = true
        }

        val dealContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }
        dealContainer.addView(btnDeal)

        // Área del dealer
        val dealerContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 30 }
        }

        dealerTitle = TextView(this).apply {
            text = "DEALER (SE PARA EN 17)"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

        dealerCardsLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                230
            )
        }

        dealerContainer.addView(dealerTitle)
        dealerContainer.addView(dealerCardsLayout)

        // Área del jugador
        val playerContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 30 }
        }

        playerTitle = TextView(this).apply {
            text = "TÚ"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

        playerCardsLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                230
            )
        }

        playerContainer.addView(playerTitle)
        playerContainer.addView(playerCardsLayout)

        // Botones avanzados (Double y Split)
        val advancedButtonsContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

        btnDouble = Button(this).apply {
            text = "DOUBLE\nx2 Apuesta"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#FF5722"))
            layoutParams = android.widget.LinearLayout.LayoutParams(200, 120).apply {
                setMargins(5, 0, 5, 0)
            }
            setOnClickListener { doubleDown() }
            visibility = android.view.View.GONE
        }

        btnSplit = Button(this).apply {
            text = "SPLIT\nDividir Cartas"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
            layoutParams = android.widget.LinearLayout.LayoutParams(200, 120).apply {
                setMargins(5, 0, 5, 0)
            }
            setOnClickListener { splitCards() }
            visibility = android.view.View.GONE
        }

        advancedButtonsContainer.addView(btnDouble)
        advancedButtonsContainer.addView(btnSplit)

        // Botones del juego
        val buttonsContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        btnHit = Button(this).apply {
            text = "PEDIR"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            layoutParams = android.widget.LinearLayout.LayoutParams(260, 170).apply {
                setMargins(10, 0, 10, 0)
            }
            setOnClickListener {
                if (isSplitHand && splitFinished) {
                    handleSplitHit()
                } else {
                    hitCard()
                }
            }
            visibility = android.view.View.GONE
        }

        btnStand = Button(this).apply {
            text = "PLANTARSE"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
            layoutParams = android.widget.LinearLayout.LayoutParams(290, 170).apply {
                setMargins(10, 0, 10, 0)
            }
            setOnClickListener {
                if (isSplitHand && splitFinished) {
                    handleSplitStand()
                } else {
                    stand()
                }
            }
            visibility = android.view.View.GONE
        }

        buttonsContainer.addView(btnHit)
        buttonsContainer.addView(btnStand)

        // Información del juego
        gameInfo = TextView(this).apply {
            text = "Haz tu apuesta y presiona REPARTIR"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Agregar todo al contenedor principal
        mainContainer.addView(chipsContainer)
        mainContainer.addView(betButtonsContainer)
        mainContainer.addView(dealContainer)
        mainContainer.addView(dealerContainer)
        mainContainer.addView(playerContainer)
        mainContainer.addView(advancedButtonsContainer)
        mainContainer.addView(buttonsContainer)
        mainContainer.addView(gameInfo)

        // Agregar al área de juego
        binding.gameArea.addView(mainContainer)

        updateBetDisplay()
        checkGameOver()
    }

    // SISTEMA DE APUESTAS
    private fun placeBet(amount: Int) {
        if (isBettingPhase && playerChips >= amount) {
            playSoundBJ("apuesta_bj") // ← AGREGAR ESTA LÍNEA
            currentBet += amount
            playerChips -= amount
            updateBetDisplay()
            btnDeal.isEnabled = currentBet > 0 // se habilita automáticamente
        } else if (playerChips < amount) {
            Toast.makeText(this, "¡No tienes suficientes fichas!", Toast.LENGTH_SHORT).show()
        }
    }

    //Función para restar apuesta
    private fun subtractBet(amount: Int) {
        if (isBettingPhase && currentBet >= amount) {
            playSoundBJ("quitar_bj") // ← AGREGAR ESTA LÍNEA
            currentBet -= amount
            playerChips += amount
            updateBetDisplay()
            btnDeal.isEnabled = currentBet > 0
        } else if (currentBet < amount) {
            Toast.makeText(this, "¡No puedes restar más de lo apostado!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBetDisplay() {
        chipsText.text = "FICHAS: $playerChips"
        betText.text = "APUESTA: $currentBet"

        if (currentBet <= 0) {
            checkGameOver()
        }
    }

    // INICIAR JUEGO
    private fun startGame() {
        if (currentBet <= 0) {
            Toast.makeText(this, "¡Debes hacer una apuesta primero!", Toast.LENGTH_SHORT).show()
            return
        }
        playSoundBJ("repartir_bj") // ← AGREGAR ESTA LÍNEA

        isBettingPhase = false
        gameActive = true

        // Ocultar/mostrar botones apropiados
        betButtonsContainer.visibility = android.view.View.GONE
        btnDeal.visibility = android.view.View.GONE
        btnHit.visibility = android.view.View.VISIBLE
        btnStand.visibility = android.view.View.VISIBLE

        // Crear y barajar el deck
        createDeck()

        // Limpiar manos anteriores
        playerCards.clear()
        dealerCards.clear()
        playerSum = 0
        dealerSum = 0
        playerAces = 0
        dealerAces = 0

        // Repartir cartas iniciales
        dealCard(true)
        dealCard(false)
        dealCard(true)
        dealCard(false)

        updateDisplay()
        checkAdvancedOptions()

        // Verificar BlackJack natural
        if (playerSum == 21) {
            gameActive = false
            // Pago 3:2 por BlackJack
            val winnings = (currentBet * 2.5).toInt()
            playerChips += winnings + currentBet
            playSoundBJ("ganar_bj") // ← AGREGAR ESTA LÍNEA
            showGameResult("¡BLACKJACK! Ganas $winnings fichas")
            updateBetDisplay()
        }
    }

    // DOUBLE DOWN
    private fun doubleDown() {
        if (canDoubleDown && playerChips >= currentBet) {
            playSoundBJ("pedir_bj") // ← AGREGAR ESTA LÍNEA
            playerChips -= currentBet
            currentBet *= 2
            dealCard(true)
            updateBetDisplay()
            updateDisplay()
            stand() // Después de double down, el jugador se planta automáticamente
        }
    }

    private fun splitCards() {
        if (canSplit && playerChips >= currentBet) {
            isSplitHand = true
            splitCards.add(playerCards.removeAt(1))

            // Recalcular ases después del split
            playerAces = 0
            splitAces = 0
            for (card in playerCards) {
                if (card.startsWith("A")) playerAces++
            }
            for (card in splitCards) {
                if (card.startsWith("A")) splitAces++
            }

            // Duplicar la apuesta
            playerChips -= currentBet
            currentBet *= 2

            // SOLO repartir cartas adicionales al JUGADOR, NO al dealer
            dealCard(true) // Para mano principal
            dealCard(true, true) // Para mano split

            // Calcular sumas después de las nuevas cartas
            splitSum = calculateSum(splitCards, splitAces)
            playerSum = calculateSum(playerCards, playerAces)

            updateBetDisplay()
            updateDisplay()

            // Ocultar botones avanzados durante el split
            btnDouble.visibility = android.view.View.GONE
            btnSplit.visibility = android.view.View.GONE

            // SOLO VERIFICAR SI LA MANO PRINCIPAL SE PASÓ
            if (playerSum > 21) {
                gameInfo.text = "Mano principal se pasó ($playerSum). Pasando a mano split..."
                btnHit.isEnabled = false
                btnStand.isEnabled = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    playSplitSecondHand()
                }, 1500)
            } else {
                gameInfo.text = "Split creado - Jugando mano principal (Principal: $playerSum | Split: $splitSum)"
                btnHit.isEnabled = true
                btnStand.isEnabled = true
            }
        }
    }

    // VERIFICAR OPCIONES AVANZADAS
    private fun checkAdvancedOptions() {
        // Double Down: Permitir si la suma es 9, 10 u 11
        canDoubleDown = (playerSum in 9..11) && playerCards.size == 2 && playerChips >= currentBet

        // Split: Permitir si las dos cartas son del mismo valor
        canSplit = (playerCards.size == 2 &&
                getCardValue(playerCards[0]) == getCardValue(playerCards[1]) &&
                playerChips >= currentBet && !isSplitHand)

        // Mostrar/ocultar botones
        btnDouble.visibility = if (canDoubleDown) android.view.View.VISIBLE else android.view.View.GONE
        btnSplit.visibility = if (canSplit) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun getCardValue(card: String): Int {
        return when {
            card.startsWith("A") -> 1
            card.startsWith("K") || card.startsWith("Q") || card.startsWith("J") -> 10
            else -> card.dropLast(1).toIntOrNull() ?: 0
        }
    }

    // dealCard para soportar split
    private fun dealCard(toPlayer: Boolean, isSplit: Boolean = false) {
        if (deck.isEmpty()) {
            createDeck()
        }

        val card = deck.removeAt(0)

        if (toPlayer) {
            if (isSplit) {
                splitCards.add(card)
                if (card.startsWith("A")) splitAces++
                splitSum = calculateSum(splitCards, splitAces)
            } else {
                playerCards.add(card)
                if (card.startsWith("A")) playerAces++
                playerSum = calculateSum(playerCards, playerAces)
            }
        } else {
            dealerCards.add(card)
            if (card.startsWith("A")) dealerAces++
            dealerSum = calculateSum(dealerCards, dealerAces)
        }
    }

    private fun createDeck() {
        val suits = listOf("♠", "♥", "♦", "♣")
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

        deck.clear()
        for (suit in suits) {
            for (rank in ranks) {
                deck.add("$rank$suit")
            }
        }
        deck.shuffle()
    }

    private fun calculateSum(cards: List<String>, aces: Int): Int {
        var sum = 0
        var acesCount = aces

        for (card in cards) {
            when {
                card.startsWith("A") -> sum += 11
                card.startsWith("K") || card.startsWith("Q") || card.startsWith("J") -> sum += 10
                else -> {
                    val value = card.dropLast(1).toIntOrNull() ?: 0
                    sum += value
                }
            }
        }

        // Ajustar ases si es necesario
        while (sum > 21 && acesCount > 0) {
            sum -= 10
            acesCount--
        }

        return sum
    }


    private fun hitCard() {
        if (!gameActive && !isSplitHand) return

        if (isSplitHand && !splitFinished) {
            // Jugando mano principal durante split
            playSoundBJ("pedir_bj")
            dealCard(true)
            updateDisplay()

            if (playerSum > 21) {
                gameInfo.text = "Mano principal se pasó ($playerSum). Pasando a mano split..."
                btnHit.isEnabled = false
                btnStand.isEnabled = false

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    splitFinished = true
                    playSplitSecondHand()
                }, 1500)
                return
            }

            if (playerSum == 21) {
                gameInfo.text = "¡21 en mano principal! Pasando a mano split..."
                btnHit.isEnabled = false
                btnStand.isEnabled = false

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    splitFinished = true
                    playSplitSecondHand()
                }, 1500)
                return
            }
        } else {
            // Juego normal sin split
            playSoundBJ("pedir_bj")
            dealCard(true)
            updateDisplay()
            checkAdvancedOptions()

            if (playerSum > 21) {
                gameActive = false
                playSoundBJ("perder_bj")
                showGameResult("¡TE PASASTE! Perdiste $currentBet fichas")
                return
            }

            if (playerSum == 21) {
                stand()
            }
        }
    }

    private fun playSplitSecondHand() {
        // VERIFICAR SI LA MANO SPLIT YA LLEGÓ A 21 O SE PASÓ
        if (splitSum >= 21) {
            gameInfo.text = "Mano split: $splitSum. Dealer juega..."
            btnHit.isEnabled = false
            btnStand.isEnabled = false
            gameActive = false
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                playDealerTurn()
            }, 1000)
            return
        }

        gameInfo.text = "Jugando mano split ($splitSum) - ¿Pedir o plantarse?"

        // Habilitar botones
        btnHit.isEnabled = true
        btnStand.isEnabled = true
    }

    private fun handleSplitHit() {
        if (splitSum <= 21) {
            dealCard(true, true)
            playSoundBJ("pedir_bj")
            updateDisplay()

            if (splitSum > 21) {
                gameInfo.text = "Mano split se pasó ($splitSum). Dealer juega..."
                btnHit.isEnabled = false
                btnStand.isEnabled = false
                gameActive = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    playDealerTurn()
                }, 1500)
            } else if (splitSum == 21) {
                gameInfo.text = "¡21 en mano split! Dealer juega..."
                btnHit.isEnabled = false
                btnStand.isEnabled = false
                gameActive = false
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    playDealerTurn()
                }, 1500)
            }
        }
    }

    private fun handleSplitStand() {
        btnHit.isEnabled = false
        btnStand.isEnabled = false
        gameActive = false
        playDealerTurn()
    }

    private fun stand() {
        // Si estamos en split y no hemos terminado la primera mano
        if (isSplitHand && !splitFinished) {
            splitFinished = true
            btnHit.isEnabled = false
            btnStand.isEnabled = false
            playSplitSecondHand()
            return
        }

        // Juego normal o segunda mano de split
        gameActive = false
        btnHit.isEnabled = false
        btnStand.isEnabled = false
        updateDisplay()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playDealerTurn()
        }, 1000)
    }

    private fun playDealerTurn() {
        // Primero revelar todas las cartas del dealer
        updateDisplay()

        if (dealerSum < 17) {
            dealCard(false)
            updateDisplay()

            if (dealerSum > 21) {
                if (isSplitHand) {
                    determineSplitWinner()
                } else {
                    val winnings = currentBet * 2
                    playerChips += winnings
                    playSoundBJ("ganar_bj")
                    showGameResult("¡DEALER SE PASÓ! Ganas $currentBet fichas")
                }
                return
            }

            if (dealerSum < 17) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    playDealerTurn()
                }, 1000)
            } else {
                if (isSplitHand) {
                    determineSplitWinner()
                } else {
                    determineWinner()
                }
            }
        } else {
            if (isSplitHand) {
                determineSplitWinner()
            } else {
                determineWinner()
            }
        }
    }

    private fun determineSplitWinner() {
        val mainResult = calculateHandResult(playerSum, dealerSum)
        val splitResult = calculateHandResult(splitSum, dealerSum)

        // Total de ganancias sumando ambas manos
        val totalWinnings = mainResult.first + splitResult.first
        val betPerHand = currentBet / 2  // Cada mano tiene la mitad de la apuesta total

        // Calcular ganancia/pérdida neta
        val netResult = totalWinnings - currentBet

        // Construir mensaje detallado
        val message = buildString {
            append("Mano Principal: ${mainResult.second}\n")
            append("Mano Split: ${splitResult.second}\n")
            append("Total: ")
            when {
                netResult > 0 -> append("Ganaste $netResult fichas")
                netResult < 0 -> append("Perdiste ${-netResult} fichas")
                else -> append("Empate - Recuperas tu apuesta")
            }
        }

        // Actualizar fichas
        playerChips += totalWinnings

        // Reproducir sonido según el resultado NETO
        when {
            netResult > 0 -> playSoundBJ("ganar_bj")
            netResult < 0 -> playSoundBJ("perder_bj")
            // No reproducir sonido en empate
        }

        // Mostrar resultado
        showGameResult(message)
        updateBetDisplay()
    }

    private fun calculateHandResult(handSum: Int, dealerSum: Int): Pair<Int, String> {
        val betPerHand = currentBet / 2
        return when {
            handSum > 21 -> Pair(0, "Se pasó - Pierde $betPerHand")
            dealerSum > 21 -> Pair(betPerHand * 2, "Dealer se pasó - Gana $betPerHand")
            dealerSum > handSum -> Pair(0, "Dealer gana - Pierde $betPerHand")
            handSum > dealerSum -> Pair(betPerHand * 2, "Gana $betPerHand")
            else -> Pair(betPerHand, "Empate - Recupera $betPerHand")
        }
    }

    private fun determineWinner() {
        when {
            dealerSum > 21 -> {
                val winnings = currentBet * 2
                playerChips += winnings
                playSoundBJ("ganar_bj") // ← AGREGAR ESTA LÍNEA
                showGameResult("¡DEALER SE PASÓ! Ganas $currentBet fichas")
            }
            dealerSum > playerSum -> {
                playSoundBJ("perder_bj") // ← AGREGAR ESTA LÍNEA
                showGameResult("Dealer ganó. Pierdes $currentBet fichas")
            }
            playerSum > dealerSum -> {
                val winnings = currentBet * 2
                playerChips += winnings
                playSoundBJ("ganar_bj") // ← AGREGAR ESTA LÍNEA
                showGameResult("¡Ganaste! Ganas $currentBet fichas")
            }
            else -> {
                playerChips += currentBet
                showGameResult("¡EMPATE! Recuperas tu apuesta")
            }
        }
        updateBetDisplay()
    }

    private fun updateDisplay() {
        // Mostrar cartas del jugador
        playerCardsLayout.removeAllViews()

        // Mano principal
        for (card in playerCards) {
            val cardView = createCardView(card)
            playerCardsLayout.addView(cardView)
        }

        // Si hay split, mostrar separador y mano split
        if (isSplitHand && splitCards.isNotEmpty()) {
            val separator = TextView(this).apply {
                text = " | "
                textSize = 20f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(50, 230)
            }
            playerCardsLayout.addView(separator)

            for (card in splitCards) {
                val cardView = createCardView(card)
                playerCardsLayout.addView(cardView)
            }
        }

        // Mostrar cartas del dealer
        dealerCardsLayout.removeAllViews()
        for (i in dealerCards.indices) {
            // La segunda carta se mantiene oculta si:
            // 1. El juego está activo (cualquier mano del jugador sigue jugando)
            // 2. Es la segunda carta (índice 1)
            val shouldHideCard = (gameActive || (isSplitHand && !splitFinished)) && i == 1

            val cardView = if (shouldHideCard) {
                createCardView("🂠")
            } else {
                createCardView(dealerCards[i])
            }
            dealerCardsLayout.addView(cardView)
        }

        // Actualizar títulos con sumas
        if (isSplitHand) {
            playerTitle.text = "TÚ (Principal: $playerSum | Split: $splitSum)"
        } else {
            playerTitle.text = "TÚ ($playerSum)"
        }

        // Mostrar suma del dealer según el estado del juego
        if ((gameActive || (isSplitHand && !splitFinished)) && dealerCards.size > 1) {
            val visibleSum = calculateSum(listOf(dealerCards[0]), if (dealerCards[0].startsWith("A")) 1 else 0)
            dealerTitle.text = "DEALER ($visibleSum + ?)"
        } else {
            dealerTitle.text = "DEALER ($dealerSum)"
        }
    }

    private fun createCardView(card: String): TextView {
        return TextView(this).apply {
            text = card
            textSize = 24f
            setTextColor(
                when {
                    card.contains("♥") || card.contains("♦") -> android.graphics.Color.RED
                    else -> android.graphics.Color.WHITE
                }
            )
            setBackgroundColor(android.graphics.Color.DKGRAY)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(120, 230).apply {
                setMargins(3, 0, 3, 0)
            }
            setPadding(8, 8, 8, 8)
        }
    }

    private fun showGameResult(message: String) {
        gameInfo.text = message
        updateDisplay()

        if (gameType == "BLACKJACK") {
            saveIfHigherForCurrentGame(playerChips) // <-- guarda si playerChips supera el récord
            saveSessionProgress() // ← GUARDAR PROGRESO
        }

        // Deshabilitar botones temporalmente
        btnHit.isEnabled = false
        btnStand.isEnabled = false
        btnDouble.isEnabled = false
        btnSplit.isEnabled = false


        // Preparar para nueva ronda después de 3 segundos
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            resetForNewRound()
        }, if (isSplitHand) 5500L else 3000L)


        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun resetForNewRound() {
        gameOverShown = false
        isBettingPhase = true
        currentBet = 0

        // Reiniciar variables de split
        isSplitHand = false
        splitFinished = false
        splitCards.clear()
        splitSum = 0
        splitAces = 0

        // Restaurar visibilidad de botones
        betButtonsContainer.visibility = android.view.View.VISIBLE
        btnDeal.visibility = android.view.View.VISIBLE
        btnDeal.isEnabled = false
        btnHit.visibility = android.view.View.GONE
        btnStand.visibility = android.view.View.GONE
        btnDouble.visibility = android.view.View.GONE
        btnSplit.visibility = android.view.View.GONE

        // RESTAURAR LISTENERS ORIGINALES
        btnHit.setOnClickListener { hitCard() }
        btnStand.setOnClickListener { stand() }

        // Habilitar botones
        btnHit.isEnabled = true
        btnStand.isEnabled = true
        btnDouble.isEnabled = true
        btnSplit.isEnabled = true

        // Habilitar botones de apuesta si hay fichas
        if (playerChips > 0) {
            for (i in 0 until betButtonsContainer.childCount) {
                betButtonsContainer.getChildAt(i).isEnabled = true
            }
        }

        // Limpiar displays
        playerCardsLayout.removeAllViews()
        dealerCardsLayout.removeAllViews()

        gameInfo.text = if (playerChips > 0) "Haz tu apuesta y presiona REPARTIR" else "¡Te quedaste sin fichas! Presiona REINICIAR"
        updateBetDisplay()

        // Limpiar manos
        playerCards.clear()
        dealerCards.clear()
        playerSum = 0
        dealerSum = 0
        playerAces = 0
        dealerAces = 0

        checkGameOver()
    }

    private fun checkGameOver() {
        if (playerChips <= 0 && currentBet <= 0 && isBettingPhase && !gameOverShown) {
            gameOverShown = true

            gameInfo.text = "¡Te quedaste sin fichas! Presiona REINICIAR"

            // Deshabilitar botones de apuesta
            for (i in 0 until betButtonsContainer.childCount) {
                betButtonsContainer.getChildAt(i).isEnabled = false
            }
            btnDeal.isEnabled = false

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, "¡Game Over! Te quedaste sin fichas. Presiona REINICIAR para jugar otra vez.", Toast.LENGTH_LONG).show()
            }, 500)
        }
    }



    private fun playSoundBJ(soundType: String) {
        try {
            // Liberar cualquier MediaPlayer activo
            mediaPlayer?.release()
            mediaPlayer = null

            // Seleccionar sonido según tipo (insensible a mayúsculas)
            val soundRes = when (soundType.lowercase()) {
                "apuesta_bj" -> R.raw.apuesta_bj
                "quitar_bj" -> R.raw.quitar_bj
                "repartir_bj" -> R.raw.repartir_bj
                "pedir_bj" -> R.raw.pedir_bj
                "ganar_bj" -> R.raw.ganar_bj
                "perder_bj" -> R.raw.perder_bj
                else -> null
            }

            soundRes?.let {
                mediaPlayer = MediaPlayer.create(this, it)
                mediaPlayer?.setOnCompletionListener { mp ->
                    mp.release() // liberar cuando termine
                }
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeSnake() {

        // Limpiar área de juego
        binding.gameArea.removeAllViews()
        snakeRunning = false

        // Crear contenedor principal
        val mainContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Puntuación
        snakeScoreText = TextView(this).apply {
            text = "PUNTOS: 0"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        // Área del juego
        gameWidth = 900
        gameHeight = 900

        snakeBitmap = android.graphics.Bitmap.createBitmap(gameWidth, gameHeight, android.graphics.Bitmap.Config.ARGB_8888)
        snakeCanvas = android.graphics.Canvas(snakeBitmap)

        snakePaint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        snakeGameView = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(gameWidth, gameHeight).apply {
                topMargin = 10
                bottomMargin = 20
            }
            setBackgroundColor(android.graphics.Color.BLACK)
            setImageBitmap(snakeBitmap)
        }

        // Controles direccionales
        snakeControlsLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Botón ARRIBA - Hacer más grande
        val btnUp = Button(this).apply {
            text = "↑"
            textSize = 20f // Aumentado de 20f a 30f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.DKGRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(150, 130).apply { // Aumentado de 100x80 a 150x120
                bottomMargin = 5
            }
            setOnClickListener { changeDirection("UP") }
        }

        // Fila de botones IZQUIERDA y DERECHA
        val middleRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // Botón IZQUIERDA - Hacer más grande
        val btnLeft = Button(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.DKGRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(150, 130).apply { // Aumentado de 100x80 a 150x120
                rightMargin = 10
            }
            setOnClickListener { changeDirection("LEFT") }
        }

        // Botón DERECHA - Hacer más grande
        val btnRight = Button(this).apply {
            text = "→"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.DKGRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(150, 130).apply { // Aumentado de 100x80 a 150x120
                leftMargin = 10
            }
            setOnClickListener { changeDirection("RIGHT") }
        }

        middleRow.addView(btnLeft)
        middleRow.addView(btnRight)

        // Botón ABAJO - Hacer más grande
        val btnDown = Button(this).apply {
            text = "↓"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.DKGRAY)
            layoutParams = android.widget.LinearLayout.LayoutParams(150, 130).apply { // Aumentado de 100x80 a 150x120
                topMargin = 5
            }
            setOnClickListener { changeDirection("DOWN") }
        }

        snakeControlsLayout.addView(btnUp)
        snakeControlsLayout.addView(middleRow)
        snakeControlsLayout.addView(btnDown)

// Botón START/STOP - Hacer más grande (modificado para que solo inicie)
        val btnStartStop = Button(this).apply {
            text = "INICIAR"
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            layoutParams = android.widget.LinearLayout.LayoutParams(500, 120).apply {
                topMargin = 20
            }
            setOnClickListener {
                // Solo inicia el juego si no está corriendo
                if (!snakeRunning) {
                    startSnakeGame()
                }
                // No hacer nada si ya está corriendo (quitamos la funcionalidad de pausa)
            }
        }
        // Agregar todo al contenedor
        mainContainer.addView(snakeScoreText)
        mainContainer.addView(snakeGameView)
        mainContainer.addView(snakeControlsLayout)
        mainContainer.addView(btnStartStop)

        // Agregar al área de juego
        binding.gameArea.addView(mainContainer)

        eatSound = MediaPlayer.create(this, R.raw.eat_snake)
        deadSound = MediaPlayer.create(this, R.raw.dead_snake)

        // Inicializar juego
        initSnakeGame()
    }

    private fun initSnakeGame() {
        // Resetear variables
        snakeBody.clear()
        snakeScore = 0
        direction = "RIGHT"

        // Posición inicial de la serpiente (centro)
        val startX = gameWidth / (2 * gridSize)
        val startY = gameHeight / (2 * gridSize)
        snakeBody.add(Pair(startX, startY))
        snakeBody.add(Pair(startX - 1, startY))
        snakeBody.add(Pair(startX - 2, startY))

        // Generar primera comida
        generateFood()

        // Actualizar puntuación
        updateSnakeScore()

        // Dibujar estado inicial
        drawSnakeGame()
    }


    private fun toggleSnakeGame() {
        if (snakeRunning) {
            stopSnakeGame()
        } else {
            startSnakeGame()
        }
    }

    private fun startSnakeGame() {
        if (snakeRunning) return

        snakeRunning = true
        snakeHandler = android.os.Handler(android.os.Looper.getMainLooper())

        snakeRunnable = object : Runnable {
            override fun run() {
                if (snakeRunning) {
                    moveSnake()
                    drawSnakeGame()
                    snakeHandler.postDelayed(this, 200) // Velocidad del juego (200ms)
                }
            }
        }

        snakeHandler.post(snakeRunnable)
    }

    private fun stopSnakeGame() {
        snakeRunning = false
        if (this::snakeHandler.isInitialized) {
            snakeHandler.removeCallbacks(snakeRunnable)
        }
    }

    private fun moveSnake() {
        if (snakeBody.isEmpty()) return

        val head = snakeBody[0]
        var newHead = head

        // Calcular nueva posición de la cabeza
        when (direction) {
            "UP" -> newHead = Pair(head.first, head.second - 1)
            "DOWN" -> newHead = Pair(head.first, head.second + 1)
            "LEFT" -> newHead = Pair(head.first - 1, head.second)
            "RIGHT" -> newHead = Pair(head.first + 1, head.second)
        }

        // Verificar colisiones con los bordes
        val maxX = gameWidth / gridSize - 1
        val maxY = gameHeight / gridSize - 1

        if (newHead.first < 0 || newHead.first > maxX ||
            newHead.second < 0 || newHead.second > maxY) {
            gameOver("¡Chocaste con el borde!")
            return
        }

        // Verificar colisión consigo misma
        if (snakeBody.contains(newHead)) {
            gameOver("¡Te mordiste la cola!")
            return
        }

        // Agregar nueva cabeza
        snakeBody.add(0, newHead)

        // Verificar si comió la comida
        if (newHead == food) {
            eatSound?.start()
            snakeScore += 10
            updateSnakeScore()
            generateFood()
        } else {
            // Remover la cola si no comió
            snakeBody.removeAt(snakeBody.size - 1)
        }
    }

    private fun generateFood() {
        val maxX = gameWidth / gridSize - 1
        val maxY = gameHeight / gridSize - 1

        do {
            food = Pair(
                (0..maxX).random(),
                (0..maxY).random()
            )
        } while (snakeBody.contains(food)) // Asegurar que no aparezca en la serpiente
    }

    private fun drawSnakeGame() {
        // Limpiar canvas
        snakeCanvas.drawColor(android.graphics.Color.BLACK)

        // Dibujar la serpiente
        snakePaint.color = android.graphics.Color.GREEN
        for (i in snakeBody.indices) {
            val segment = snakeBody[i]
            val left = (segment.first * gridSize).toFloat()
            val top = (segment.second * gridSize).toFloat()
            val right = left + gridSize - 2
            val bottom = top + gridSize - 2

            // La cabeza en color diferente
            if (i == 0) {
                snakePaint.color = android.graphics.Color.YELLOW
            } else {
                snakePaint.color = android.graphics.Color.GREEN
            }

            snakeCanvas.drawRect(left, top, right, bottom, snakePaint)
        }

        // Dibujar la comida
        snakePaint.color = android.graphics.Color.RED
        val foodLeft = (food.first * gridSize).toFloat()
        val foodTop = (food.second * gridSize).toFloat()
        val foodRight = foodLeft + gridSize - 2
        val foodBottom = foodTop + gridSize - 2
        snakeCanvas.drawRect(foodLeft, foodTop, foodRight, foodBottom, snakePaint)

        // Actualizar la vista
        (snakeGameView as android.widget.ImageView).setImageBitmap(snakeBitmap)
    }

    private fun changeDirection(newDirection: String) {
        if (!snakeRunning) return

        // Prevenir movimientos opuestos (evitar que se muerda inmediatamente)
        val canChange = when (direction) {
            "UP" -> newDirection != "DOWN"
            "DOWN" -> newDirection != "UP"
            "LEFT" -> newDirection != "RIGHT"
            "RIGHT" -> newDirection != "LEFT"
            else -> true
        }

        if (canChange) {
            direction = newDirection
        }
    }

    private fun updateSnakeScore() {
        snakeScoreText.text = "PUNTOS: $snakeScore"
    }

    private fun gameOver(message: String) {
        // Detener el juego
        deadSound?.start()
        stopSnakeGame()

        // Guardar el récord del Snake si el jugador superó el máximo
        saveIfHigherForCurrentGame(snakeScore)

        if (snakeScore > playerScore) {
            playerScore = snakeScore
            updateScore()
        }

        // Avisar al usuario
        Toast.makeText(
            this,
            "$message\nPuntuación: $snakeScore",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPause() {
        super.onPause()
        saveSessionProgress() // ← GUARDAR AL SALIR DEL JUEGO

        if (gameType == "SNAKE") {
            stopSnakeGame()
        }
    }


    // Método auxiliar para mostrar mensajes en el área de juego
    private fun showGameMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Liberar todos los recursos de MediaPlayer
        eatSound?.release()
        deadSound?.release()
        winSound?.release()
        loseSound?.release()
        mediaPlayer?.release()
        winSound?.release()
        loseSound?.release()
        mediaPlayer?.release()
        eatSound?.release()
        deadSound?.release()
    }
}