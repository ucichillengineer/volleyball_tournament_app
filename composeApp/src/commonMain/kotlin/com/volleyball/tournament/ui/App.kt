package com.volleyball.tournament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volleyball.tournament.TournamentViewModel
import com.volleyball.tournament.domain.Player
import com.volleyball.tournament.domain.SkillLevel
import com.volleyball.tournament.domain.SkillRatings
import com.volleyball.tournament.domain.Tournament
import com.volleyball.tournament.openWhatsApp
import com.volleyball.tournament.shareText

private val CourtGreen = Color(0xFF0B3D2E)
private val CourtMid = Color(0xFF146B4A)
private val Sand = Color(0xFFE8D5A3)
private val Spike = Color(0xFFFF6B2C)
private val Foam = Color(0xFFF4F7F2)
private val Ink = Color(0xFF10231C)

private val AppColors = darkColorScheme(
    primary = Spike,
    onPrimary = Color.White,
    secondary = Sand,
    onSecondary = Ink,
    background = CourtGreen,
    onBackground = Foam,
    surface = Color(0xFF124836),
    onSurface = Foam,
    outline = Sand.copy(alpha = 0.4f)
)

enum class AppTab { Home, Players, Teams, Tournaments, Admin }

@Composable
fun VolleyballApp(viewModel: TournamentViewModel = remember { TournamentViewModel() }) {
    MaterialTheme(colorScheme = AppColors) {
        val state by viewModel.appState.collectAsState()
        val isAdmin by viewModel.isAdmin.collectAsState()
        val message by viewModel.message.collectAsState()
        val busy by viewModel.busy.collectAsState()
        val whatsApp by viewModel.whatsAppText.collectAsState()
        var tab by remember { mutableStateOf(AppTab.Home) }
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(message) {
            message?.let {
                snackbar.showSnackbar(it.text)
                viewModel.clearMessage()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF07261C), CourtGreen, CourtMid),
                            start = Offset(0f, 0f),
                            end = Offset(900f, 1400f)
                        )
                    )
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(2.dp, Sand.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    if (busy) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Spike,
                            trackColor = Sand.copy(alpha = 0.2f)
                        )
                    }

                    when (tab) {
                        AppTab.Home -> HomeScreen(
                            playerCount = state.players.size,
                            teamCount = state.teams.size,
                            tournamentCount = state.tournaments.size,
                            isAdmin = isAdmin,
                            onPlayers = { tab = AppTab.Players },
                            onTeams = { tab = AppTab.Teams },
                            onTournaments = { tab = AppTab.Tournaments },
                            onAdmin = { tab = AppTab.Admin }
                        )
                        AppTab.Players -> PlayersScreen(
                            players = state.players,
                            isAdmin = isAdmin,
                            onBack = { tab = AppTab.Home },
                            onAdd = viewModel::addPlayer,
                            onUpdateRatings = viewModel::updateRatings,
                            onRemove = viewModel::removePlayer
                        )
                        AppTab.Teams -> TeamsScreen(
                            statePlayers = state.players,
                            stateTeams = state.teams,
                            pending = state.pendingSwitch,
                            whatsAppText = whatsApp,
                            onBack = { tab = AppTab.Home },
                            onCreate = viewModel::createTeams,
                            onRequestSwitch = viewModel::requestTeamSwitch,
                            onConfirm = viewModel::confirmSwitch,
                            onCancel = viewModel::cancelSwitch
                        )
                        AppTab.Tournaments -> TournamentsScreen(
                            players = state.players,
                            tournaments = state.tournaments,
                            activeTournamentId = state.activeTournamentId,
                            whatsAppText = viewModel.tournamentWhatsAppText.collectAsState().value,
                            onBack = { tab = AppTab.Home },
                            onCreate = viewModel::createTournament,
                            onSelect = viewModel::selectTournament,
                            onDelete = viewModel::deleteTournament,
                            onAddMatch = viewModel::addMatch,
                            onRemoveMatch = viewModel::removeMatch
                        )
                        AppTab.Admin -> AdminScreen(
                            isAdmin = isAdmin,
                            exportJson = { viewModel.exportSnapshot() },
                            onBack = { tab = AppTab.Home },
                            onLogin = viewModel::loginAdmin,
                            onLogout = viewModel::logoutAdmin,
                            onPassword = viewModel::updateAdminPassword,
                            onRefresh = viewModel::refreshFromCloud,
                            onRestoreGitHub = viewModel::restoreFromGitHubBackup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    playerCount: Int,
    teamCount: Int,
    tournamentCount: Int,
    isAdmin: Boolean,
    onPlayers: () -> Unit,
    onTeams: () -> Unit,
    onTournaments: () -> Unit,
    onAdmin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "COURT BALANCE",
                color = Sand,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 46.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Build fair volleyball sides, name captains, and share the lineup in one tap.",
                color = Foam.copy(alpha = 0.85f),
                fontSize = 17.sp,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatPill("$playerCount players")
                StatPill("$teamCount teams")
                StatPill("$tournamentCount tournaments")
                if (isAdmin) StatPill("admin")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryAction("Open roster", onPlayers)
            PrimaryAction("Organize tournament", onTournaments)
            PrimaryAction("Balance teams", onTeams)
            OutlinedButton(
                onClick = onAdmin,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) {
                Text("Admin")
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Text(
        text = text.uppercase(),
        color = Sand,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.dp, Sand.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Spike, contentColor = Color.White)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersScreen(
    players: List<Player>,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onAdd: (String, SkillRatings, Boolean) -> Unit,
    onUpdateRatings: (String, SkillRatings) -> Unit,
    onRemove: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var setter by remember { mutableStateOf(SkillLevel.MEDIUM) }
    var lifter by remember { mutableStateOf(SkillLevel.MEDIUM) }
    var spiker by remember { mutableStateOf(SkillLevel.MEDIUM) }
    var allRounder by remember { mutableStateOf(SkillLevel.MEDIUM) }
    var editingId by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Roster", onBack = onBack) {
        Text("Add yourself or, as admin, add any member.", color = Foam.copy(alpha = 0.75f))
        Spacer(modifier = Modifier.height(16.dp))

        Field(value = name, onValueChange = { name = it }, label = "Player name")
        Spacer(modifier = Modifier.height(12.dp))
        SkillPicker("Setter", setter) { setter = it }
        SkillPicker("Lifter", lifter) { lifter = it }
        SkillPicker("Spiker", spiker) { spiker = it }
        SkillPicker("All rounder", allRounder) { allRounder = it }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    onAdd(name, SkillRatings(setter, lifter, spiker, allRounder), true)
                    name = ""
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Spike)
            ) { Text("Add myself") }

            if (isAdmin) {
                OutlinedButton(
                    onClick = {
                        onAdd(name, SkillRatings(setter, lifter, spiker, allRounder), false)
                        name = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
                ) { Text("Admin add") }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text("Members", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = Sand)
        Spacer(modifier = Modifier.height(12.dp))

        players.forEach { player ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .border(1.dp, Sand.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Spike.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            player.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(player.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text(
                            "Score ${player.ratings.totalScore()} · S ${player.ratings.setter.label} · L ${player.ratings.lifter.label} · K ${player.ratings.spiker.label} · A ${player.ratings.allRounder.label}",
                            color = Foam.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        editingId = if (editingId == player.id) null else player.id
                        if (editingId == player.id) {
                            setter = player.ratings.setter
                            lifter = player.ratings.lifter
                            spiker = player.ratings.spiker
                            allRounder = player.ratings.allRounder
                        }
                    }) { Text(if (editingId == player.id) "Close" else "Edit ratings", color = Sand) }
                    TextButton(onClick = { onRemove(player.id) }) {
                        Text("Remove", color = Color(0xFFFF8A80))
                    }
                }
                if (editingId == player.id) {
                    SkillPicker("Setter", setter) { setter = it }
                    SkillPicker("Lifter", lifter) { lifter = it }
                    SkillPicker("Spiker", spiker) { spiker = it }
                    SkillPicker("All rounder", allRounder) { allRounder = it }
                    Button(
                        onClick = {
                            onUpdateRatings(player.id, SkillRatings(setter, lifter, spiker, allRounder))
                            editingId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Spike)
                    ) { Text("Save ratings") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillPicker(label: String, selected: SkillLevel, onSelect: (SkillLevel) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Sand.copy(alpha = 0.9f), fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillLevel.entries.forEach { level ->
                FilterChip(
                    selected = selected == level,
                    onClick = { onSelect(level) },
                    label = { Text(level.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Spike,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF0F3F30),
                        labelColor = Foam
                    )
                )
            }
        }
    }
}

@Composable
private fun TeamsScreen(
    statePlayers: List<Player>,
    stateTeams: List<com.volleyball.tournament.domain.Team>,
    pending: com.volleyball.tournament.domain.PendingSwitch?,
    whatsAppText: String,
    onBack: () -> Unit,
    onCreate: (Int, List<String>) -> Unit,
    onRequestSwitch: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var teamCount by remember { mutableStateOf("2") }

    ScreenScaffold(title = "Teams", onBack = onBack) {
        Text(
            "Generate balanced sides with captains. Only one player switch can be pending at a time.",
            color = Foam.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Field(value = teamCount, onValueChange = { teamCount = it.filter(Char::isDigit).take(1) }, label = "Number of teams")
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { onCreate(teamCount.toIntOrNull()?.coerceAtLeast(2) ?: 2, emptyList()) },
            colors = ButtonDefaults.buttonColors(containerColor = Spike),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Create balanced teams") }

        if (pending != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Spike.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Spike, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                val playerName = statePlayers.firstOrNull { it.id == pending.playerId }?.name ?: "Player"
                val from = stateTeams.firstOrNull { it.id == pending.fromTeamId }?.name ?: "?"
                val to = stateTeams.firstOrNull { it.id == pending.toTeamId }?.name ?: "?"
                Text("Pending switch", color = Spike, fontWeight = FontWeight.Bold)
                Text("$playerName: $from → $to", color = Foam)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Spike)) {
                        Text("Confirm")
                    }
                    OutlinedButton(onClick = onCancel, colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)) {
                        Text("Cancel")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        stateTeams.forEach { team ->
            val roster = statePlayers.filter { it.teamId == team.id }
                .sortedWith(compareByDescending<Player> { it.isCaptain }.thenBy { it.name })
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Sand.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(team.name, fontFamily = FontFamily.Serif, fontSize = 26.sp, color = Sand)
                Text(
                    "Balance ${roster.sumOf { it.ratings.totalScore() }}",
                    color = Foam.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                roster.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                player.name + if (player.isCaptain) "  · captain" else "",
                                fontWeight = if (player.isCaptain) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (stateTeams.size > 1 && pending == null) {
                            SwitchMenu(
                                teams = stateTeams.filter { it.id != player.teamId },
                                onPick = { onRequestSwitch(player.id, it) }
                            )
                        }
                    }
                }
            }
        }

        if (stateTeams.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryAction("Share on WhatsApp") { openWhatsApp(whatsAppText) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { shareText(whatsAppText) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) { Text("Share / copy team sheet") }
        }
    }
}

@Composable
private fun SwitchMenu(
    teams: List<com.volleyball.tournament.domain.Team>,
    onPick: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("Move", color = Spike) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text("To ${team.name}") },
                    onClick = {
                        open = false
                        onPick(team.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentsScreen(
    players: List<Player>,
    tournaments: List<Tournament>,
    activeTournamentId: String?,
    whatsAppText: String,
    onBack: () -> Unit,
    onCreate: (String, String, List<String>, Int, Map<String, String>) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddMatch: (String, String, String, String) -> Unit,
    onRemoveMatch: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var teamCountText by remember { mutableStateOf("2") }
    var manualTeams by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(players.map { it.id }.toSet()) }
    var assignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val teamCount = teamCountText.toIntOrNull()?.coerceIn(2, 6) ?: 2
    val active = tournaments.firstOrNull { it.id == activeTournamentId }

    ScreenScaffold(title = "Tournament", onBack = onBack) {
        Text(
            "Pick the players for one event, form teams, then record and share each match result.",
            color = Foam.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Field(name, { name = it }, "Tournament name")
        Spacer(modifier = Modifier.height(8.dp))
        Field(date, { date = it }, "Date (YYYY-MM-DD)")
        Spacer(modifier = Modifier.height(8.dp))
        Field(
            teamCountText,
            { teamCountText = it.filter(Char::isDigit).take(1) },
            "Number of teams (2–6)"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Players in this tournament", color = Sand, fontWeight = FontWeight.SemiBold)
        Text(
            "Tap a player to switch between Playing and Not playing.",
            color = Foam.copy(alpha = 0.65f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            players.sortedBy { it.name }.forEach { player ->
                PlayerAttendanceRow(
                    player = player,
                    isPlaying = player.id in selectedIds,
                    onToggle = {
                        selectedIds = if (player.id in selectedIds) selectedIds - player.id
                        else selectedIds + player.id
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val playing = players.filter { it.id in selectedIds }
        val notPlaying = players.filterNot { it.id in selectedIds }
        AvailabilitySummary(
            playing = playing,
            notPlaying = notPlaying
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilterChip(
            selected = manualTeams,
            onClick = { manualTeams = !manualTeams },
            label = { Text("Create teams manually") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Spike,
                selectedLabelColor = Color.White,
                labelColor = Foam
            )
        )

        if (manualTeams) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Assign every selected player", color = Foam.copy(alpha = 0.8f), fontSize = 13.sp)
            selectedIds.mapNotNull { id -> players.firstOrNull { it.id == id } }
                .sortedBy { it.name }
                .forEach { player ->
                    ManualAssignmentRow(
                        player = player,
                        teamCount = teamCount,
                        selectedTeamId = assignments[player.id],
                        onAssign = { teamId -> assignments = assignments + (player.id to teamId) }
                    )
                }
        }
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryAction(
            if (manualTeams) "Create manual tournament" else "Create balanced tournament"
        ) {
            onCreate(
                name,
                date,
                selectedIds.toList(),
                teamCount,
                if (manualTeams) assignments else emptyMap()
            )
        }

        if (tournaments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text("Your tournaments", fontFamily = FontFamily.Serif, fontSize = 27.sp, color = Sand)
            Spacer(modifier = Modifier.height(8.dp))
            tournaments.reversed().forEach { tournament ->
                val selected = tournament.id == activeTournamentId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) Spike else Sand.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(tournament.id) }
                        .padding(12.dp)
                ) {
                    Text(tournament.name, fontWeight = FontWeight.Bold, color = Foam)
                    Text(
                        "${tournament.date} · ${tournament.participantIds.size} players · ${tournament.teams.size} teams",
                        color = Foam.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                    if (selected) {
                        Text("Active", color = Spike, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { onDelete(tournament.id) }) {
                        Text("Remove tournament", color = Color(0xFFFF8A80))
                    }
                }
            }
        }

        active?.let { tournament ->
            Spacer(modifier = Modifier.height(28.dp))
            Text("Teams · ${tournament.name}", fontFamily = FontFamily.Serif, fontSize = 27.sp, color = Sand)
            tournament.teams.forEach { team ->
                val roster = team.playerIds.mapNotNull { id -> players.firstOrNull { it.id == id } }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        .border(1.dp, Sand.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    Text(team.name, color = Foam, fontWeight = FontWeight.Bold)
                    Text(
                        roster.joinToString(" · ") { it.name + if (it.id == team.captainId) " (C)" else "" },
                        color = Foam.copy(alpha = 0.72f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            MatchScoreCard(tournament = tournament, onAddMatch = onAddMatch)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Results", fontFamily = FontFamily.Serif, fontSize = 25.sp, color = Sand)
            if (tournament.matches.isEmpty()) {
                Text("No scores recorded yet.", color = Foam.copy(alpha = 0.7f))
            }
            tournament.matches.forEachIndexed { index, match ->
                val first = tournament.teams.firstOrNull { it.id == match.teamOneId }?.name ?: "Team 1"
                val second = tournament.teams.firstOrNull { it.id == match.teamTwoId }?.name ?: "Team 2"
                val winner = tournament.teams.firstOrNull { it.id == match.winnerTeamId() }?.name
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        .border(1.dp, Sand.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    Text("Match ${index + 1} · $first ${match.teamOneScore} – ${match.teamTwoScore} $second", color = Foam)
                    Text(
                        "Winner: ${winner ?: "Not decided"} · Loser: ${
                            if (winner == first) second else if (winner == second) first else "—"
                        }",
                        color = Sand,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { onRemoveMatch(match.id) }) {
                        Text("Remove score", color = Color(0xFFFF8A80))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryAction("Share tournament on WhatsApp") { openWhatsApp(whatsAppText) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { shareText(whatsAppText, "${tournament.name}-results.txt") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) { Text("Share / copy tournament sheet") }
        }
    }
}

@Composable
private fun PlayerAttendanceRow(
    player: Player,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) Spike.copy(alpha = 0.22f) else Color(0xFF0F3F30))
            .border(
                1.dp,
                if (isPlaying) Spike.copy(alpha = 0.8f) else Sand.copy(alpha = 0.2f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(player.name, modifier = Modifier.weight(1f), color = Foam, fontWeight = FontWeight.SemiBold)
        Text(
            if (isPlaying) "Playing" else "Not playing",
            color = if (isPlaying) Spike else Foam.copy(alpha = 0.65f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AvailabilitySummary(
    playing: List<Player>,
    notPlaying: List<Player>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F3F30), RoundedCornerShape(12.dp))
            .border(1.dp, Sand.copy(alpha = 0.24f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Playing (${playing.size})", color = Spike, fontWeight = FontWeight.Bold)
        Text(
            playing.joinToString { it.name }.ifEmpty { "No players selected" },
            color = Foam,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Not playing (${notPlaying.size})", color = Sand, fontWeight = FontWeight.Bold)
        Text(
            notPlaying.joinToString { it.name }.ifEmpty { "Everyone is playing" },
            color = Foam.copy(alpha = 0.75f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ManualAssignmentRow(
    player: Player,
    teamCount: Int,
    selectedTeamId: String?,
    onAssign: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(player.name, modifier = Modifier.weight(1f), color = Foam)
        Box {
            OutlinedButton(onClick = { open = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)) {
                Text(selectedTeamId?.removePrefix("manual-team-")?.toIntOrNull()?.let { "Team ${it + 1}" } ?: "Assign")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                (0 until teamCount).forEach { index ->
                    DropdownMenuItem(
                        text = { Text("Team ${index + 1}") },
                        onClick = {
                            open = false
                            onAssign("manual-team-$index")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchScoreCard(
    tournament: Tournament,
    onAddMatch: (String, String, String, String) -> Unit
) {
    var firstTeamId by remember(tournament.id) { mutableStateOf(tournament.teams.firstOrNull()?.id ?: "") }
    var secondTeamId by remember(tournament.id) { mutableStateOf(tournament.teams.getOrNull(1)?.id ?: "") }
    var firstScore by remember(tournament.id) { mutableStateOf("") }
    var secondScore by remember(tournament.id) { mutableStateOf("") }
    var firstOpen by remember { mutableStateOf(false) }
    var secondOpen by remember { mutableStateOf(false) }
    val firstName = tournament.teams.firstOrNull { it.id == firstTeamId }?.name ?: "Choose team"
    val secondName = tournament.teams.firstOrNull { it.id == secondTeamId }?.name ?: "Choose team"

    Text("Record a match", fontFamily = FontFamily.Serif, fontSize = 25.sp, color = Sand)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { firstOpen = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)) {
                Text(firstName)
            }
            DropdownMenu(expanded = firstOpen, onDismissRequest = { firstOpen = false }) {
                tournament.teams.forEach { team ->
                    DropdownMenuItem(text = { Text(team.name) }, onClick = { firstTeamId = team.id; firstOpen = false })
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { secondOpen = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)) {
                Text(secondName)
            }
            DropdownMenu(expanded = secondOpen, onDismissRequest = { secondOpen = false }) {
                tournament.teams.forEach { team ->
                    DropdownMenuItem(text = { Text(team.name) }, onClick = { secondTeamId = team.id; secondOpen = false })
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) { Field(firstScore, { firstScore = it.filter(Char::isDigit) }, "$firstName score") }
        Box(modifier = Modifier.weight(1f)) { Field(secondScore, { secondScore = it.filter(Char::isDigit) }, "$secondName score") }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = {
            onAddMatch(firstTeamId, secondTeamId, firstScore, secondScore)
            firstScore = ""
            secondScore = ""
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Spike)
    ) { Text("Save result and choose winner") }
}

@Composable
private fun AdminScreen(
    isAdmin: Boolean,
    exportJson: () -> String,
    onBack: () -> Unit,
    onLogin: (String, String) -> Boolean,
    onLogout: () -> Unit,
    onPassword: (String) -> Unit,
    onRefresh: () -> Unit,
    onRestoreGitHub: () -> Unit
) {
    var user by remember { mutableStateOf("admin") }
    var pass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    ScreenScaffold(title = "Admin", onBack = onBack) {
        if (!isAdmin) {
            Text("Default login: admin / volleyball", color = Foam.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(12.dp))
            Field(user, { user = it }, "Username")
            Spacer(modifier = Modifier.height(8.dp))
            Field(pass, { pass = it }, "Password")
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryAction("Unlock admin") { onLogin(user, pass) }
        } else {
            Text(
                "Admin unlocked. Live roster syncs to cloud. GitHub file data/roster.json is the editable backup.",
                color = Sand
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Spike),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh from cloud") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRestoreGitHub,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) { Text("Restore from GitHub backup") }
            Spacer(modifier = Modifier.height(16.dp))
            Field(newPass, { newPass = it }, "New admin password")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onPassword(newPass) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) { Text("Update password") }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { shareText(exportJson(), "tournament-data.json") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sand)
            ) { Text("Export JSON backup") }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onLogout) { Text("Lock admin", color = Color(0xFFFF8A80)) }
        }
    }
}

@Composable
private fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Back", color = Sand)
        }
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            color = Sand,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Spike,
            unfocusedBorderColor = Sand.copy(alpha = 0.4f),
            focusedLabelColor = Sand,
            unfocusedLabelColor = Foam.copy(alpha = 0.7f),
            focusedTextColor = Foam,
            unfocusedTextColor = Foam,
            cursorColor = Spike
        )
    )
}
