package com.amrit.commercehub11

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SupportSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val pal = LocalPal.current
    val track = if (checked) (if (pal.dark) Color.White else Color.Black) else (if (pal.dark) Slate.s700 else Slate.s300)
    val thumb = if (checked && pal.dark) Color.Black else Color.White
    val x by animateDpAsState(if (checked) 18.dp else 2.dp, tween(200), label = "sw")
    Box(
        modifier = Modifier
            .width(36.dp).height(20.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .plainClick { onChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = x, y = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(thumb)
        )
    }
}

@Composable
fun SquareIconBtn(icon: ImageVector, onClick: () -> Unit, size: Int = 20, pad: Int = 8) {
    val pal = LocalPal.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pal.chip)
            .border(1.dp, pal.chipLine, RoundedCornerShape(12.dp))
            .tap { onClick() }
            .padding(pad.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = pal.textStrong, modifier = Modifier.size(size.dp))
    }
}

@Composable
fun AppHeader(
    supportOn: Boolean, onSupport: (Boolean) -> Unit, onSettings: () -> Unit,
    query: String, onQuery: (String) -> Unit
) {
    val pal = LocalPal.current
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(pal.header)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(pal.accent).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.School, null, tint = pal.onAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Commerce Hub 11", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = pal.text, letterSpacing = 0.4.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pal.dark) Slate.s300 else Slate.s700)
                Spacer(Modifier.width(8.dp))
                SupportSwitch(supportOn, onSupport)
                Spacer(Modifier.width(12.dp))
                SquareIconBtn(Icons.Outlined.Settings, onSettings)
            }
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.chip)
                    .border(if (focused) 2.dp else 1.dp, if (focused) pal.accent else pal.chipLine, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = Slate.s400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search by Book Title, Subject or Author...", fontSize = 14.sp, color = Slate.s400,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query, onValueChange = onQuery, singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = pal.text),
                        cursorBrush = SolidColor(pal.text),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
    }
}

@Composable
fun BottomNav(tab: String, onTab: (String) -> Unit) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxWidth().background(pal.header)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem("Home", Icons.Outlined.Home, tab == "home") { onTab("home") }
            NavItem("My Books", Icons.Outlined.BookmarkBorder, tab == "mybooks") { onTab("mybooks") }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    val c = if (active) pal.textStrong else pal.muted
    Column(Modifier.tap { onClick() }.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = c, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c)
    }
}

@Composable
fun HeroSlider(onOpen: (Book) -> Unit) {
    val featured = booksData.take(4)
    if (featured.isEmpty()) return
    val pager = rememberPagerState { featured.size }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val next = (pager.currentPage + 1) % featured.size
            try { pager.animateScrollToPage(next, 0f, tween(700)) } catch (_: Throwable) {}
        }
    }
    Box(
        Modifier.fillMaxWidth().height(224.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)).background(Slate.s900)
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val book = featured[page]
            Row(
                Modifier.fillMaxSize().plainClick { onOpen(book) }.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(2f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        book.medium.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        book.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp
                    )
                    Text("Tap to read chapters instantly", fontSize = 12.sp, color = Slate.s300)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AssetImage(
                        book.cover,
                        Modifier.height(144.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        ContentScale.Fit, 500
                    )
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            featured.indices.forEach { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 6.dp, tween(300), label = "dot")
                Box(
                    Modifier.width(w).height(6.dp).clip(RoundedCornerShape(50))
                        .background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun BookCard(book: Book, saved: Boolean, modifier: Modifier, onOpen: () -> Unit) {
    val pal = LocalPal.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)).background(pal.card)
            .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp))
            .tap { onOpen() }.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(12.dp)).background(pal.coverBox)
                .border(1.dp, pal.coverLine, RoundedCornerShape(12.dp)).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AssetImage(book.cover, Modifier.fillMaxSize(), ContentScale.Fit, 600)
        }
        Spacer(Modifier.height(10.dp))
        if (saved) {
            Text("SAVED BOOK", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                color = if (pal.dark) Emerald400 else Emerald600)
        } else {
            Text(book.medium.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = pal.sub)
        }
        Text(
            book.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.text, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp)
        )
        if (!saved) {
            Text("${book.chapters.size} Chapters", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = pal.muted,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun BookGrid(list: List<Book>, saved: Boolean, emptyText: String, onOpen: (Book) -> Unit) {
    val pal = LocalPal.current
    if (list.isEmpty()) {
        Text(
            emptyText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = if (saved) 80.dp else 48.dp, horizontal = 16.dp)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        list.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { b -> BookCard(b, saved, Modifier.weight(1f)) { onOpen(b) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MadeInIndia() {
    val pal = LocalPal.current
    Text(
        "Made in \u2764\uFE0F India \u2022 Made for \u2764\uFE0F India",
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.sub, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp)
    )
}

@Composable
fun HomeTab(query: String, onOpen: (Book) -> Unit) {
    val pal = LocalPal.current
    val q = query.lowercase().trim()
    val list = if (q.isEmpty()) booksData else booksData.filter { it.title.lowercase().contains(q) || it.medium.lowercase().contains(q) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        HeroSlider(onOpen)
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("NCERT & Core Books", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = pal.textStrong)
            Text("Explore the best books for your learning journey", fontSize = 12.sp, color = pal.sub)
        }
        BookGrid(list, false, "No books found matching your search.", onOpen)
        MadeInIndia()
    }
}

@Composable
fun MyBooksTab(savedIds: List<String>, onOpen: (Book) -> Unit) {
    val pal = LocalPal.current
    val list = booksData.filter { savedIds.contains(it.id) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("My Saved Books", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = pal.textStrong,
            modifier = Modifier.padding(horizontal = 4.dp))
        BookGrid(list, true, "No books saved in My Books yet. Tap 'Add to My Books' on any book to save it here!", onOpen)
    }
}

@Composable
fun BookDetail(book: Book, saved: Boolean, onBack: () -> Unit, onToggleSave: () -> Unit, onChapter: (Chapter) -> Unit) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(pal.chip).tap { onBack() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.text, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.text)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.card)
                .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().height(192.dp).clip(RoundedCornerShape(12.dp)).background(pal.coverBox)
                    .border(1.dp, pal.cardLine, RoundedCornerShape(12.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) { AssetImage(book.cover, Modifier.fillMaxSize(), ContentScale.Fit, 800) }
            Text(book.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = pal.text, textAlign = TextAlign.Center)
            Text("${book.medium} \u2022 NCERT Edition", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.sub)
            val bg = if (saved) (if (pal.dark) Emerald950.copy(alpha = 0.4f) else Emerald50) else pal.accent
            val fg = if (saved) (if (pal.dark) Emerald400 else Emerald700) else pal.onAccent
            val bd = if (saved) (if (pal.dark) Emerald800 else Emerald300) else Color.Transparent
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(bg)
                    .border(BorderStroke(1.dp, bd), RoundedCornerShape(12.dp))
                    .tap { onToggleSave() }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (saved) Icons.Filled.Check else Icons.Outlined.BookmarkBorder, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved in My Books" else "Add to My Books", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CHAPTERS LIST", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = pal.muted,
                letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                book.chapters.forEachIndexed { index, chap ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.card)
                            .border(1.dp, pal.cardLine, RoundedCornerShape(12.dp))
                            .tap { onChapter(chap) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("CH-${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pal.muted)
                            Text(chap.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.text,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(if (pal.dark) Slate.s800 else Color.White)
                                .border(1.dp, pal.chipLine, RoundedCornerShape(8.dp)).padding(8.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, null, tint = if (pal.dark) Slate.s300 else Slate.s600, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
