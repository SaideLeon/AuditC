package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReposState {
    object Idle : ReposState
    object Loading : ReposState
    data class Success(val repos: List<GithubRepo>) : ReposState
    data class Error(val message: String) : ReposState
}

sealed interface FilesState {
    object Idle : FilesState
    object Loading : FilesState
    data class Success(val files: List<GithubTreeEntry>) : FilesState
    data class Error(val message: String) : FilesState
}

sealed interface AnalysisState {
    object Idle : AnalysisState
    data class Loading(val message: String) : AnalysisState
    data class Success(val record: AuditRecord) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AuditRepository(db.auditDao())
    private val prefs = PreferencesHelper(application)

    // Flow of past audits from Room
    val historyAudits: StateFlow<List<AuditRecord>> = repository.allAudits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Keys and configs
    var githubToken: String
        get() = prefs.gitHubToken ?: ""
        set(value) {
            prefs.gitHubToken = value
            if (value.isNotEmpty()) {
                fetchRepositories()
            }
        }

    var geminiApiKey: String
        get() = prefs.geminiApiKey ?: ""
        set(value) {
            prefs.geminiApiKey = value
        }

    var selectedModel: String
        get() = prefs.selectedModel
        set(value) {
            prefs.selectedModel = value
        }

    // UI States
    private val _reposState = MutableStateFlow<ReposState>(ReposState.Idle)
    val reposState: StateFlow<ReposState> = _reposState.asStateFlow()

    private val _filesState = MutableStateFlow<FilesState>(FilesState.Idle)
    val filesState: StateFlow<FilesState> = _filesState.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // Currently selected repository for auditing
    private val _selectedRepo = MutableStateFlow<GithubRepo?>(null)
    val selectedRepo: StateFlow<GithubRepo?> = _selectedRepo.asStateFlow()

    // Files checked for analysis
    private val _checkedFiles = MutableStateFlow<Set<String>>(emptySet())
    val checkedFiles: StateFlow<Set<String>> = _checkedFiles.asStateFlow()

    // Currently active audit details
    private val _activeAuditDetail = MutableStateFlow<AuditRecord?>(null)
    val activeAuditDetail: StateFlow<AuditRecord?> = _activeAuditDetail.asStateFlow()

    init {
        // Automatically fetch repositories on load if a token exists
        if (githubToken.isNotEmpty()) {
            fetchRepositories()
        }
    }

    fun selectRepo(repo: GithubRepo?) {
        _selectedRepo.value = repo
        _checkedFiles.value = emptySet()
        _filesState.value = FilesState.Idle
        if (repo != null) {
            fetchRepositoryFiles(repo)
        }
    }

    fun toggleFileChecked(path: String) {
        val current = _checkedFiles.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _checkedFiles.value = current
    }

    fun selectAllFiles(files: List<GithubTreeEntry>) {
        _checkedFiles.value = files.filter { it.type == "blob" }.map { it.path }.toSet()
    }

    fun selectNoneFiles() {
        _checkedFiles.value = emptySet()
    }

    fun autoSelectCodeFiles(files: List<GithubTreeEntry>) {
        // Smart select: identify typical source code or config files prone to security issues
        val codeExtensions = listOf(
            ".kt", ".java", ".ts", ".js", ".py", ".go", ".sql", ".env", ".json", 
            "dockerfile", "yaml", "yml", ".properties", ".config", ".xml", ".gradle"
        )
        val selected = files.filter { entry ->
            entry.type == "blob" && (
                codeExtensions.any { ext -> entry.path.lowercase().endsWith(ext) } ||
                entry.path.lowercase().contains("auth") ||
                entry.path.lowercase().contains("login") ||
                entry.path.lowercase().contains("config") ||
                entry.path.lowercase().contains("route")
            )
        }.map { it.path }.toSet()
        _checkedFiles.value = selected
    }

    fun fetchRepositories() {
        if (githubToken.isEmpty()) {
            _reposState.value = ReposState.Error("Token do GitHub não configurado nas Configurações.")
            return
        }
        viewModelScope.launch {
            _reposState.value = ReposState.Loading
            try {
                val repos = withContext(Dispatchers.IO) {
                    repository.getRepositories(githubToken)
                }
                _reposState.value = ReposState.Success(repos)
            } catch (e: Exception) {
                _reposState.value = ReposState.Error("Falha ao carregar repositórios: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun fetchRepositoryFiles(repo: GithubRepo) {
        if (githubToken.isEmpty()) {
            _filesState.value = FilesState.Error("Token do GitHub ausente.")
            return
        }
        viewModelScope.launch {
            _filesState.value = FilesState.Loading
            try {
                val owner = repo.owner.login
                val name = repo.name
                val treeResponse = withContext(Dispatchers.IO) {
                    repository.getRepositoryTree(githubToken, owner, name)
                }
                val allFiles = treeResponse.tree.filter { it.type == "blob" }
                _filesState.value = FilesState.Success(allFiles)
                autoSelectCodeFiles(allFiles) // Auto-select critical files initially
            } catch (e: Exception) {
                _filesState.value = FilesState.Error("Erro ao ler arquivos: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun startAnalysis(allFiles: List<GithubTreeEntry>) {
        val repo = _selectedRepo.value ?: return
        val selectedPaths = _checkedFiles.value
        if (selectedPaths.isEmpty()) {
            _analysisState.value = AnalysisState.Error("Por favor, selecione pelo menos um arquivo para analisar.")
            return
        }

        // Key resolution priority: Custom Key -> BuildConfig Key
        val finalApiKey = geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (finalApiKey.isEmpty() || finalApiKey == "MY_GEMINI_API_KEY") {
            _analysisState.value = AnalysisState.Error("Chave de API do Gemini não configurada. Insira nas Configurações.")
            return
        }

        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading("Preparando download dos arquivos...")
            try {
                val owner = repo.owner.login
                val name = repo.name
                val filesToFetch = allFiles.filter { selectedPaths.contains(it.path) }
                
                val downloadedFilesContents = mutableMapOf<String, String>()
                
                // Fetch files sequentially with a fluid loading indicator update
                filesToFetch.forEachIndexed { idx, fileEntry ->
                    _analysisState.value = AnalysisState.Loading(
                        "Baixando arquivo ${idx + 1}/${filesToFetch.size}:\n${fileEntry.path}"
                    )
                    try {
                        val content = withContext(Dispatchers.IO) {
                            repository.getFileContent(githubToken, owner, name, fileEntry.sha)
                        }
                        // Limit size to avoid overwhelming context limits
                        if (content.length > 50000) {
                            downloadedFilesContents[fileEntry.path] = content.take(50000) + "\n... [TRUNCATED - FILE EXCEEDS SIZE LIMIT] ..."
                        } else {
                            downloadedFilesContents[fileEntry.path] = content
                        }
                    } catch (e: Exception) {
                        downloadedFilesContents[fileEntry.path] = "Erro ao baixar este arquivo: ${e.message}"
                    }
                }

                _analysisState.value = AnalysisState.Loading("Enviando código para auditoria com o Gemini ($selectedModel)...")
                
                val prompt = SecurityEngine.buildUserPrompt(repo.fullName, downloadedFilesContents)
                val systemInstruction = SecurityEngine.SYSTEM_INSTRUCTIONS

                val response = withContext(Dispatchers.IO) {
                    repository.analyzeRepository(finalApiKey, selectedModel, prompt, systemInstruction)
                }

                val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Modelo retornou resposta vazia.")

                _analysisState.value = AnalysisState.Loading("Processando relatório de auditoria...")

                val parsedResult = GeminiParser.parseResponse(candidateText)

                val record = AuditRecord(
                    repoName = repo.fullName,
                    repoUrl = repo.htmlUrl,
                    score = parsedResult.score,
                    status = parsedResult.status,
                    criticalCount = parsedResult.criticalCount,
                    highCount = parsedResult.highCount,
                    mediumCount = parsedResult.mediumCount,
                    report = parsedResult.executiveReport,
                    blueprint = parsedResult.blueprint
                )

                val savedId = withContext(Dispatchers.IO) {
                    repository.saveAudit(record)
                }
                
                val savedRecord = record.copy(id = savedId.toInt())
                _analysisState.value = AnalysisState.Success(savedRecord)
                _activeAuditDetail.value = savedRecord

            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error("Análise falhou: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun showAuditDetail(record: AuditRecord?) {
        _activeAuditDetail.value = record
    }

    fun deleteAudit(record: AuditRecord) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteAuditById(record.id)
            }
            if (_activeAuditDetail.value?.id == record.id) {
                _activeAuditDetail.value = null
            }
        }
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisState.Idle
    }
}
