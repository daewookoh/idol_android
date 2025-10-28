package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetChartIdolIdsUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Logger
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class NewRankingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
    private val getChartIdolIdsUseCase: GetChartIdolIdsUseCase,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _rankingList = MutableLiveData<Event<ArrayList<IdolModel>>>()
    val rankingList: LiveData<Event<ArrayList<IdolModel>>> = _rankingList

    private var currentChartCode = ""
    private var idList = arrayListOf<Int>()

    fun checkChangeGender(newChartCode: String) {
        if (!rankingList.value?.peekContent().isNullOrEmpty() && newChartCode != currentChartCode) {
            mainChartIdols(newChartCode)
        }
    }

    fun changeIdolList(chartCode: String) {
        if (currentChartCode != chartCode) {
            idList.clear()
        }
        mainChartIdols(chartCode)
    }

    fun mainChartIdols(chartCode: String) = viewModelScope.launch(Dispatchers.IO) {
        if (idList.isNotEmpty() && currentChartCode == chartCode) {
            val idols = getIdolsByIdsUseCase(idList)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            val collator = java.text.Collator.getInstance(java.util.Locale.ROOT).apply {
                strength = java.text.Collator.PRIMARY
            }
            val sorted = idols
                ?.sortedWith(
                    compareByDescending<IdolModel> { it.heart }
                        .thenComparator { a, b -> collator.compare(a.getName(context), b.getName(context)) }
                )

            sorted?.let { list ->
                list.forEachIndexed { index, idol ->
                    idol.rank = if (index > 0 && list[index - 1].heart == idol.heart) {
                        list[index - 1].rank
                    } else {
                        index
                    }
                }

                _rankingList.postValue(Event(ArrayList(list)))
            }
            return@launch
        }
        try {
            currentChartCode = chartCode
            val objects = getIdolsForChartResponse(chartCode)

            idList = objects.toCollection(ArrayList())

            val idols = getIdolsByIdsUseCase(idList)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()

            val collator = java.text.Collator.getInstance(java.util.Locale.ROOT).apply {
                strength = java.text.Collator.PRIMARY
            }
            val sorted = idols
                ?.sortedWith(
                    compareByDescending<IdolModel> { it.heart }
                        .thenComparator { a, b -> collator.compare(a.getName(context), b.getName(context)) }
                )

            sorted?.let { list ->
                list.forEachIndexed { index, idol ->
                    idol.rank = if (index > 0 && list[index - 1].heart == idol.heart) {
                        list[index - 1].rank
                    } else {
                        index
                    }
                }

                _rankingList.postValue(Event(ArrayList(list)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshData() = viewModelScope.launch(Dispatchers.IO) {
        if (idList.isEmpty()) return@launch

        val idols = getIdolsByIdsUseCase(idList)
            .mapListDataResource { it.toPresentation() }
            .awaitOrThrow()

        val collator = java.text.Collator.getInstance(java.util.Locale.ROOT).apply {
            strength = java.text.Collator.PRIMARY
        }
        val sorted = idols
            ?.sortedWith(
                compareByDescending<IdolModel> { it.heart }
                    .thenComparator { a, b -> collator.compare(a.getName(context), b.getName(context)) }
            )

        sorted?.let { list ->
            list.forEachIndexed { index, idol ->
                idol.rank = if (index > 0 && list[index - 1].heart == idol.heart) {
                    list[index - 1].rank
                } else {
                    index
                }
            }

            _rankingList.postValue(Event(ArrayList(list)))
        }
    }

    private suspend fun getIdolsForChartResponse(chartCode: String): List<Int> = coroutineScope {
        val responseList = getChartIdolIdsUseCase(chartCode)
        responseList.mapNotNull { response ->
            response.data
        }.firstOrNull() ?: emptyList()
    }

    fun updateTutorial(tutorialIndex: Int) = viewModelScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                }
            },
            errorListener = { throwable ->
                // no - op
            }
        )
    }
}