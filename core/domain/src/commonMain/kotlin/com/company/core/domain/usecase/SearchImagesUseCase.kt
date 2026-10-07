package com.company.core.domain.usecase

import androidx.paging.PagingData
import com.company.core.domain.model.ImageItem
import com.company.core.domain.repository.PixabayRepository
import kotlinx.coroutines.flow.Flow

class SearchImagesUseCase(
    private val repository: PixabayRepository
) {
    operator fun invoke(query: String): Flow<PagingData<ImageItem>> {
        return repository.getSearchImagesStream(query)
    }
}