@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AuditRecord
import com.example.data.GithubRepo
import com.example.data.GithubTreeEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Main Navigation Tabs
enum class NavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AUDIT("Auditoria", Icons.Default.Lock),
    HISTORY("Histórico", Icons.Default.History),
    SETTINGS("Configurações", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AuditViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.AUDIT) }
    val context = LocalContext.current

    val activeAuditDetail by viewModel.activeAuditDetail.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (activeAuditDetail == null) {
                NavigationBar(
                    tonalElevation = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeAuditDetail != null) {
                // Display Audit Details Screen if an active audit is being viewed
                AuditDetailsScreen(
                    record = activeAuditDetail!!,
                    onBack = { viewModel.showAuditDetail(null) }
                )
            } else {
                // Display content based on active tab
                Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                    when (tab) {
                        NavigationTab.AUDIT -> AuditTabContent(viewModel)
                        NavigationTab.HISTORY -> HistoryTabContent(viewModel)
                        NavigationTab.SETTINGS -> SettingsTabContent(viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// AUDIT TAB CONTENT
// ==========================================

@Composable
fun AuditTabContent(viewModel: AuditViewModel) {
    val githubToken = viewModel.githubToken
    val reposState by viewModel.reposState.collectAsStateWithLifecycle()
    val selectedRepo by viewModel.selectedRepo.collectAsStateWithLifecycle()
    val filesState by viewModel.filesState.collectAsStateWithLifecycle()
    val checkedFiles by viewModel.checkedFiles.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

    when {
        analysisState is AnalysisState.Loading -> {
            // Live Progress Screen
            AuditProgressScreen(
                message = (analysisState as AnalysisState.Loading).message,
                onCancel = { viewModel.resetAnalysisState() }
            )
        }
        analysisState is AnalysisState.Error -> {
            // Analysis Error Screen
            ErrorResultScreen(
                message = (analysisState as AnalysisState.Error).message,
                onRetry = { viewModel.resetAnalysisState() }
            )
        }
        githubToken.isEmpty() -> {
            // Token Empty Warning Screen
            TokenWarningScreen()
        }
        selectedRepo == null -> {
            // Repository Selector Screen
            RepositorySelectorScreen(
                reposState = reposState,
                onRepoSelected = { viewModel.selectRepo(it) },
                onRefresh = { viewModel.fetchRepositories() }
            )
        }
        else -> {
            // File Selector & Analysis Trigger Screen
            FileSelectorScreen(
                repo = selectedRepo!!,
                filesState = filesState,
                checkedFiles = checkedFiles,
                onFileToggled = { viewModel.toggleFileChecked(it) },
                onBack = { viewModel.selectRepo(null) },
                onSelectAll = { viewModel.selectAllFiles(it) },
                onSelectNone = { viewModel.selectNoneFiles() },
                onAutoSelect = { viewModel.autoSelectCodeFiles(it) },
                onStartAnalysis = { viewModel.startAnalysis(it) },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun TokenWarningScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Token Ausente",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Token GitHub Requerido",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Para filtrar seus repositórios e realizar auditorias de segurança de código, você precisa inserir seu Personal Access Token do GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vá para a aba Configurações e insira seu token do GitHub e a chave de API do Gemini para começar.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositorySelectorScreen(
    reposState: ReposState,
    onRepoSelected: (GithubRepo) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Selecione o Repositório",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Escolha um projeto do seu GitHub para auditar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar repositório...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_repo_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Repository State Handler
        Box(modifier = Modifier.fillMaxSize()) {
            when (reposState) {
                is ReposState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ReposState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Erro",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = reposState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tentar novamente")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentar Novamente")
                        }
                    }
                }
                is ReposState.Success -> {
                    val filteredRepos = reposState.repos.filter {
                        it.fullName.lowercase().contains(searchQuery.lowercase()) ||
                                (it.description?.lowercase()?.contains(searchQuery.lowercase()) ?: false)
                    }

                    if (filteredRepos.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum repositório encontrado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredRepos) { repo ->
                                RepoCard(repo = repo, onClick = { onRepoSelected(repo) })
                            }
                        }
                    }
                }
                is ReposState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = onRefresh) {
                            Text("Carregar Repositórios")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoCard(repo: GithubRepo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repo_item_card_${repo.name}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Owner Avatar
            AsyncImage(
                model = repo.owner.avatarUrl,
                contentDescription = "Avatar de ${repo.owner.login}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = repo.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (repo.isPrivate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Privado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repo.description ?: "Sem descrição disponível.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (repo.language != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = repo.language,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Estrelas",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = repo.stars.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Visualizar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileSelectorScreen(
    repo: GithubRepo,
    filesState: FilesState,
    checkedFiles: Set<String>,
    onFileToggled: (String) -> Unit,
    onBack: () -> Unit,
    onSelectAll: (List<GithubTreeEntry>) -> Unit,
    onSelectNone: () -> Unit,
    onAutoSelect: (List<GithubTreeEntry>) -> Unit,
    onStartAnalysis: (List<GithubTreeEntry>) -> Unit,
    viewModel: AuditViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = repo.fullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings / Engine Selector inside File Selection
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Configuração do Auditor",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Modelo Inteligente:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewModel.selectedModel == "gemini-3.5-flash",
                            onClick = { viewModel.selectedModel = "gemini-3.5-flash" },
                            label = { Text("Gemini 3.5 Flash") }
                        )
                        FilterChip(
                            selected = viewModel.selectedModel == "gemini-3.1-pro-preview",
                            onClick = { viewModel.selectedModel = "gemini-3.1-pro-preview" },
                            label = { Text("Gemini 3.1 Pro") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Files List Handler
        when (filesState) {
            is FilesState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is FilesState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = filesState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is FilesState.Success -> {
                val files = filesState.files

                // Selection Actions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAutoSelect(files) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("auto_select_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Smart", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Smart", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onSelectAll(files) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Marcar Todos", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onSelectNone,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Limpar", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "${checkedFiles.size} de ${files.size} arquivos selecionados para auditoria",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Files Checklist
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(files) { file ->
                        val isChecked = checkedFiles.contains(file.path)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFileToggled(file.path) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onFileToggled(file.path) },
                                modifier = Modifier.testTag("file_checkbox_${file.path}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (file.path.endsWith(".env") || file.path.contains("secret")) Icons.Default.Lock else Icons.Default.Description,
                                contentDescription = "Ficheiro",
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.path,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Action Button
                Button(
                    onClick = { onStartAnalysis(files) },
                    enabled = checkedFiles.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Analisar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar Auditoria de Segurança", fontWeight = FontWeight.Bold)
                }
            }
            is FilesState.Idle -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AuditProgressScreen(message: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Beautiful Pulsing Loader
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Escanando Vulnerabilidades",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Message
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = onCancel) {
            Text("Cancelar Análise")
        }
    }
}

@Composable
fun ErrorResultScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Erro de Análise",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Erro na Auditoria",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Voltar ao Painel")
        }
    }
}

// ==========================================
// HISTORY TAB CONTENT
// ==========================================

@Composable
fun HistoryTabContent(viewModel: AuditViewModel) {
    val history by viewModel.historyAudits.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Histórico de Auditorias",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Veja relatórios e blueprints de análises passadas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Nenhum histórico",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma auditoria realizada ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(history) { audit ->
                    HistoryCard(
                        audit = audit,
                        onView = { viewModel.showAuditDetail(audit) },
                        onDelete = { viewModel.deleteAudit(audit) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    audit: AuditRecord,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(audit.date) { dateFormat.format(Date(audit.date)) }

    val statusColor = when (audit.status) {
        "APROVADO COM DISTINÇÃO" -> Color(0xFF2E7D32)
        "APROVADO COM RESSALVAS" -> Color(0xFF1565C0)
        "APROVADO CONDICIONALMENTE" -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Card(
        onClick = onView,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audit.repoName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score Gauge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text(
                            text = audit.score.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = audit.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Severity Badge Counts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (audit.criticalCount > 0) {
                        SeverityTag(count = audit.criticalCount, color = Color(0xFFC62828), label = "C")
                    }
                    if (audit.highCount > 0) {
                        SeverityTag(count = audit.highCount, color = Color(0xFFEF6C00), label = "A")
                    }
                    if (audit.mediumCount > 0) {
                        SeverityTag(count = audit.mediumCount, color = Color(0xFFFBC02D), label = "M")
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityTag(count: Int, color: Color, label: String) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count$label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ==========================================
// SETTINGS TAB CONTENT
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabContent(viewModel: AuditViewModel) {
    var tokenInput by remember { mutableStateOf(viewModel.githubToken) }
    var keyInput by remember { mutableStateOf(viewModel.geminiApiKey) }
    var hideToken by remember { mutableStateOf(true) }
    var hideKey by remember { mutableStateOf(true) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure seus tokens de acesso e chaves de API",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        // GitHub Token Setup
        Text(
            text = "Configurações do GitHub",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text("GitHub Personal Access Token") },
            placeholder = { Text("ghp_...") },
            singleLine = true,
            visualTransformation = if (hideToken) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = { hideToken = !hideToken }) {
                    Icon(
                        imageVector = if (hideToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Visualizar"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("github_token_input")
        )
        Text(
            text = "Para gerar um token, vá no GitHub: Settings > Developer Settings > Personal Access Tokens > Tokens (classic). É necessário marcar o escopo 'repo'.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Gemini API Key Setup
        Text(
            text = "Configurações do Gemini AI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Gemini API Key (Opcional)") },
            placeholder = { Text("Se vazio, usará a chave padrão") },
            singleLine = true,
            visualTransformation = if (hideKey) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = { hideKey = !hideKey }) {
                    Icon(
                        imageVector = if (hideKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Visualizar"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_key_input")
        )
        Text(
            text = "Se você não inserir uma chave, o aplicativo usará a chave de desenvolvedor padrão fornecida pelo AI Studio. Opcionalmente, adicione sua chave de produção.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Model Selection Dropdown/Radio
        Text(
            text = "Modelo de Análise Padrão",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = viewModel.selectedModel == "gemini-3.5-flash",
                    onClick = { viewModel.selectedModel = "gemini-3.5-flash" }
                )
                Text("Gemini 3.5 Flash", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = viewModel.selectedModel == "gemini-3.1-pro-preview",
                    onClick = { viewModel.selectedModel = "gemini-3.1-pro-preview" }
                )
                Text("Gemini 3.1 Pro", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.githubToken = tokenInput.trim()
                viewModel.geminiApiKey = keyInput.trim()
                Toast.makeText(context, "Configurações salvas com sucesso!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = "Salvar")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar Configurações", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security Instruction Info Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sobre as Auditorias",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A análise varre o código à procura de falhas críticas de acordo com os padrões CTF e OWASP: vazamento de credenciais, criptografia fraca, falhas de autenticação manual, SQL injection, RLS desconfigurado, Race Conditions e bypass de controle de acesso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ==========================================
// AUDIT DETAILS SCREEN (REPORT & BLUEPRINT)
// ==========================================

@Composable
fun AuditDetailsScreen(
    record: AuditRecord,
    onBack: () -> Unit
) {
    var tabSelected by remember { mutableStateOf(0) } // 0 = Relatório, 1 = Blueprint
    val context = LocalContext.current

    val statusColor = when (record.status) {
        "APROVADO COM DISTINÇÃO" -> Color(0xFF2E7D32)
        "APROVADO COM RESSALVAS" -> Color(0xFF1565C0)
        "APROVADO CONDICIONALMENTE" -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom Top Bar (no experimental APIs)
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.repoName.substringAfter("/"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = {
                    shareBlueprint(context, record.repoName, record.blueprint)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartilhar")
                }
                IconButton(onClick = {
                    downloadBlueprint(context, record.repoName, record.blueprint)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Baixar")
                }
            }
        }

        // Summary Header Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.08f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "STATUS DA AUDITORIA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            text = record.status,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                    // Giant Score
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, statusColor, CircleShape)
                    ) {
                        Text(
                            text = record.score.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = statusColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Counts
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CountIndicator(count = record.criticalCount, label = "CRÍTICO", color = Color(0xFFC62828))
                    CountIndicator(count = record.highCount, label = "ALTO", color = Color(0xFFEF6C00))
                    CountIndicator(count = record.mediumCount, label = "MÉDIO", color = Color(0xFFFBC02D))
                }
            }
        }

        // Tabs Selector (Relatório Executivo vs Blueprint de Correção)
        TabRow(selectedTabIndex = tabSelected) {
            Tab(
                selected = tabSelected == 0,
                onClick = { tabSelected = 0 },
                text = { Text("Relatório Executivo") }
            )
            Tab(
                selected = tabSelected == 1,
                onClick = { tabSelected = 1 },
                text = { Text("Blueprint de Correção") }
            )
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                if (tabSelected == 0) {
                    MarkdownText(markdown = record.report)
                } else {
                    MarkdownText(markdown = record.blueprint)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CountIndicator(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================
// EXCELENT MARKDOWN PARSER FOR COMPOSE
// ==========================================

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.split("\n")
    var inCodeBlock = false
    val codeBlockLines = remember { mutableStateListOf<String>() }
    var codeBlockLanguage = ""

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // Render finished code block
                    CodeBlock(codeBlockLines.joinToString("\n"), codeBlockLanguage)
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeBlockLanguage = trimmedLine.removePrefix("```").trim()
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            when {
                line.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = line.substring(2).replace("**", ""),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.substring(3).replace("**", ""),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = line.substring(4).replace("**", ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("- [ ] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Unchecked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseBoldMarkdown(line.substring(6)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Checked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseBoldMarkdown(line.substring(6)),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseBoldMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("|") && line.contains("---") -> {
                    // Skip markdown separator lines in tables
                    continue
                }
                line.startsWith("|") -> {
                    // Clean and style table records beautifully in monospace
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                trimmedLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseBoldMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(codeBlockLines.joinToString("\n"), codeBlockLanguage)
        }
    }
}

fun parseBoldMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            } else if (text.getOrNull(i) == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC2185B),
                        background = Color(0xFFFCE4EC)
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append("`")
                    i += 1
                }
            } else {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String) {
    Surface(
        color = Color(0xFF1E1E1E), // Obsidian dark editor background
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (language.isNotEmpty()) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = code,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }
}

// ==========================================
// SHARING & DOWNLOAD HELPERS
// ==========================================

fun downloadBlueprint(context: Context, repoName: String, blueprint: String) {
    try {
        val fileName = "${repoName.replace("/", "_")}_security_blueprint.md"
        
        // Write file directly into standard Download folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)
        file.writeText(blueprint)

        Toast.makeText(context, "Blueprint baixado em: Downloads/$fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Falha ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareBlueprint(context: Context, repoName: String, blueprint: String) {
    try {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Security Blueprint - $repoName")
            putExtra(Intent.EXTRA_TEXT, blueprint)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Enviar Blueprint de Segurança")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível compartilhar.", Toast.LENGTH_SHORT).show()
    }
}
