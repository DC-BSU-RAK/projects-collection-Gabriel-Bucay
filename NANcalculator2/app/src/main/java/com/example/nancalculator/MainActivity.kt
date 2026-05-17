package com.example.nancalculator

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * MainActivity for WrapLab - A Shawarma-themed flavor calculator.
 * This activity handles the UI logic, ingredient selection, and chef reactions.
 * 
 * Design Philosophy:
 * - High interactivity through animations.
 * - Responsive feedback for every user action.
 * - "Talking Chef" avatar system for branding.
 */
class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var display: TextView
    private lateinit var chefReaction: TextView
    private lateinit var chefImg: ImageView
    
    // Audio feedback
    private var clickPlayer: MediaPlayer? = null
    private var wrapPlayer: MediaPlayer? = null
    
    // State Management
    private val selectedToppings = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        val splashScreen = installSplashScreen()
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        // Timer to dismiss splash screen after 3 seconds, then show loading bar overlay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            keepSplashScreen = false
        }, 3000)

        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for a modern, immersive look
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val loadingOverlay = findViewById<View>(R.id.loading_overlay)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.loading_bar)
        
        // Animate the progress bar from 0 to 100 over 5 seconds
        val progressAnimator = android.animation.ValueAnimator.ofInt(0, 100)
        progressAnimator.duration = 5000
        progressAnimator.addUpdateListener { animator ->
            progressBar.progress = animator.animatedValue as Int
        }
        progressAnimator.start()

        // Hide overlay after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction { loadingOverlay.visibility = View.GONE }
                .start()
        }, 5000)
        
        // Handle window insets for status/navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI components
        display = findViewById(R.id.display)
        chefReaction = findViewById(R.id.chef_reaction)
        chefImg = findViewById(R.id.chef_img)
        
        // Initialize sound effects dynamically to avoid "Unresolved reference 'raw'" errors when files are missing
        val clickResId = resources.getIdentifier("click_sound", "raw", packageName)
        clickPlayer = if (clickResId != 0) MediaPlayer.create(this, clickResId) else null

        val wrapResId = resources.getIdentifier("wrap_sound", "raw", packageName)
        wrapPlayer = if (wrapResId != 0) MediaPlayer.create(this, wrapResId) else null

        // Start initialization logic
        setupButtons()
        startIdleAnimations()
        playEntranceAnimation()
    }

    /**
     * Animates the main UI components when the app starts.
     * Makes the kitchen feel "alive" from the first second.
     */
    private fun playEntranceAnimation() {
        // Slide the receipt machine up from the bottom
        display.translationY = 500f
        display.animate()
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()

        // Fade in the chef
        chefImg.alpha = 0f
        chefImg.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()
    }

    /**
     * Configures click listeners for all interactive elements.
     */
    private fun setupButtons() {
        // Map of Button IDs to their ingredient names
        // Using string resources for localization support (Arabic/English)
        val toppingsMap = mapOf(
            R.id.btnChicken to getString(R.string.topping_chicken),
            R.id.btnBeef to getString(R.string.topping_beef),
            R.id.btnLavash to getString(R.string.topping_lavash),
            R.id.btnLettuce to getString(R.string.topping_lettuce),
            R.id.btnOnions to getString(R.string.topping_onions),
            R.id.btnFries to getString(R.string.topping_fries),
            R.id.btnPickled to getString(R.string.topping_pickled),
            R.id.btnTomatoes to getString(R.string.topping_tomatoes),
            R.id.btnGarlic to getString(R.string.topping_garlic)
        )

        // Assigning click listeners to ingredient buttons
        for ((id, topping) in toppingsMap) {
            findViewById<android.view.View>(id).setOnClickListener {
                addTopping(topping)
            }
        }

        // Operator Logic: DOUBLE (+)
        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener { 
            playClickSound()
            if (selectedToppings.isNotEmpty()) {
                val last = selectedToppings.last()
                selectedToppings.add(last)
                updateDisplay(getString(R.string.double_toppings, last))
                showReaction(getString(R.string.chef_reaction_double))
            }
        }

        // Operator Logic: REMOVE (-)
        findViewById<android.view.View>(R.id.btnSub).setOnClickListener { 
            playClickSound()
            if (selectedToppings.isNotEmpty()) {
                selectedToppings.removeAt(selectedToppings.size - 1)
                updateDisplay()
                showReaction(getString(R.string.chef_reaction_remove))
            }
        }

        // Operator Logic: FAMILY SIZE (x)
        findViewById<android.view.View>(R.id.btnMul).setOnClickListener { 
            playClickSound()
            if (selectedToppings.isNotEmpty()) {
                val copy = ArrayList(selectedToppings)
                selectedToppings.addAll(copy)
                updateDisplay(getString(R.string.family_size))
                showReaction(getString(R.string.chef_reaction_family))
            }
        }

        // Operator Logic: HALF & HALF (/)
        findViewById<android.view.View>(R.id.btnDiv).setOnClickListener {
            playClickSound()
            selectedToppings.add(getString(R.string.separator_half_half))
            updateDisplay(getString(R.string.half_half))
            showReaction(getString(R.string.chef_reaction_half))
        }

        // WRAP / FINALIZE ORDER
        findViewById<Button>(R.id.btnBake).setOnClickListener { 
            playWrapSound()
            wrapOrder()
            showReaction(getString(R.string.chef_reaction_bake))
        }

        // RESET ORDER
        findViewById<Button>(R.id.btnClear).setOnClickListener { 
            playClickSound()
            clearOrder()
            showReaction(getString(R.string.chef_reaction_clear))
        }

        // INFO DIALOG
        findViewById<ImageButton>(R.id.btnInfo).setOnClickListener { showInfoModal() }
    }

    /**
     * Triggers the "Talking Chef" animation and text bubble.
     * Uses image swapping to simulate speech and a bounce animation.
     */
    private fun showReaction(message: String) {
        // Swap to the 'talking' beaver image
        chefImg.setImageResource(R.drawable.beaver_2)
        
        // Start a bounce animation while talking
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.idle_bounce)
        chefImg.startAnimation(bounceAnim)

        chefReaction.text = message
        chefReaction.visibility = android.view.View.VISIBLE
        chefReaction.alpha = 0f
        chefReaction.translationY = 20f
        
        // Pop-up and fade-in animation for the speech bubble
        chefReaction.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                // Auto-hide the bubble after 2000ms
                chefReaction.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setStartDelay(2000)
                    .withEndAction {
                        chefReaction.visibility = android.view.View.INVISIBLE
                        // Revert to idle 'closed mouth' beaver and stop bouncing
                        chefImg.setImageResource(R.drawable.beaver_1)
                        chefImg.clearAnimation()
                    }
                    .start()
            }
            .start()

        // Responsive physical feedback for the chef avatar (Quick scale pop)
        chefImg.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(100)
            .withEndAction {
                chefImg.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /**
     * Staggers a gentle bounce animation for all ingredient buttons.
     */
    private fun startIdleAnimations() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.idle_bounce)
        val buttonIds = listOf(
            R.id.btnChicken, R.id.btnBeef, R.id.btnLavash,
            R.id.btnLettuce, R.id.btnOnions, R.id.btnFries,
            R.id.btnPickled, R.id.btnTomatoes, R.id.btnGarlic,
            R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv
        )

        buttonIds.forEachIndexed { index, id ->
            val view = findViewById<android.view.View>(id)
            view.postDelayed({
                view.startAnimation(bounceAnim)
            }, index * 80L) // 80ms stagger for a smooth flow
        }
    }

    private fun playClickSound() {
        clickPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }

    private fun playWrapSound() {
        wrapPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clickPlayer?.release()
        wrapPlayer?.release()
        clickPlayer = null
        wrapPlayer = null
    }

    private fun addTopping(topping: String) {
        playClickSound()
        selectedToppings.add(topping)
        updateDisplay()
        showReaction(getString(R.string.chef_reaction_add))
    }

    /**
     * Updates the text display (Receipt) with a physics-based "shivering" effect.
     */
    private fun updateDisplay(extraMessage: String? = null) {
        val oldText = display.text.toString()
        val newText: String
        
        if (selectedToppings.isEmpty()) {
            newText = getString(R.string.select_toppings)
        } else {
            val orderText = StringBuilder(getString(R.string.order_prefix))
            selectedToppings.forEach { orderText.append("- $it\n") }
            if (extraMessage != null) {
                orderText.append("\n$extraMessage")
            }
            newText = orderText.toString()
        }

        if (oldText != newText) {
            display.text = newText
            // Visual feedback: Receipt "vibrates" slightly when updated
            display.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(50)
                .withEndAction {
                    display.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                }
                .start()
        }
    }

    private fun clearOrder() {
        selectedToppings.clear()
        updateDisplay()
    }

    private fun wrapOrder() {
        if (selectedToppings.isEmpty()) {
            display.text = getString(R.string.error_no_ingredients)
        } else {
            val separator = getString(R.string.separator_half_half)
            val actualToppingsCount = selectedToppings.count { it != separator }
            display.text = resources.getQuantityString(R.plurals.order_ready_plural, actualToppingsCount, actualToppingsCount)
            selectedToppings.clear()
            
            // Grand finale animation for the display
            display.animate()
                .rotationX(360f)
                .setDuration(500)
                .withEndAction { display.rotationX = 0f }
                .start()
        }
    }

    private fun showInfoModal() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
