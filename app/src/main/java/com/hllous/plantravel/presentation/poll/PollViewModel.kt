package com.hllous.plantravel.presentation.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PollCandidateUiModel(
    val candidate: PollCandidate,
    val voteCount: Int,
    val votedByCurrentMember: Boolean,
    val voteProgress: Float,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
) : ViewModel() {

    private val _reloadTrigger = MutableStateFlow(0)
    private val _candidateReloadTrigger = MutableStateFlow(0)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val poll: StateFlow<Poll?> = _reloadTrigger
        .flatMapLatest {
            val groupId = selectedGroupHolder.selectedGroupId.value
                ?: return@flatMapLatest flowOf(null)
            repository.observeActivePoll(groupId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _screenPoll = MutableStateFlow<Poll?>(null)

    // screenPoll is the poll currently displayed. If explicitly set (activity poll navigation),
    // it overrides poll; otherwise falls back to the active destination poll.
    val screenPoll: StateFlow<Poll?> = combine(_screenPoll, poll) { sp, p -> sp ?: p }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setScreenPoll(poll: Poll?) {
        _screenPoll.value = poll
    }

    val currentMember: StateFlow<GroupMember?> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(null)
            else repository.observeMembers(groupId)
                .map { members -> members.firstOrNull { it.userId == sessionProvider.userId } }
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Denominator for the vote progress bar and winner overlay "N de M votos" label.
    val memberCount: StateFlow<Int> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(0)
            else repository.observeMembers(groupId)
                .map { it.size }
                .catch { emit(0) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPolls: StateFlow<List<Poll>> = combine(
        _reloadTrigger,
        selectedGroupHolder.selectedGroupId,
    ) { _, groupId -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeAllPolls(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeActivityPolls: StateFlow<List<Poll>> = combine(
        _reloadTrigger,
        selectedGroupHolder.selectedGroupId,
    ) { _, groupId -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeActiveActivityPolls(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedActivityPolls: StateFlow<List<Poll>> = combine(
        _reloadTrigger,
        selectedGroupHolder.selectedGroupId,
    ) { _, groupId -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeAllPolls(groupId)
                .map { polls -> polls.filter { it.type == PollType.ACTIVITY && it.state == PollState.CLOSED } }
                .catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestDestinationPoll: StateFlow<Poll?> = allPolls
        .map { polls -> polls.firstOrNull { it.type == PollType.DESTINATION } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestActivityPoll: StateFlow<Poll?> = allPolls
        .map { polls -> polls.firstOrNull { it.type == PollType.ACTIVITY } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val destPollCandidates: StateFlow<List<PollCandidate>> = latestDestinationPoll
        .flatMapLatest { poll ->
            if (poll == null) flowOf(emptyList())
            else repository.observePollCandidates(poll.id)
                .map { list -> list.sortedByDescending { it.voteCount } }
                .catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityPollCandidates: StateFlow<List<PollCandidate>> = latestActivityPoll
        .flatMapLatest { poll ->
            if (poll == null) flowOf(emptyList())
            else repository.observePollCandidates(poll.id)
                .map { list -> list.sortedByDescending { it.voteCount } }
                .catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val candidates: StateFlow<UiState<List<PollCandidateUiModel>>> = combine(
        screenPoll,
        _candidateReloadTrigger,
        memberCount,
    ) { activePoll, _, count -> activePoll to count }
        .flatMapLatest { (activePoll, count) ->
            if (activePoll == null) return@flatMapLatest flowOf(UiState.Success(emptyList()))
            repository.observePollCandidates(activePoll.id)
                .map<List<PollCandidate>, UiState<List<PollCandidateUiModel>>> { list ->
                    val denominator = count.coerceAtLeast(1)
                    UiState.Success(list.map {
                        PollCandidateUiModel(
                            candidate = it,
                            voteCount = it.voteCount,
                            votedByCurrentMember = it.votedByCurrentMember,
                            voteProgress = it.voteCount.toFloat() / denominator,
                        )
                    })
                }
                .catch { emit(UiState.Error(it.message ?: "Error al cargar candidatos")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val isTied: StateFlow<Boolean> = candidates
        .map { state ->
            if (state !is UiState.Success || state.data.size < 2) return@map false
            val maxVotes = state.data.maxOf { it.voteCount }
            if (maxVotes == 0) return@map false
            state.data.count { it.voteCount == maxVotes } >= 2
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun createPoll(type: PollType, name: String, expiresAt: String? = null) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching { repository.createPoll(groupId, type, name, expiresAt) }
                    .onFailure { _errorMessage.value = "Ya hay una encuesta activa" }
                reloadPoll()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun renamePoll(pollId: String, name: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching { repository.renamePoll(pollId, name) }
                    .onFailure { _errorMessage.value = "Error al renombrar la encuesta" }
                    .onSuccess { reloadPoll() }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun addCandidate(place: PlaceResult) {
        val pollId = screenPoll.value?.id ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching {
                    repository.addPollCandidate(pollId, place.placeId, place.name, place.photoUrl, place.lat, place.lng)
                }.onSuccess { reloadCandidates() }
                    .onFailure { _errorMessage.value = "Error al agregar candidato" }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun toggleVote(candidateId: String) {
        val memberId = currentMember.value?.id ?: return
        val pollId = screenPoll.value?.id ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching { repository.toggleVote(candidateId, memberId, pollId) }
                    .onSuccess { reloadCandidates() }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun closePoll() {
        val pollId = screenPoll.value?.id ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching { repository.closePoll(pollId) }
                    .onFailure { _errorMessage.value = "Error al cerrar la encuesta" }
                reloadPoll()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deletePoll() {
        val pollId = screenPoll.value?.id ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching { repository.deletePoll(pollId) }
                    .onFailure { _errorMessage.value = "Error al eliminar la encuesta" }
                    .onSuccess { reloadPoll() }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun selectWinner(candidateId: String) {
        val activePoll = screenPoll.value ?: return
        val winner = (candidates.value as? UiState.Success)
            ?.data?.firstOrNull { it.candidate.id == candidateId }
            ?.candidate ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                runCatching {
                    repository.setPollWinner(activePoll.id, winner.placeId, winner.photoUrl)
                }
                reloadPoll()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun setWinnerAsDestination(
        placeId: String,
        name: String,
        lat: Double,
        lng: Double,
        onResult: (Boolean) -> Unit = {},
    ) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = runCatching {
                    repository.setTripDestination(groupId, placeId, name, lat, lng)
                }.onFailure { _errorMessage.value = "Error al seleccionar destino" }
                onResult(result.isSuccess)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun reloadPoll() {
        _reloadTrigger.value++
    }

    private fun reloadCandidates() {
        _candidateReloadTrigger.value++
    }
}
