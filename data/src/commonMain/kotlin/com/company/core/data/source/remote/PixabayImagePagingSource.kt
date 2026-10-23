package com.company.core.data.source.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.company.core.domain.model.ImageItem
import com.company.core.data.api.PixabayApi
import kotlinx.io.IOException

class PixabayImagePagingSource(
    private val pixabayApi: PixabayApi,
    private val query: String
) : PagingSource<Int, ImageItem>() {

    override fun getRefreshKey(state: PagingState<Int, ImageItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ImageItem> {
        return try {
            val page = params.key ?: 1
            // PagingConfig에서 설정한 페이지 사이즈를 가져옵니다.
            val perPage = params.loadSize

            val response = pixabayApi.searchImagesPagingData(
                query = query,
                page = page,
                perPage = perPage
            )

            // 2. Data Model -> Domain Model 매핑
            // (PixabayApi의 응답인 PixabayImageHitResponse를 ImageItem으로 변환)
            val items = response.hits.map { hit ->
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
            }

            // 3. 다음 페이지 키 계산 (데이터가 비어있으면 끝)
            // 주의: Pixabay API는 데이터가 없으면 hits가 빈 리스트로 옵니다.
            val nextKey = if (items.isEmpty()) null else page + 1

            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: IOException) {
            // 네트워크 에러 처리
            LoadResult.Error(e)
        } catch (e: Exception) {
            // 그 외 일반 에러 처리
            LoadResult.Error(e)
        }
    }
}