package com.example.calcnote

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal

// ----------------- Ranglar -----------------
private val BLUE = Color(0xFF1E88E5)
private val DARKBLUE = Color(0xFF1565C0)
private val GREYBG = Color(0xFFEFEFEF)
private val LINE_NUM = Color(0xFFC62828)
private val FOCUS_BG = Color(0xFFF0F7FF)

// ----------------- ViewModel (mantiq + saqlash) -----------------
class CalcViewModel(app: Application) : AndroidViewModel(app) {

    private val notesDir: File by lazy {
        File(getApplication<Application>().filesDir, "notes").apply { mkdirs() }
    }
    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("calcnote", Context.MODE_PRIVATE)
    }

    val lines = androidx.compose.runtime.mutableStateListOf(TextFieldValue(""))
    var focused by mutableStateOf(0)
    var currentNote by mutableStateOf("Hisob 1")

    private val undoStack = ArrayDeque<List<String>>()
    private val redoStack = ArrayDeque<List<String>>()

    init {
        currentNote = prefs.getString("current", "Hisob 1") ?: "Hisob 1"
        loadNote(currentNote)
    }

    // ---- snapshot / undo ----
    private fun snapshot(): List<String> = lines.map { it.text }

    private fun applyTexts(texts: List<String>) {
        lines.clear()
        if (texts.isEmpty()) lines.add(TextFieldValue(""))
        else texts.forEach { lines.add(TextFieldValue(it)) }
        if (focused > lines.lastIndex) focused = lines.lastIndex
        if (focused < 0) focused = 0
    }

    private fun pushUndo() {
        undoStack.addLast(snapshot())
        if (undoStack.size > 100) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(snapshot())
        applyTexts(undoStack.removeLast())
        save()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(snapshot())
        applyTexts(redoStack.removeLast())
        save()
    }

    // ---- tahrirlash (klaviaturadan) ----
    fun input(s: String) {
        pushUndo()
        val tfv = lines[focused]
        val start = tfv.selection.start.coerceIn(0, tfv.text.length)
        val end = tfv.selection.end.coerceIn(0, tfv.text.length)
        val newText = tfv.text.substring(0, start) + s + tfv.text.substring(end)
        lines[focused] = tfv.copy(text = newText, selection = TextRange(start + s.length))
        save()
    }

    fun backspace() {
        pushUndo()
        val tfv = lines[focused]
        val start = tfv.selection.start
        val end = tfv.selection.end
        if (start != end) {
            val newText = tfv.text.substring(0, start) + tfv.text.substring(end)
            lines[focused] = tfv.copy(text = newText, selection = TextRange(start))
        } else if (start > 0) {
            val newText = tfv.text.substring(0, start - 1) + tfv.text.substring(start)
            lines[focused] = tfv.copy(text = newText, selection = TextRange(start - 1))
        } else if (focused > 0) {
            val prev = lines[focused - 1]
            val mergePos = prev.text.length
            lines[focused - 1] = prev.copy(text = prev.text + tfv.text, selection = TextRange(mergePos))
            lines.removeAt(focused)
            focused -= 1
        }
        save()
    }

    fun newLine() {
        pushUndo()
        val tfv = lines[focused]
        val p = tfv.selection.start.coerceIn(0, tfv.text.length)
        val before = tfv.text.substring(0, p)
        val after = tfv.text.substring(p)
        lines[focused] = tfv.copy(text = before, selection = TextRange(before.length))
        lines.add(focused + 1, TextFieldValue(after, selection = TextRange(0)))
        focused += 1
        save()
    }

    fun moveCursor(delta: Int) {
        val tfv = lines[focused]
        val cur = (tfv.selection.start + delta).coerceIn(0, tfv.text.length)
        lines[focused] = tfv.copy(selection = TextRange(cur))
    }

    fun focusLine(i: Int) {
        focused = i
        val t = lines[i].text
        lines[i] = lines[i].copy(selection = TextRange(t.length))
    }

    fun clearLine() {
        pushUndo()
        lines[focused] = TextFieldValue("")
        save()
    }

    fun clearAll() {
        pushUndo()
        lines.clear()
        lines.add(TextFieldValue(""))
        focused = 0
        save()
    }

    // ---- natijalar ----
    fun total(): BigDecimal {
        var sum = BigDecimal.ZERO
        var ans = BigDecimal.ZERO
        lines.forEach { l ->
            val r = Calc.eval(l.text, ans)
            if (r != null) { sum = sum.add(r); ans = r }
        }
        return sum
    }

    // ---- fayl saqlash (ma'lumot yo'qolmaydi) ----
    private fun sanitize(name: String) =
        name.replace(Regex("[^a-zA-Z0-9 _-]"), "_").trim().ifBlank { "Hisob" }

    private fun fileFor(name: String) = File(notesDir, sanitize(name) + ".txt")

    fun save() {
        try {
            fileFor(currentNote).writeText(lines.joinToString("\n") { it.text })
            prefs.edit().putString("current", currentNote).apply()
        } catch (_: Exception) {}
    }

    fun loadNote(name: String) {
        currentNote = name
        val f = fileFor(name)
        val content = if (f.exists()) f.readText() else ""
        val parts = if (content.isEmpty()) listOf("") else content.split("\n")
        lines.clear()
        parts.forEach { lines.add(TextFieldValue(it)) }
        if (lines.isEmpty()) lines.add(TextFieldValue(""))
        focused = 0
        undoStack.clear(); redoStack.clear()
        prefs.edit().putString("current", currentNote).apply()
    }

    fun listNotes(): List<String> {
        val files = notesDir.listFiles { f -> f.extension == "txt" }?.toList() ?: emptyList()
        val names = files.sortedByDescending { it.lastModified() }.map { it.nameWithoutExtension }
        return if (names.isEmpty()) listOf(currentNote) else names
    }

    fun newNote() {
        save()
        var n = 1
        var name: String
        do { name = "Hisob $n"; n++ } while (fileFor(name).exists())
        currentNote = name
        lines.clear(); lines.add(TextFieldValue("")); focused = 0
        undoStack.clear(); redoStack.clear()
        save()
    }

    fun deleteNote(name: String) {
        try { fileFor(name).delete() } catch (_: Exception) {}
        if (name == currentNote) {
            val remaining = listNotes().filter { it != name }
            if (remaining.isNotEmpty()) loadNote(remaining.first()) else newNote()
        }
    }

    fun renameCurrent(newName: String) {
        val old = fileFor(currentNote)
        val nw = fileFor(newName)
        try {
            val content = if (old.exists()) old.readText() else lines.joinToString("\n") { it.text }
            nw.writeText(content)
            if (old.exists() && old.absolutePath != nw.absolutePath) old.delete()
            currentNote = newName
            prefs.edit().putString("current", currentNote).apply()
        } catch (_: Exception) {}
    }
}

// ----------------- Activity -----------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = BLUE)) {
                CalcApp()
            }
        }
    }
}

@Composable
fun CalcApp(vm: CalcViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showFiles by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onNew = { vm.newNote(); scope.launch { drawerState.close() } },
                    onSave = { vm.save(); toast(context, "Saqlandi"); scope.launch { drawerState.close() } },
                    onFiles = { showFiles = true; scope.launch { drawerState.close() } },
                    onRename = { showRename = true; scope.launch { drawerState.close() } },
                    onShare = { shareText(context, vm); scope.launch { drawerState.close() } },
                    onUndo = { vm.undo() },
                    onRedo = { vm.redo() },
                    onClear = { vm.clearAll(); scope.launch { drawerState.close() } },
                    onAbout = { showAbout = true; scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        CalcScreen(vm = vm, onMenu = { scope.launch { drawerState.open() } })
    }

    if (showFiles) FilesDialog(vm) { showFiles = false }
    if (showRename) RenameDialog(vm) { showRename = false }
    if (showAbout) AboutDialog { showAbout = false }
}

@Composable
fun CalcScreen(vm: CalcViewModel, onMenu: () -> Unit) {
    // har bir qator natijasi (Ans = oldingi qator natijasi)
    var ans = BigDecimal.ZERO
    val results = vm.lines.map { line ->
        val r = Calc.eval(line.text, ans)
        if (r != null) ans = r
        r
    }
    val total = results.filterNotNull().fold(BigDecimal.ZERO) { a, b -> a.add(b) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // Yuqori panel
        Surface(color = BLUE) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menyu", tint = Color.White)
                }
                Text(
                    vm.currentNote, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                IconButton(onClick = { vm.newLine() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Yangi qator", tint = Color.White)
                }
            }
        }

        // Qatorlar
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(vm.lines) { i, _ ->
                CalcLine(vm, i, results.getOrNull(i))
            }
        }

        // Jami
        Surface(color = GREYBG, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jami: ", fontSize = 18.sp, color = Color(0xFF555555))
                Text(formatBD(total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BLUE)
            }
        }

        // Klaviatura
        Keypad(vm)
    }
}

@Composable
fun CalcLine(vm: CalcViewModel, index: Int, result: BigDecimal?) {
    val tfv = vm.lines[index]
    val isFocused = vm.focused == index
    val text = tfv.text
    val cur = tfv.selection.start.coerceIn(0, text.length)

    Row(
        Modifier.fillMaxWidth()
            .clickable { vm.focusLine(index) }
            .background(if (isFocused) FOCUS_BG else Color.White)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${index + 1}", color = LINE_NUM, fontSize = 16.sp,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End
        )
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(text.take(cur), fontSize = 19.sp, color = Color.Black, maxLines = 1)
            if (isFocused) {
                Box(Modifier.width(2.dp).height(24.dp).background(BLUE))
            }
            Text(text.substring(cur), fontSize = 19.sp, color = Color.Black, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            result?.let { formatBD(it) } ?: "", color = Color(0xFF222222),
            fontSize = 19.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 60.dp), textAlign = TextAlign.End
        )
    }
}

// ----------------- Klaviatura -----------------
data class K(val label: String, val action: String)

@Composable
fun Keypad(vm: CalcViewModel) {
    val rows = listOf(
        listOf(K("7", "7"), K("8", "8"), K("9", "9"), K("⌫", "BKSP"), K("C", "CLR")),
        listOf(K("4", "4"), K("5", "5"), K("6", "6"), K("(", "("), K(")", ")")),
        listOf(K("1", "1"), K("2", "2"), K("3", "3"), K("×", " * "), K("÷", " / ")),
        listOf(K("0", "0"), K("00", "00"), K(".", "."), K("+", " + "), K("−", " - ")),
        listOf(K("◀", "LEFT"), K("▶", "RIGHT"), K("mod", " mod "), K("Ans", "Ans"), K("↵", "ENTER"))
    )
    Surface(color = Color(0xFFF5F5F5)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(4.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { k ->
                        KeyButton(k, Modifier.weight(1f)) { onKey(vm, k.action) }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(k: K, modifier: Modifier, onClick: () -> Unit) {
    val bg = when (k.action) {
        "ENTER" -> BLUE
        "CLR", "BKSP" -> Color(0xFFEF9A9A)
        else -> Color.White
    }
    val fg = when (k.action) {
        "ENTER" -> Color.White
        "CLR", "BKSP" -> Color(0xFFB71C1C)
        else -> Color(0xFF222222)
    }
    Surface(
        modifier = modifier.padding(3.dp).height(52.dp).clickable { onClick() },
        color = bg,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(k.label, fontSize = 20.sp, color = fg, fontWeight = FontWeight.Medium)
        }
    }
}

fun onKey(vm: CalcViewModel, action: String) {
    when (action) {
        "BKSP" -> vm.backspace()
        "CLR" -> vm.clearLine()
        "ENTER" -> vm.newLine()
        "LEFT" -> vm.moveCursor(-1)
        "RIGHT" -> vm.moveCursor(1)
        else -> vm.input(action)
    }
}

// ----------------- Menyu (Drawer) -----------------
@Composable
fun DrawerContent(
    onNew: () -> Unit, onSave: () -> Unit, onFiles: () -> Unit, onRename: () -> Unit,
    onShare: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onClear: () -> Unit, onAbout: () -> Unit
) {
    Column(Modifier.padding(top = 28.dp)) {
        Text(
            "CalcNote", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BLUE,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )
        HorizontalDivider()
        DrawerItem(Icons.Filled.Add, "Yangi fayl", onNew)
        DrawerItem(Icons.Filled.Save, "Saqlash", onSave)
        DrawerItem(Icons.Filled.Folder, "Fayllar", onFiles)
        DrawerItem(Icons.Filled.Edit, "Nomini o'zgartirish", onRename)
        DrawerItem(Icons.Filled.Share, "Ulashish", onShare)
        HorizontalDivider()
        DrawerItem(Icons.AutoMirrored.Filled.Undo, "Orqaga (Undo)", onUndo)
        DrawerItem(Icons.AutoMirrored.Filled.Redo, "Qaytarish (Redo)", onRedo)
        DrawerItem(Icons.Filled.Delete, "Tozalash", onClear)
        HorizontalDivider()
        DrawerItem(Icons.Filled.Info, "Dastur haqida", onAbout)
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF555555))
        Spacer(Modifier.width(20.dp))
        Text(label, fontSize = 16.sp)
    }
}

// ----------------- Dialoglar -----------------
@Composable
fun FilesDialog(vm: CalcViewModel, onDismiss: () -> Unit) {
    val notes = remember { vm.listNotes() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fayllar") },
        text = {
            Column {
                notes.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.loadNote(name); onDismiss() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = BLUE)
                        Spacer(Modifier.width(10.dp))
                        Text(name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        IconButton(onClick = { vm.deleteNote(name); onDismiss() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = Color(0xFFE57373))
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Yopish") } }
    )
}

@Composable
fun RenameDialog(vm: CalcViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(vm.currentNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fayl nomini o'zgartirish") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Nom") }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) vm.renameCurrent(name.trim())
                onDismiss()
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CalcNote haqida") },
        text = {
            Text(
                "Qatorma-qator hisoblagich.\n\n" +
                    "Har bir qator alohida hisoblanadi, pastda umumiy yig'indi (Jami) chiqadi. " +
                    "Ma'lumotlar avtomatik saqlanadi — dasturni yopsangiz ham yo'qolmaydi.\n\n" +
                    "Amallar:  +  −  ×  ÷  mod  ( )  Ans"
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

// ----------------- Yordamchilar -----------------
fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, vm: CalcViewModel) {
    val sb = StringBuilder()
    var ans = BigDecimal.ZERO
    vm.lines.forEach { l ->
        val r = Calc.eval(l.text, ans)
        if (r != null) ans = r
        if (l.text.isNotBlank()) {
            sb.append(l.text)
            if (r != null) sb.append("  = ").append(formatBD(r))
            sb.append("\n")
        }
    }
    sb.append("\nJami: ").append(formatBD(vm.total()))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Ulashish"))
}
