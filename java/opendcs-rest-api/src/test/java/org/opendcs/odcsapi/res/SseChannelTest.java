/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class SseChannelTest
{
	@Test
	void testSendTextStampsChannelNameAndTaskId()
	{
		OutboundSseEvent event = mock(OutboundSseEvent.class);
		OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
		when(builder.build()).thenReturn(event);
		Sse sse = mock(Sse.class);
		when(sse.newEventBuilder()).thenReturn(builder);
		SseEventSink eventSink = mock(SseEventSink.class);

		new SseChannel(sse, eventSink, "COMP_STATUS", "task-1").sendText("Running computation");

		verify(builder).name("COMP_STATUS");
		verify(builder).id("task-1");
		verify(builder).mediaType(MediaType.TEXT_PLAIN_TYPE);
		verify(builder).data("Running computation");
		verify(eventSink).send(event);
	}

	@Test
	void testNewEventOverridesNameButKeepsTaskId()
	{
		OutboundSseEvent.Builder builder = mock(OutboundSseEvent.Builder.class, RETURNS_SELF);
		Sse sse = mock(Sse.class);
		when(sse.newEventBuilder()).thenReturn(builder);
		SseEventSink eventSink = mock(SseEventSink.class);

		SseChannel channel = new SseChannel(sse, eventSink, "COMP_STATUS", "task-1");
		assertSame(builder, channel.newEvent("ERROR"));

		verify(builder).name("ERROR");
		verify(builder).id("task-1");
	}

	@Test
	void testSendForwardsEventToSink()
	{
		OutboundSseEvent event = mock(OutboundSseEvent.class);
		SseEventSink eventSink = mock(SseEventSink.class);

		new SseChannel(mock(Sse.class), eventSink, "COMP_STATUS", "task-1").send(event);

		verify(eventSink).send(event);
	}
}
