package kr.ac.duksung.dobongzip.ui.like

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.repository.LikeRepository

class LikesViewModel(private val repo: LikeRepository) : ViewModel() {

    private val _items = MutableStateFlow<List<LikeItemUi>>(emptyList())
    val items: StateFlow<List<LikeItemUi>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var currentOrder = "latest"
    private var currentSize = 30

    fun load(order: String = currentOrder, size: Int = currentSize) {
        currentOrder = order
        currentSize = size
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                repo.getMyLikes(size, order).map {
                    LikeItemUi(it.placeId, it.name, it.imageUrl)
                }
            }.onSuccess { _items.value = it }
                .onFailure { /* TODO: 에러 핸들링 */ }
            _loading.value = false
        }
    }

    fun unlike(placeId: String, onError: () -> Unit = {}) {
        val before = _items.value
        _items.value = before.filterNot { it.placeId == placeId }

        viewModelScope.launch {
            runCatching { repo.unlike(placeId) }
                .onFailure {
                    _items.value = before // 롤백
                    onError()
                }
        }
    }

    fun likeFirstAndRefresh(lat: Double, lng: Double, onFail: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                // PlacesRepository 주입이 없다면 repo를 전달받도록 생성자 확장하세요.
                // 아래 예시는 LikeRepository만 있는 현재 구조 기준으로 별도 PlacesRepository 인스턴스를 만들어 씀
                val placesRepo = kr.ac.duksung.dobongzip.data.repository.PlacesRepository()
                placesRepo.likeFirstPlaceFromServer(lat, lng)
            }.onSuccess {
                // 성공 시 최신순으로 다시 로드
                load(order = "latest")
            }.onFailure {
                onFail(it)
            }
        }
    }
}
