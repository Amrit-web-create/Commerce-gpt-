package com.amrit.commercehub11

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun boldParse(s: String): AnnotatedString = buildAnnotatedString {
    val parts = s.split("**")
    parts.forEachIndexed { i, p ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p)
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    val pal = LocalPal.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.card)
            .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun SectionHead(icon: ImageVector, title: String, sub: String) {
    val pal = LocalPal.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(pal.accent).padding(8.dp)) {
            Icon(icon, null, tint = pal.onAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = pal.textStrong)
            Text(sub, fontSize = 12.sp, color = pal.sub)
        }
    }
}

@Composable
fun SettingsScreen(themeMode: String, onTheme: (String) -> Unit, notifOn: Boolean, onNotif: (Boolean) -> Unit, onBack: () -> Unit, onContact: () -> Unit) {
    val pal = LocalPal.current
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(pal.bg)) {
        Column(Modifier.fillMaxWidth().background(pal.header)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SquareIconBtn(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Spacer(Modifier.width(12.dp))
                Text("App Settings", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = pal.text, letterSpacing = 0.4.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 512.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Theme
                SectionCard {
                    SectionHead(Icons.Outlined.DarkMode, "App Theme", "Choose your preferred viewing mode")
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeBtn("Light", Icons.Outlined.LightMode, themeMode == "light", Modifier.weight(1f)) { onTheme("light") }
                        ThemeBtn("Dark", Icons.Outlined.DarkMode, themeMode == "dark", Modifier.weight(1f)) { onTheme("dark") }
                        ThemeBtn("Auto", Icons.Outlined.PhoneAndroid, themeMode == "auto", Modifier.weight(1f)) { onTheme("auto") }
                    }
                }
                // Notifications (default OFF)
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionHead(Icons.Outlined.Notifications, "Notifications", if (notifOn) "Message alerts are ON" else "Message alerts are OFF") }
                        SupportSwitch(notifOn, onNotif)
                    }
                }
                // Contact
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.card)
                        .border(1.dp, pal.cardLine, RoundedCornerShape(16.dp)).tap { onContact() }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { SectionHead(Icons.Outlined.Mail, "Contact Us", "Have a query? Send us a message") }
                    Icon(Icons.Filled.ChevronRight, null, tint = pal.sub, modifier = Modifier.size(20.dp))
                }
                // About
                SectionCard {
                    SectionHead(Icons.Outlined.Info, "About Us", "Our mission and vision for commerce students")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                        ABOUT_PARAGRAPHS.forEach { Text(boldParse(it), fontSize = 12.sp, color = pal.body, lineHeight = 19.5.sp) }
                    }
                }
                // Privacy
                SectionCard {
                    SectionHead(Icons.Outlined.Security, "Privacy Policy & Safety", "Your data safety is our highest priority")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                        PRIVACY_PARAGRAPHS.forEach { Text(boldParse(it), fontSize = 12.sp, color = pal.body, lineHeight = 19.5.sp) }
                    }
                }
                MadeInIndia()
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ThemeBtn(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current
    val bg = if (active) pal.accent else (if (pal.dark) Slate.s800 else Color.White)
    val fg = if (active) pal.onAccent else (if (pal.dark) Slate.s200 else Slate.s800)
    val bd = if (active) pal.accent else pal.cardLine
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(12.dp))
            .tap { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** Contact Us page. Subject me secret word likhne par private chat khulta hai, baaki sab ke liye "sent" toast. */
@Composable
fun ContactScreen(onBack: () -> Unit, onSecret: () -> Unit) {
    val pal = LocalPal.current
    BackHandler { onBack() }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var wa by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(pal.bg).imePadding()) {
        Column(Modifier.fillMaxWidth().background(pal.header)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(50)).tap { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = pal.text, modifier = Modifier.size(24.dp))
                }
                Text("Contact Us", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = pal.text, modifier = Modifier.padding(start = 6.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(pal.line))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 40.dp)
            ) {
                Text("Contact Us", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = pal.text)
                Text("For any query, please send us a message.", fontSize = 13.5.sp, color = Slate.s500, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
                FormField("Name", name, { name = it })
                FormField("Email", email, { email = it }, KeyboardType.Email)
                FormField("Phone number", phone, { phone = it }, KeyboardType.Phone)
                FormField("WhatsApp number", wa, { wa = it }, KeyboardType.Phone)
                FormField("Subject", subject, { subject = it })
                FormField("Message", msg, { msg = it }, multiline = true)
                Box(
                    Modifier.fillMaxWidth().padding(top = 22.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (pal.dark) Color.White else Slate.s900)
                        .tap {
                            val s = subject.trim().lowercase()
                            name = ""; email = ""; phone = ""; wa = ""; subject = ""; msg = ""
                            if (s == SECRET_WORD) onSecret() else Toaster.show("Message sent successfully \u2713")
                        }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Send", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (pal.dark) Slate.s900 else Color.White)
                }
            }
        }
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, type: KeyboardType = KeyboardType.Text, multiline: Boolean = false) {
    val pal = LocalPal.current
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pal.dark) Slate.s400 else Slate.s600, modifier = Modifier.padding(top = 14.dp, bottom = 5.dp))
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (pal.dark) Slate.s900 else Slate.s50)
            .border(1.dp, if (pal.dark) Slate.s800 else Slate.s200, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = !multiline,
            minLines = if (multiline) 4 else 1,
            textStyle = TextStyle(fontSize = 15.sp, color = pal.text),
            cursorBrush = SolidColor(pal.text),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
