package org.opensearch.notifications.index

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.opensearch.action.ActionFuture
import org.opensearch.action.index.IndexRequest
import org.opensearch.action.index.IndexResponse
import org.opensearch.client.Client
import org.opensearch.cluster.ClusterState
import org.opensearch.cluster.routing.RoutingTable
import org.opensearch.cluster.service.ClusterService
import org.opensearch.commons.notifications.model.*
import org.opensearch.index.shard.ShardId
import org.opensearch.notifications.model.DocMetadata
import org.opensearch.notifications.model.NotificationEventDoc
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationEventIndexTests {
    private val docMetadata = DocMetadata(Instant.now(),
        Instant.now(),
        "tenant",
        arrayListOf("User:admin"))
    private val event = NotificationEvent(
        EventSource("title",
            "reference_id",
            Feature.REPORTS,
            SeverityType.HIGH,
            arrayListOf("tag")),
        arrayListOf(EventStatus("config_id",
            "config_name",
            ConfigType.SLACK,
            arrayListOf(EmailRecipientStatus("test@email.com",
                DeliveryStatus("200", "delivered"))),
            DeliveryStatus("200", "delivered"))))
    private val eventDoc = NotificationEventDoc(docMetadata, event)

    private val initClient = mock(Client::class.java)
    private val clusterService = mock(ClusterService::class.java)

    private lateinit var realClient: Client

    @BeforeAll
    fun `init`() {
        NotificationEventIndex.initialize(initClient, clusterService)

        // this is the real client, but not able to mock
        realClient = NotificationEventIndex.getClient()

        val clusterState = mock(ClusterState::class.java)
        `when`(clusterService.state()).thenReturn(clusterState)

        val routingTable = mock(RoutingTable::class.java)
        `when`(clusterState.routingTable).thenReturn(routingTable)
        `when`(routingTable.hasIndex(anyString())).thenReturn(true)
    }

    // test failure, client is not mockable
    @Test
    fun `test create event`() {
        val indexResponse = IndexResponse(
            ShardId("index", "uuid", 10),
            "type",
            "id",
            123L,
            321L,
            1L,
            true)
        val actionFuture = mock<ActionFuture<IndexResponse>> {
            on {
                actionGet(anyLong())
            } doReturn indexResponse
        }
        // mock with initClient not working since the real client is created when initialize @{link NotificationEventIndex}
        `when`(initClient.index(any())).thenReturn(actionFuture)

        NotificationEventIndex.createNotificationEvent(eventDoc)
        verify(realClient, times(1)).index(any(IndexRequest::class.java))
    }
}