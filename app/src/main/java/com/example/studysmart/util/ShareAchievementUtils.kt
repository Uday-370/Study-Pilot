package com.example.studysmart.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.studysmart.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

object ShareAchievementUtils {

    fun shareToInstagramStory(context: Context, name: String, totalHours: Float, currentStreak: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val cx = width / 2f
            val cy = height / 2f

            // ==========================================
            // 1. DYNAMIC TITLE LOGIC
            // ==========================================
            val titleText = when {
                currentStreak == 0 -> "THE JOURNEY BEGINS"
                currentStreak in 1..2 -> "TRAINING IN PROGRESS"
                currentStreak in 3..6 -> "MOMENTUM BUILDING"
                currentStreak in 7..13 -> "UNBREAKABLE FOCUS"
                else -> "ELITE MASTERY ACHIEVED"
            }

            // ==========================================
            // 2. ZEN AURORA BACKGROUND (Sage & Rose)
            // ==========================================
            // Rich charcoal base
            canvas.drawColor(Color.parseColor("#0C0F0D"))

            // Sage Green Glow (Top Left)
            val nebula1 = Paint().apply {
                shader = RadialGradient(cx - 200f, -100f, 1500f,
                    intArrayOf(Color.parseColor("#406B8A7A"), Color.parseColor("#102A3B32"), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), nebula1)

            // Soft Rose Glow (Bottom Right)
            val nebula2 = Paint().apply {
                shader = RadialGradient(width.toFloat() + 200f, height.toFloat() + 200f, 1400f,
                    Color.parseColor("#30D48A88"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), nebula2)

            // ==========================================
            // 3. FOCUS EMBERS (Dynamic Particle Field)
            // ==========================================
            val random = Random(currentStreak.toLong())
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (i in 0..120) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                val radius = random.nextFloat() * 4f + 1f
                val alpha = random.nextInt(120) + 30

                // Embers are a mix of white and soft sage
                val emberColor = if (random.nextBoolean()) Color.WHITE else Color.parseColor("#DDF0E6")
                starPaint.color = Color.argb(alpha, Color.red(emberColor), Color.green(emberColor), Color.blue(emberColor))

                if (radius > 3f) starPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#80FFFFFF"))
                else starPaint.clearShadowLayer()
                canvas.drawCircle(x, y, radius, starPaint)
            }

            // ==========================================
            // 4. THE SACRED GEOMETRY (The Mascot's Halo)
            // ==========================================
            val mascotY = 480f
            val geoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#806B8A7A") // Sage shadow
                setShadowLayer(20f, 0f, 0f, Color.parseColor("#6B8A7A"))
            }

            canvas.save()
            canvas.rotate(45f, cx, mascotY)
            canvas.drawRect(cx - 180f, mascotY - 180f, cx + 180f, mascotY + 180f, geoPaint)
            canvas.rotate(45f, cx, mascotY)
            geoPaint.color = Color.parseColor("#40D48A88") // Rose inner diamond
            canvas.drawRect(cx - 150f, mascotY - 150f, cx + 150f, mascotY + 150f, geoPaint)
            canvas.restore()

            val mascotSize = 180
            val mascot = ContextCompat.getDrawable(context, R.drawable.ic_self_improvement1)
            mascot?.let {
                it.setBounds((cx - mascotSize / 2).toInt(), (mascotY - mascotSize / 2).toInt(),
                    (cx + mascotSize / 2).toInt(), (mascotY + mascotSize / 2).toInt())
                it.setTint(Color.WHITE)
                it.draw(canvas)
            }

            // ==========================================
            // 5. THE FROSTED GLASS SLAB (The Card)
            // ==========================================
            val cardRect = RectF(80f, 680f, width - 80f, 1580f)

            val cardShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                setShadowLayer(100f, 0f, 40f, Color.parseColor("#CC000000"))
            }
            canvas.drawRoundRect(cardRect, 60f, 60f, cardShadow)

            val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F1A211D") // Deep tinted glass
            }
            canvas.drawRoundRect(cardRect, 60f, 60f, glassPaint)

            // Iridescent Foil (Sage & Rose)
            val foilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#20FFFFFF"), Color.parseColor("#30D48A88"), Color.parseColor("#306B8A7A"), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f), Shader.TileMode.CLAMP)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            canvas.drawRoundRect(cardRect, 60f, 60f, foilPaint)

            // Smooth Theme Border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = SweepGradient(cx, cardRect.centerY(), intArrayOf(
                    Color.parseColor("#6B8A7A"), Color.parseColor("#FFFFFF"), Color.parseColor("#D48A88"),
                    Color.parseColor("#FFFFFF"), Color.parseColor("#6B8A7A")
                ), null)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(cardRect, 60f, 60f, borderPaint)

            // ==========================================
            // 6. TYPOGRAPHY
            // ==========================================
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EBDCC3") // Soft champagne gold
                textSize = 50f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.15f
                textAlign = Paint.Align.CENTER
                setShadowLayer(10f, 0f, 4f, Color.parseColor("#80000000"))
            }
            canvas.drawText(titleText, cx, 780f, titlePaint)

            // Metallic Number (Sage to Rose gradient)
            val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 420f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                shader = LinearGradient(0f, 800f, 0f, 1200f,
                    intArrayOf(Color.WHITE, Color.parseColor("#D48A88"), Color.parseColor("#8DAA9D")),
                    null, Shader.TileMode.CLAMP)
                setShadowLayer(25f, 0f, 10f, Color.parseColor("#99000000"))
            }
            canvas.drawText("$currentStreak", cx, 1180f, streakPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D48A88")
                textSize = 65f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.1f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("DAY STREAK", cx, 1300f, labelPaint)

            canvas.drawLine(cx - 150f, 1380f, cx + 150f, 1380f, Paint().apply { color = Color.WHITE; alpha = 40; strokeWidth = 2f })

            val hoursPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 50f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                alpha = 180
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.05f
            }
            canvas.drawText("TOTAL FOCUS TIME: ${totalHours.toInt()} HRS", cx, 1460f, hoursPaint)

            // ==========================================
            // 7. THE QUOTE
            // ==========================================
            val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 140
                textSize = 42f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("\"Consistency is the silent engine", cx, 1680f, quotePaint)
            canvas.drawText("of extraordinary results.\"", cx, 1745f, quotePaint)

            // ==========================================
            // 8. FINAL BRAND TAG (Cleaned up)
            // ==========================================
            val safeName = if (name.isBlank() || name == "Student") "PILOT" else name.uppercase()
            val footerLabel = "STUDYPILOT • $safeName"

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.1f
                textAlign = Paint.Align.CENTER
            }

            val textWidth = footerPaint.measureText(footerLabel)
            val footerRect = RectF(cx - textWidth/2 - 50, 1800f, cx + textWidth/2 + 50, 1880f)

            val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#20FFFFFF")
            }
            canvas.drawRoundRect(footerRect, 100f, 100f, footerBg)

            val textMetrics = footerPaint.fontMetrics
            val textOffset = (textMetrics.descent + textMetrics.ascent) / 2
            canvas.drawText(footerLabel, cx, footerRect.centerY() - textOffset, footerPaint)

            // ==========================================
            // 9. SAVE & FIRE THE SHARE INTENT
            // ==========================================
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "achievement.png")

            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            bitmap.recycle()

            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Achievement"))
            }
        }
    }
}