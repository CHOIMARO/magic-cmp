package com.company.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.company.core.data.source.remote.PixabayImagePagingSource
import com.company.core.data.source.remote.PixabayRemoteDataSource
import com.company.core.domain.model.ImageItem
import com.company.core.domain.repository.PixabayRepository
import com.company.core.data.api.PixabayApi
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class PixabayRepositoryImpl(
    private val pixabayApi: PixabayApi,
    private val pixabayRemoteDataSource: PixabayRemoteDataSource,
): PixabayRepository {
    override fun getSearchImagesStream(query: String): Flow<PagingData<ImageItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                // PagingSource는 Api를 직접 사용 (이전 단계에서 결정한 방식 유지)
                PixabayImagePagingSource(pixabayApi = pixabayApi, query = query)
            }
        ).flow
    }

    override suspend fun getImageDetail(id: String): Result<ImageItem> {
        return runCatching {
            // 상세 조회는 RemoteDataSource를 통해서 호출
            val response = pixabayRemoteDataSource.getImageById(id)

            if (response.hits.isEmpty()) {
                throw Exception("Image not found (id: $id)")
            }

            val hit = response.hits.first()

            // 매핑 (Response -> Domain Model)
            ImageItem(
                id = hit.id.toString(),
                imageUrl = hit.imageUrl,
                type = hit.type,
                user = hit.user,
                tags = hit.tags.split(",").map { it.trim() },
                isFavorited = false,
                views = hit.views,
                likes = hit.likes,
                downloads = hit.downloads
            )
        }.onFailure { e ->
            if (e is CancellationException) {
                throw e
            }
        }
    }

}