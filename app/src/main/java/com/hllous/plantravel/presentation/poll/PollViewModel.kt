package com.hllous.plantravel.presentation.poll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
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
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val poll: StateFlow<Poll?> = _reloadTrigger
        .flatMapLatest {
            val groupId = selectedGroupHolder.selectedGroupId.value
                ?: return@flatMapLatest flowOf(null)
            repository.observeActivePoll(groupId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val candidates: StateFlow<UiState<List<PollCandidateUiModel>>> = poll
        .flatMapLatest { activePoll ->
            if (activePoll == null) return@flatMapLatest flowOf(UiState.Success(emptyList()))
            repository.observePollCandidates(activePoll.id)
                .map<List<PollCandidate>, UiState<List<PollCandidateUiModel>>> { list ->
                    val totalVotes = list.sumOf { it.voteCount }
                    UiState.Success(list.map {
                        PollCandidateUiModel(
                            candidate = it,
                            voteCount = it.voteCount,
                            votedByCurrentMember = it.votedByCurrentMember,
                            voteProgress = if (totalVotes == 0) 0f else it.voteCount.toFloat() / totalVotes,
                        )
                    })
                }
                .catch { emit(UiState.Error(it.message ?: "Error al cargar candidatos")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val currentMember: StateFlow<GroupMember?> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(null)
            else repository.observeMembers(groupId)
                .map { members -> members.firstOrNull { it.userId == sessionProvider.userId } }
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createPoll(type: PollType, expiresAt: String? = null) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            runCatching { repository.createPoll(groupId, type, expiresAt) }
                .onFailure { _errorMessage.value = "Ya hay una encuesta activa" }
                .onSuccess { reloadPoll() }
        }
    }

    fun addCandidate(place: PlaceResult) {
        val pollId = poll.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                repository.addPollCandidate(pollId, place.placeId, place.name, place.photoUrl, place.lat, place.lng)
            }
        }
    }

    fun toggleVote(candidateId: String) {
        val memberId = currentMember.value?.id ?: return
        val pollId = poll.value?.id ?: return
        viewModelScope.launch {
            runCatching { repository.toggleVote(candidateId, memberId, pollId) }
        }
    }

    fun closePoll() {
        val pollId = poll.value?.id ?: return
        viewModelScope.launch {
            runCatching { repository.closePoll(pollId) }
            reloadPoll()
        }
    }

    fun selectWinner(candidateId: String) {
        val activePoll = poll.value ?: return
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        val winner = (candidates.value as? UiState.Success)
            ?.data?.firstOrNull { it.candidate.id == candidateId }
            ?.candidate ?: return
        viewModelScope.launch {
            runCatching {
                repository.setPollWinner(activePoll.id, winner.placeId)
                if (activePoll.type == PollType.DESTINATION) {
                    repository.setTripDestination(
                        groupId = groupId,
                        placeId = winner.placeId,
                        name = winner.name,
                        lat = winner.lat,
                        lng = winner.lng,
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun reloadPoll() {
        _reloadTrigger.value++
    }
}
