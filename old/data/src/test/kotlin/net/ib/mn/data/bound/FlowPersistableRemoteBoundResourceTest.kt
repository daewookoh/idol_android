package net.ib.mn.data.bound

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
class FlowPersistableRemoteBoundResourceTest {

    @Test
    fun `emits loading with local data then success with remote data`() = runTest {
        // given
        val localData = "local"
        val remoteData = "remote"
        var cachedData: String? = null

        val flow = flowDataResource<String, String>(
            dataAction = { remoteData },
            localSourceAction = { localData },
            saveCache = { cachedData = it }
        )

        val emissions = mutableListOf<DataResource<String>>()

        // when
        flow.toList(emissions)

        // then
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DataResource.Loading)
        assertEquals(localData, (emissions[0] as DataResource.Loading).data)

        assertTrue(emissions[1] is DataResource.Success)
        assertEquals(remoteData, (emissions[1] as DataResource.Success).data)
        assertEquals(remoteData, cachedData)
    }

    @Test
    fun `emits error when remote fetch fails`() = runTest {
        // given
        val exception = RuntimeException("Failed")
        val localData = "local"

        val flow = flowDataResource<String, String>(
            dataAction = { throw exception },
            localSourceAction = { localData },
            saveCache = { }
        )

        val emissions = mutableListOf<DataResource<String>>()

        // when
        flow.toList(emissions)

        // then
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DataResource.Loading)
        assertEquals(localData, (emissions[0] as DataResource.Loading).data)

        assertTrue(emissions[1] is DataResource.Error)
        assertEquals(exception, (emissions[1] as DataResource.Error).throwable)
    }

    @Test
    fun `emits null as local data when local source fails`() = runTest {
        // given
        val remoteData = "remote"

        val flow = flowDataResource<String, String>(
            dataAction = { remoteData },
            localSourceAction = { throw Exception("Local source error") },
            saveCache = { }
        )

        val emissions = mutableListOf<DataResource<String>>()

        // when
        flow.toList(emissions)

        // then
        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DataResource.Loading)
        assertNull((emissions[0] as DataResource.Loading).data)

        assertTrue(emissions[1] is DataResource.Success)
        assertEquals(remoteData, (emissions[1] as DataResource.Success).data)
    }
}
