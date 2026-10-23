package com.company.core.domain.usecase

import com.company.core.domain.model.ImageItem
import com.company.core.domain.repository.PixabayRepository

class GetImageDetailUseCase(
    private val repository: PixabayRepository
) {
    suspend operator fun invoke(id: String): Result<ImageItem> {
        return repository.getImageDetail(id)
    }
}