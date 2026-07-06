package com.sketchbook.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** A single sampled point of a stroke (canvas coordinates, pressure 0..1). */
@Serializable
data class StrokePoint(val x: Float, val y: Float, val p: Float = 1f)

/** A committed vector stroke on a layer. */
@Serializable
data class Stroke(
    val brush: BrushType = BrushType.PEN,
    val colorHex: String = "#111418",
    val width: Float = 5f,
    val points: List<StrokePoint> = emptyList(),
    val isEraser: Boolean = false,
    val eraserWidth: Float = 24f,
    val seed: Long = 0L,
)

/** One drawing layer: an optional raster base image with vector strokes above it. */
@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val strokes: List<Stroke> = emptyList(),
    val imageBase64: String? = null,
    val isReference: Boolean = false,
)

/** A page in the sketchbook — its own layer stack. */
@Serializable
data class Page(
    val id: String = UUID.randomUUID().toString(),
    val layers: List<Layer> = listOf(Layer(name = "Layer 1")),
    val activeLayerIndex: Int = 0,
)

/** The persisted sketch document (JSON `.sketch` file in app storage). */
@Serializable
data class SketchDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val template: TemplateKind = TemplateKind.BLANK,
    val canvasWidth: Float = 2048f,
    val canvasHeight: Float = 1536f,
    val backgroundHex: String = "#FFFFFF",
    val pages: List<Page> = listOf(Page()),
    val currentPageIndex: Int = 0,
    val thumbnailBase64: String? = null,
    val isFavorite: Boolean = false,
) {
    val currentPage: Page get() = pages[currentPageIndex.coerceIn(0, pages.size - 1)]
    val fileName: String get() = "$id.sketch"
}

enum class TemplateKind(val title: String) {
    BLANK("Blank"),
    RING_FILE("Ring File"),
    RULED("Ruled"),
    GRID("Grid"),
    DOT_GRID("Dot Grid"),
    ISOMETRIC("Isometric"),
    STORYBOARD("Storyboard"),
    MUSIC_STAFF("Music Staff"),
}

/** Brush library — mirrors the iOS BrushType presets. */
enum class BrushType(
    val title: String,
    val category: Category,
    val defaultWidth: Float,
    val opacity: Float,
) {
    // Inking
    PEN("Studio Pen", Category.INKING, 5f, 1f),
    FOUNTAIN("Fountain Pen", Category.INKING, 7f, 1f),
    MONOLINE("Monoline", Category.INKING, 5f, 1f),
    TECHNICAL("Technical Pen", Category.INKING, 3f, 1f),
    GEL("Gel Pen", Category.INKING, 8f, 0.95f),
    BRUSH_PEN("Brush Pen", Category.INKING, 10f, 0.9f),
    // Sketching
    PENCIL("Pencil", Category.SKETCHING, 4f, 1f),
    CHARCOAL("Charcoal", Category.SKETCHING, 10f, 0.7f),
    CRAYON("Crayon", Category.SKETCHING, 12f, 0.85f),
    CHALK("Chalk", Category.SKETCHING, 14f, 0.6f),
    PASTEL("Soft Pastel", Category.SKETCHING, 16f, 0.7f),
    // Painting
    MARKER("Marker", Category.PAINTING, 18f, 0.5f),
    HIGHLIGHTER("Highlighter", Category.PAINTING, 22f, 0.35f),
    OIL("Oil Paint", Category.PAINTING, 20f, 0.85f),
    GOUACHE("Gouache", Category.PAINTING, 20f, 0.7f),
    ACRYLIC("Acrylic", Category.PAINTING, 16f, 1f),
    WATERCOLOR("Watercolor", Category.PAINTING, 24f, 0.5f),
    AIRBRUSH("Airbrush", Category.PAINTING, 26f, 0.35f),
    INK("Ink Bleed", Category.PAINTING, 12f, 0.8f),
    STARDUST("Stardust", Category.PAINTING, 20f, 1f);

    enum class Category(val title: String) {
        INKING("Inking"), SKETCHING("Sketching"), PAINTING("Painting")
    }

    /** Whether segment width should follow stylus pressure / speed. */
    val pressureSensitive: Boolean
        get() = this in setOf(PEN, FOUNTAIN, BRUSH_PEN, PENCIL, CHARCOAL, INK)

    /** Soft (blurred) edge radius factor, 0 = crisp. */
    val softness: Float
        get() = when (this) {
            CHARCOAL, CHALK, PASTEL -> 0.35f
            WATERCOLOR, INK -> 0.5f
            AIRBRUSH -> 1.0f
            OIL, GOUACHE -> 0.2f
            else -> 0f
        }

    companion object {
        const val MIN_SIZE = 1f
        const val MAX_SIZE = 60f
    }
}

/** Graphite pencil grades (width + gray level, 0 = black). */
enum class PencilGrade(val title: String, val width: Float, val whiteLevel: Float) {
    H2("2H", 3f, 0.55f),
    H("H", 3.5f, 0.45f),
    HB("HB", 4f, 0.32f),
    B2("2B", 6f, 0.18f),
    B4("4B", 8f, 0.08f),
    B6("6B", 10f, 0f);

    val hex: String
        get() {
            val v = (whiteLevel * 255).toInt().coerceIn(0, 255)
            return "#%02X%02X%02X".format(v, v, v)
        }
}

enum class ShapeKind(val title: String, val canFill: Boolean = true) {
    RECTANGLE("Rectangle"),
    ELLIPSE("Circle"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    STAR("Star"),
    PENTAGON("Pentagon"),
    HEXAGON("Hexagon"),
    ARROW("Arrow"),
    HEART("Heart"),
    LINE("Line", canFill = false),
}

enum class SymmetryMode(val title: String) {
    OFF("Off"), VERTICAL("Vertical"), HORIZONTAL("Horizontal"), QUAD("Quad")
}

/** Canvas size presets (portrait pixel sizes; landscape flips, except square). */
enum class CanvasPreset(val title: String, val subtitle: String, val pw: Int, val ph: Int) {
    SQUARE("Square", "1:1", 2048, 2048),
    STANDARD("Standard", "3:4", 1536, 2048),
    WIDE("Wide", "9:16", 1170, 2080),
    A4("A4", "ISO A4", 1654, 2339),
    LETTER("Letter", "US Letter", 1700, 2200);

    fun width(landscape: Boolean): Int = if (landscape && this != SQUARE) ph else pw
    fun height(landscape: Boolean): Int = if (landscape && this != SQUARE) pw else ph
}

enum class PaperColor(val title: String, val hex: String) {
    WHITE("White", "#FFFFFF"),
    CREAM("Cream", "#FBF6EC"),
    GRAY("Gray", "#3A3D42"),
    BLACK("Black", "#111418"),
}
