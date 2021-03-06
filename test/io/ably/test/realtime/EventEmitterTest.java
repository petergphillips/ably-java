package io.ably.test.realtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import io.ably.util.EventEmitter;

import org.junit.Test;

public class EventEmitterTest {

	private static enum MyEvents {
		EVENT_0,
		EVENT_1
	}

	private static class MyEventPayload {
		public MyEvents event;
		public String message;
	}

	private static interface MyListener {
		public void onMyThingHappened(MyEventPayload theThing);
	}

	private static class CountingListener implements MyListener {
		@Override
		public void onMyThingHappened(MyEventPayload theThing) {
			Integer count = counts.get(theThing.event); if(count == null) count = 0;
			counts.put(theThing.event, count + 1);
		}
		HashMap<MyEvents, Integer> counts = new HashMap<MyEvents, Integer>();
	}

	private static class MyEmitter extends EventEmitter<MyEvents, MyListener> {
		@Override
		protected void apply(MyListener listener, final MyEvents ev, final Object... args) {
			listener.onMyThingHappened(new MyEventPayload() {{ event = ev; message = (String)args[0]; }});
		}
	}

	/**
	 * Register a listener, and verify it is called
	 * when the event is emitted
	 */
	@Test
	public void on_simple() {
		MyEmitter emitter = new MyEmitter();
		emitter.on(new MyListener() {
			@Override
			public void onMyThingHappened(MyEventPayload theThing) {
				assertEquals(theThing.event, MyEvents.EVENT_0);
				assertEquals(theThing.message, "on_simple");
			}
		});
		emitter.emit(MyEvents.EVENT_0, "on_simple");
	}

	/**
	 * Register a listener, and verify it is called
	 * when the event is emitted more than once
	 */
	@Test
	public void on_multiple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.on(listener);
		emitter.emit(MyEvents.EVENT_0, "on_multiple_0");
		emitter.emit(MyEvents.EVENT_0, "on_multiple_0");
		emitter.emit(MyEvents.EVENT_1, "on_multiple_1");
		assertEquals(listener.counts.get(MyEvents.EVENT_0), Integer.valueOf(2));
		assertEquals(listener.counts.get(MyEvents.EVENT_1), Integer.valueOf(1));
	}

	/**
	 * Register and unregister listener, and verify it
	 * is not called when the event is emitted
	 */
	@Test
	public void off_simple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.on(listener);
		emitter.off(listener);
		emitter.emit(MyEvents.EVENT_0, "on_multiple_0");
		emitter.emit(MyEvents.EVENT_1, "on_multiple_1");
		assertNull(listener.counts.get(MyEvents.EVENT_0));
		assertNull(listener.counts.get(MyEvents.EVENT_1));
	}

	/**
	 * Register a listener for a specific event, and verify it is called
	 * only when that event is emitted
	 */
	@Test
	public void on_event_simple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.on(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_0, "on_event_simple_0");
		emitter.emit(MyEvents.EVENT_0, "on_event_simple_0");
		emitter.emit(MyEvents.EVENT_1, "on_event_simple_1");
		assertEquals(listener.counts.get(MyEvents.EVENT_0), Integer.valueOf(2));
		assertNull(listener.counts.get(MyEvents.EVENT_1));
	}

	/**
	 * Register a listener for a specific event, and verify
	 * it is no longer called after it has been removed
	 */
	@Test
	public void off_event_simple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.on(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_0, "off_event_simple_0");
		emitter.emit(MyEvents.EVENT_1, "off_event_simple_1");
		emitter.off(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_0, "off_event_simple_0");
		assertEquals(listener.counts.get(MyEvents.EVENT_0), Integer.valueOf(1));
		assertNull(listener.counts.get(MyEvents.EVENT_1));
	}

	/**
	 * Register a "once" listener for a specific event, and
	 * verify it is called only once when that event is emitted
	 */
	@Test
	public void once_event_simple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.once(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_0, "once_event_simple_0");
		emitter.emit(MyEvents.EVENT_0, "once_event_simple_0");
		emitter.emit(MyEvents.EVENT_1, "once_event_simple_1");
		assertEquals(listener.counts.get(MyEvents.EVENT_0), Integer.valueOf(1));
		assertNull(listener.counts.get(MyEvents.EVENT_1));
	}

	/**
	 * Register a "once" listener for a specific event, then
	 * remove it, and verify it is not called when that event is emitted
	 */
	@Test
	public void once_off_event_simple() {
		MyEmitter emitter = new MyEmitter();
		CountingListener listener = new CountingListener();
		emitter.once(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_1, "once_event_simple_1");
		emitter.off(MyEvents.EVENT_0, listener);
		emitter.emit(MyEvents.EVENT_0, "once_event_simple_0");
		assertNull(listener.counts.get(MyEvents.EVENT_0));
		assertNull(listener.counts.get(MyEvents.EVENT_1));
	}

}
