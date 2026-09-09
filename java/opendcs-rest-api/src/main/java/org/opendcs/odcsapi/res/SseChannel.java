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

/**
 * One server-sent event stream, bundling the {@link Sse} factory, the {@link SseEventSink} and the
 * event name and id stamped on everything published to it.
 *
 * <p>These four values are always needed together to publish a single event, so passing the channel
 * rather than the four parts keeps endpoint helpers short and removes the event-builder boilerplate
 * that otherwise repeats at every publish site.</p>
 *
 * @param sse the JAX-RS Sse factory used to build events
 * @param eventSink the sink events are published to
 * @param eventName default name applied by {@link #sendText(String)}
 * @param taskId id stamped on every event, letting a client correlate a stream with its task
 */
public record SseChannel(Sse sse, SseEventSink eventSink, String eventName, String taskId)
{
	/** Publishes {@code data} as a plain text event named after this channel. */
	public void sendText(String data)
	{
		send(newEvent(eventName)
				.mediaType(MediaType.TEXT_PLAIN_TYPE)
				.data(data)
				.build());
	}

	/**
	 * Starts an event that needs a different name, media type or reconnect delay than
	 * {@link #sendText(String)} applies. The channel's task id is already set on the builder.
	 */
	public OutboundSseEvent.Builder newEvent(String name)
	{
		return sse.newEventBuilder()
				.name(name)
				.id(taskId);
	}

	public void send(OutboundSseEvent event)
	{
		eventSink.send(event);
	}
}
