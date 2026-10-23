package com.company.domain.usecase

import com.company.domain.model.ImageItem
import com.company.domain.repository.PixabayRepository

class GetImageDetailUseCase(
    private val repository: PixabayRepository
) {
    suspend operator fun invoke(id: String): Result<ImageItem> {
        return repository.getImageDetail(id)
    }
}