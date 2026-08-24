package echo.music.iad1tya.domain.repository

import echo.music.iad1tya.domain.data.model.update.UpdateData
import echo.music.iad1tya.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>>
    fun checkForFdroidUpdate(): Flow<Resource<UpdateData>>
}