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
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Configuração do Auditor",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Modelo Ativo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val currentModel by viewModel.selectedModelFlow.collectAsStateWithLifecycle()

                OutlinedTextField(
                    value = currentModel,
                    onValueChange = { viewModel.selectedModel = it },
                    label = { Text("Nome do Modelo Gemini", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("Ex: gemini-3.1-flash-lite") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_model_name_input"),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Atalhos rápidos:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = currentModel == "gemini-3.1-flash-lite",
                            onClick = { viewModel.selectedModel = "gemini-3.1-flash-lite" },
                            label = { Text("3.1 Lite", style = MaterialTheme.typography.bodySmall) }
                        )
                        FilterChip(
                            selected = currentModel == "gemini-1.5-flash",
                            onClick = { viewModel.selectedModel = "gemini-1.5-flash" },
                            label = { Text("1.5 Flash", style = MaterialTheme.typography.bodySmall) }
                        )
                        FilterChip(
                            selected = currentModel == "gemini-2.5-flash",
                            onClick = { viewModel.selectedModel = "gemini-2.5-flash" },
                            label = { Text("2.5 Flash", style = MaterialTheme.typography.bodySmall) }
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
    val activeModelFlowState by viewModel.selectedModelFlow.collectAsStateWithLifecycle()
    var modelInput by remember(activeModelFlowState) { mutableStateOf(activeModelFlowState) }
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
            text = "Para gerar um token, vai no GitHub: Settings > Developer Settings > Personal Access Tokens > Tokens (classic). É necessário marcar o escopo 'repo'.",
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            label = { Text("Nome do Modelo Gemini") },
            placeholder = { Text("Ex: gemini-3.1-flash-lite") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("model_name_settings_input")
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Atalhos rápidos:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = modelInput == "gemini-3.1-flash-lite",
                    onClick = { modelInput = "gemini-3.1-flash-lite" }
                )
                Text("3.1 Lite", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = modelInput == "gemini-1.5-flash",
                    onClick = { modelInput = "gemini-1.5-flash" }
                )
                Text("1.5 Flash", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = modelInput == "gemini-2.5-flash",
                    onClick = { modelInput = "gemini-2.5-flash" }
                )
                Text("2.5 Flash", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.githubToken = tokenInput.trim()
                viewModel.geminiApiKey = keyInput.trim()
                viewModel.selectedModel = modelInput.trim()
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
                HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
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
// GITHUB-STYLE MARKDOWN RENDERER
// ==========================================

sealed class MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode()
    data class Paragraph(val text: String) : MarkdownNode()
    data class BulletItem(val text: String, val indent: Int = 0) : MarkdownNode()
    data class NumberedItem(val number: Int, val text: String, val indent: Int = 0) : MarkdownNode()
    data class CheckItem(val checked: Boolean, val text: String) : MarkdownNode()
    data class CodeBlockNode(val code: String, val language: String) : MarkdownNode()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownNode()
    data class BlockQuote(val text: String) : MarkdownNode()
    object HorizontalRule : MarkdownNode()
    object Blank : MarkdownNode()
}

fun parseMarkdown(markdown: String): List<MarkdownNode> {
    val nodes = mutableListOf<MarkdownNode>()
    val rawLines = markdown.split("\n")
    var i = 0
    while (i < rawLines.size) {
        val line = rawLines[i]
        val trimmed = line.trim()

        // Code block
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < rawLines.size && !rawLines[i].trim().startsWith("```")) {
                codeLines.add(rawLines[i]); i++
            }
            nodes.add(MarkdownNode.CodeBlockNode(codeLines.joinToString("\n"), lang))
            i++; continue
        }

        // HR
        if (trimmed.matches(Regex("^(---+|===+|\\*\\*\\*+)$"))) {
            nodes.add(MarkdownNode.HorizontalRule); i++; continue
        }

        // Blockquote
        if (trimmed.startsWith("> ")) {
            nodes.add(MarkdownNode.BlockQuote(trimmed.removePrefix("> ")))
            i++; continue
        }

        // Headings (# to ######)
        val headingMatch = Regex("^(#{1,6}) (.+)$").find(trimmed)
        if (headingMatch != null) {
            nodes.add(MarkdownNode.Heading(headingMatch.groupValues[1].length,
                headingMatch.groupValues[2].replace(Regex("\\*+"), "")))
            i++; continue
        }

        // Table with explicit header separator
        if (trimmed.startsWith("|") && i + 1 < rawLines.size && rawLines[i + 1].trim().contains("---")) {
            val headers = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < rawLines.size && rawLines[i].trim().startsWith("|")) {
                rows.add(rawLines[i].trim().split("|").map { it.trim() }.filter { it.isNotEmpty() })
                i++
            }
            nodes.add(MarkdownNode.TableBlock(headers, rows)); continue
        }

        // Skip lone separator lines
        if (trimmed.startsWith("|") && trimmed.contains("---")) { i++; continue }

        // Inline table rows
        if (trimmed.startsWith("|")) {
            val allRows = mutableListOf(trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() })
            i++
            while (i < rawLines.size) {
                val nt = rawLines[i].trim()
                when {
                    nt.startsWith("|") && !nt.contains("---") -> { allRows.add(nt.split("|").map { it.trim() }.filter { it.isNotEmpty() }); i++ }
                    nt.startsWith("|") && nt.contains("---") -> i++
                    else -> break
                }
            }
            if (allRows.size > 1) nodes.add(MarkdownNode.TableBlock(allRows[0], allRows.drop(1)))
            else nodes.add(MarkdownNode.Paragraph(allRows[0].joinToString(" | ")))
            continue
        }

        // Checkboxes
        if (trimmed.startsWith("- [ ] ")) { nodes.add(MarkdownNode.CheckItem(false, trimmed.removePrefix("- [ ] "))); i++; continue }
        if (trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")) {
            nodes.add(MarkdownNode.CheckItem(true, trimmed.removePrefix("- [x] ").removePrefix("- [X] "))); i++; continue
        }

        // Bullets
        val bulletMatch = Regex("^(\\s*)[\\-\\*] (.+)$").find(line)
        if (bulletMatch != null) { nodes.add(MarkdownNode.BulletItem(bulletMatch.groupValues[2], bulletMatch.groupValues[1].length / 2)); i++; continue }

        // Numbered
        val numMatch = Regex("^(\\s*)(\\d+)\\. (.+)$").find(line)
        if (numMatch != null) { nodes.add(MarkdownNode.NumberedItem(numMatch.groupValues[2].toIntOrNull() ?: 1, numMatch.groupValues[3], numMatch.groupValues[1].length / 2)); i++; continue }

        // Blank
        if (trimmed.isEmpty()) { nodes.add(MarkdownNode.Blank); i++; continue }

        // Paragraph
        nodes.add(MarkdownNode.Paragraph(line)); i++
    }
    return nodes
}

fun parseInlineMarkdown(text: String, codeInlineBg: Color, codeInlineFg: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val s = text.trim()
        while (i < s.length) {
            when {
                s.startsWith("***", i) -> { val e = s.indexOf("***", i + 3); if (e != -1) { withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(s.substring(i + 3, e)) }; i = e + 3 } else { append(s[i]); i++ } }
                s.startsWith("**", i) -> { val e = s.indexOf("**", i + 2); if (e != -1) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.substring(i + 2, e)) }; i = e + 2 } else { append(s[i]); i++ } }
                s.startsWith("*", i) && !s.startsWith("**", i) -> { val e = s.indexOf("*", i + 1); if (e != -1 && !s.startsWith("**", e)) { withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(s.substring(i + 1, e)) }; i = e + 1 } else { append(s[i]); i++ } }
                s.startsWith("`", i) -> { val e = s.indexOf("`", i + 1); if (e != -1) { withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = codeInlineFg, background = codeInlineBg)) { append(" ${s.substring(i + 1, e)} ") }; i = e + 1 } else { append(s[i]); i++ } }
                s.startsWith("~~", i) -> { val e = s.indexOf("~~", i + 2); if (e != -1) { withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(s.substring(i + 2, e)) }; i = e + 2 } else { append(s[i]); i++ } }
                else -> { append(s[i]); i++ }
            }
        }
    }
}

@Composable
fun GitHubCodeBlock(code: String, language: String, isDark: Boolean) {
    val bgColor = if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA)
    val borderColor = if (isDark) Color(0xFF30363D) else Color(0xFFD0D7DE)
    val headerBg = if (isDark) Color(0xFF21262D) else Color(0xFFEAECEF)
    val langColor = if (isDark) Color(0xFF8B949E) else Color(0xFF57606A)
    val codeColor = if (isDark) Color(0xFFE6EDF3) else Color(0xFF24292F)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (language.isNotEmpty()) {
                Surface(color = headerBg, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = langColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = language.lowercase(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = langColor, fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
            ) {
                Text(text = code, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp, color = codeColor))
            }
        }
    }
}

@Composable
fun GitHubTable(
    headers: List<String>,
    rows: List<List<String>>,
    borderColor: Color,
    headerBg: Color,
    altRowBg: Color,
    codeInlineBg: Color,
    codeInlineFg: Color
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cellColor = if (isDark) Color(0xFFE6EDF3) else Color(0xFF24292F)

    Surface(
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header row
            if (headers.isNotEmpty()) {
                Surface(color = headerBg) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        headers.forEachIndexed { idx, h ->
                            Box(modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(parseInlineMarkdown(h, codeInlineBg, codeInlineFg),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = cellColor)
                            }
                            if (idx < headers.lastIndex) {
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(borderColor))
                            }
                        }
                    }
                }
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            // Data rows
            rows.forEachIndexed { ri, row ->
                val rowBg = if (ri % 2 == 1) altRowBg else Color.Transparent
                Surface(color = rowBg) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val cols = maxOf(headers.size, row.size)
                        (0 until cols).forEach { ci ->
                            Box(modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(parseInlineMarkdown(row.getOrElse(ci) { "" }, codeInlineBg, codeInlineFg),
                                    style = MaterialTheme.typography.bodySmall, color = cellColor)
                            }
                            if (ci < cols - 1) {
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(borderColor))
                            }
                        }
                    }
                }
                if (ri < rows.lastIndex) HorizontalDivider(color = borderColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val nodes = remember(markdown) { parseMarkdown(markdown) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val codeInlineBg = if (isDark) Color(0xFF2D333B) else Color(0xFFF0F0F0)
    val codeInlineFg = if (isDark) Color(0xFFE06C75) else Color(0xFFD73A49)
    val tableBorderColor = if (isDark) Color(0xFF444C56) else Color(0xFFD0D7DE)
    val tableHeaderBg = if (isDark) Color(0xFF2D333B) else Color(0xFFF6F8FA)
    val tableAltBg = if (isDark) Color(0xFF22272E).copy(alpha = 0.6f) else Color(0xFFFAFBFC)
    val blockQuoteBg = if (isDark) Color(0xFF2D333B) else Color(0xFFF6F8FA)
    val blockQuoteBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val hrColor = if (isDark) Color(0xFF3B434B) else Color(0xFFD0D7DE)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Blank -> Spacer(modifier = Modifier.height(8.dp))

                is MarkdownNode.HorizontalRule -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = hrColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                is MarkdownNode.Heading -> {
                    val textStyle = when (node.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        4 -> MaterialTheme.typography.titleMedium
                        5 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyLarge
                    }
                    val topPad = when (node.level) { 1 -> 20.dp; 2 -> 16.dp; 3 -> 12.dp; 4 -> 10.dp; else -> 8.dp }
                    val botPad = when (node.level) { 1 -> 8.dp; 2 -> 6.dp; else -> 4.dp }
                    val headingColor = when (node.level) {
                        1, 2 -> MaterialTheme.colorScheme.primary
                        3 -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Spacer(modifier = Modifier.height(topPad))
                    Column {
                        Text(
                            text = node.text,
                            style = textStyle,
                            fontWeight = if (node.level <= 3) FontWeight.Bold else FontWeight.SemiBold,
                            color = headingColor
                        )
                        if (node.level <= 2) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = hrColor, thickness = 1.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(botPad))
                }

                is MarkdownNode.BlockQuote -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .width(4.dp)
                            .defaultMinSize(minHeight = 32.dp)
                            .background(blockQuoteBorder, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = blockQuoteBg, shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = parseInlineMarkdown(node.text, codeInlineBg, codeInlineFg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                is MarkdownNode.BulletItem -> {
                    val bullet = when (node.indent) { 0 -> "•"; 1 -> "◦"; else -> "▸" }
                    Row(modifier = Modifier.padding(start = (8 + node.indent * 16).dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.Top) {
                        Text(bullet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(parseInlineMarkdown(node.text, codeInlineBg, codeInlineFg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }

                is MarkdownNode.NumberedItem -> {
                    Row(modifier = Modifier.padding(start = (8 + node.indent * 16).dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.Top) {
                        Text("${node.number}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(parseInlineMarkdown(node.text, codeInlineBg, codeInlineFg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }

                is MarkdownNode.CheckItem -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (node.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null,
                            tint = if (node.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(parseInlineMarkdown(node.text, codeInlineBg, codeInlineFg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (node.checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (node.checked) TextDecoration.LineThrough else TextDecoration.None)
                    }
                }

                is MarkdownNode.CodeBlockNode -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    GitHubCodeBlock(code = node.code, language = node.language, isDark = isDark)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                is MarkdownNode.TableBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    GitHubTable(
                        headers = node.headers,
                        rows = node.rows,
                        borderColor = tableBorderColor,
                        headerBg = tableHeaderBg,
                        altRowBg = tableAltBg,
                        codeInlineBg = codeInlineBg,
                        codeInlineFg = codeInlineFg
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                is MarkdownNode.Paragraph -> {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseInlineMarkdown(node.text, codeInlineBg, codeInlineFg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// Legacy helpers for backward compat
fun parseBoldMarkdown(text: String): AnnotatedString =
    parseInlineMarkdown(text, Color(0xFF2D333B), Color(0xFFE06C75))

@Composable
fun CodeBlock(code: String, language: String) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    GitHubCodeBlock(code = code, language = language, isDark = isDark)
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

fun downloadBlueprint(context: Context, repoName: String, blueprint: String) {
    try {
        val fileName = "${repoName.replace("/", "_")}_security_blueprint.md"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) { downloadsDir.mkdirs() }
        val file = File(downloadsDir, fileName)
        file.writeText(blueprint)
        Toast.makeText(context, "Blueprint baixado em: Downloads/$fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Falha ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
