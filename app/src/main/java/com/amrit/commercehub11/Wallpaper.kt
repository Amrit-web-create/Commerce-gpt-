package com.amrit.commercehub11

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.PathParser

/** Original app ka doodle wallpaper (SVG icons) native canvas me draw hota hai */
object Wall {
    private val icons: List<List<Path>> by lazy {
        WallData.icons.map { list ->
            list.mapNotNull { d -> try { PathParser.createPathFromPathData(d) } catch (_: Throwable) { null } }
        }
    }

    fun tile(density: Float, uses: List<FloatArray>, stroke: Int): Bitmap {
        val px = (384f * density).toInt().coerceAtLeast(64)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(density, density)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.8f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = stroke
        for (u in uses) {
            val ic = icons.getOrNull(u[0].toInt()) ?: continue
            c.save()
            c.translate(u[1], u[2]); c.rotate(u[3]); c.scale(u[4], u[4]); c.translate(-24f, -24f)
            ic.forEach { c.drawPath(it, p) }
            c.restore()
        }
        return bmp
    }
}

class WallOpt(val key: String, val label: String, val lightBase: Long, val lightStroke: Long, val darkBase: Long, val darkStroke: Long)

val WALL_OPTS = listOf(
    WallOpt("default", "Default", 0xFFF4F0E7, 0x57B8946C, 0xFF0B141A, 0x12FFFFFF),
    WallOpt("white", "White", 0xFFFFFFFF, 0x14000000, 0xFF161B1F, 0x0FFFFFFF),
    WallOpt("blue", "Blue", 0xFFDCE9FF, 0x4D4F7FCF, 0xFF0C1A2E, 0x14FFFFFF),
    WallOpt("red", "Red", 0xFFFFE3E3, 0x4DC95B5B, 0xFF2A0D0D, 0x14FFFFFF),
    WallOpt("pink", "Pink", 0xFFFFE2F0, 0x4DCC5C97, 0xFF2A0D1D, 0x14FFFFFF)
)

fun wallOpt(key: String): WallOpt = WALL_OPTS.firstOrNull { it.key == key } ?: WALL_OPTS[0]

/** chat = true => chat wala pattern (ghana), false => inbox wala (halka, door door) */
@Composable
fun PatternBg(base: Color, stroke: Int, chat: Boolean, dark: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val density = LocalDensity.current.density
    val uses = if (chat) (if (dark) WallData.chatUsesDark else WallData.chatUses) else (if (dark) WallData.inboxUsesDark else WallData.inboxUses)
    val brush = remember(uses, stroke, density) {
        ShaderBrush(BitmapShader(Wall.tile(density, uses, stroke), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
    }
    Box(modifier.background(base).drawBehind { drawRect(brush) }) { content() }
}
