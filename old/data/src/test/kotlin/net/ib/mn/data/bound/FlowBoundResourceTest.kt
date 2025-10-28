package net.ib.mn.data.bound

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import org.junit.Test
import java.io.IOException

class FlowBoundResourceTest {

    @Test
    fun `emit loading and success when data is fetched successfully`() = runTest {
        val expected = "hello"
        val flow: Flow<DataResource<String>> = flowDataResource {
            expected
        }

        flow.test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(DataResource.Loading::class.java)

            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(expected)

            awaitComplete()
        }
    }

    @Test
    fun `emit loading and error when exception is thrown`() = runTest {
        val flow: Flow<DataResource<String>> = flowDataResource<String, String> {
            throw IOException("network error")
        }

        flow.test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(DataResource.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).isInstanceOf(IOException::class.java)

            awaitComplete()
        }
    }
}
