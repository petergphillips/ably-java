package io.ably.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.ably.http.HttpUtils;
import io.ably.rest.AblyRest;
import io.ably.test.rest.RestSetup.TestVars;
import io.ably.test.util.StatsWriter;
import io.ably.types.AblyException;
import io.ably.types.ClientOptions;
import io.ably.types.PaginatedResult;
import io.ably.types.Param;
import io.ably.types.Stats;
import io.ably.types.StatsReader;

import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class RestAppStats {

	private static AblyRest ably;
	private static String[] intervalIds;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestVars testVars = RestSetup.getTestVars();
		ClientOptions opts = new ClientOptions(testVars.keys[0].keyStr);
		testVars.fillInOptions(opts);
		ably = new AblyRest(opts);

		populateStats();
	}

	private static void createStats(Stats[] stats) {
		try {
			ably.http.post("/stats", HttpUtils.defaultGetHeaders(false), null, StatsWriter.asJSONRequest(stats), null);
		} catch (AblyException e) {
			e.printStackTrace();
		}
	}

	private static void populateStats() {
		/* get time, preferring time from Ably */
		long currentTime = System.currentTimeMillis();
		try {
			currentTime = ably.time();
		} catch (AblyException e) {}

		/* round down to the start of the current minute */
		Date currentDate = new Date(currentTime);
		currentDate.setSeconds(0);
		currentTime = currentDate.getTime();

		/* get time bounds for test */
		intervalIds = new String[3];
		for(int i = 0; i < 3; i++) {
			long intervalTime = currentTime + (i - 3) * 60 * 1000;
			intervalIds[i] = Stats.toIntervalId(intervalTime, Stats.Granularity.MINUTE);
		}

		/* add stats for each of the minutes within the interval */
		try {
			Stats[] testStats = StatsReader.readJSON(
					'['
					+   "{ \"intervalId\": \"" + intervalIds[0] + "\","
					+     "\"inbound\": {\"realtime\":{\"messages\":{\"count\":50,\"data\":5000}}}"
					+   "},"
					+   "{ \"intervalId\": \"" + intervalIds[1] + "\","
					+     "\"inbound\": {\"realtime\":{\"messages\":{\"count\":60,\"data\":6000}}}"
					+   "},"
					+   "{ \"intervalId\": \"" + intervalIds[2] + "\","
					+     "\"inbound\": {\"realtime\":{\"messages\":{\"count\":70,\"data\":7000}}}"
					+   '}'
					+ ']'
				);

			createStats(testStats);
		} catch (AblyException e) {}
	}

	/**
	 * Check minute-level stats exist (forwards)
	 */
	@Test
	public void appstats_minute0() {
		/* get the stats for this channel */
		try {
			/* note that bounds are inclusive */
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[0])
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);

			stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[1]),
				new Param("end", intervalIds[1])
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);

			stats = ably.stats(new Param[] {
					new Param("direction", "forwards"),
					new Param("start", intervalIds[2]),
					new Param("end", intervalIds[2])
				});
				assertNotNull("Expected non-null stats", stats);
				assertEquals("Expected 1 record", stats.items().length, 1);
				assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_minute0: Unexpected exception");
			return;
		}
	}

	/**
	 * Check minute-level stats exist (backwards)
	 */
	@Test
	public void appstats_minute1() {
		/* get the stats for this channel */
		try {
			/* note that bounds are inclusive */
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "backwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[0])
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);

			stats = ably.stats(new Param[] {
				new Param("direction", "backwards"),
				new Param("start", intervalIds[1]),
				new Param("end", intervalIds[1])
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);

			stats = ably.stats(new Param[] {
					new Param("direction", "backwards"),
					new Param("start", intervalIds[2]),
					new Param("end", intervalIds[2])
				});
				assertNotNull("Expected non-null stats", stats);
				assertEquals("Expected 1 record", stats.items().length, 1);
				assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_minute1: Unexpected exception");
			return;
		}
	}

	/**
	 * Check hour-level stats exist (forwards)
	 */
	@Test
	public void appstats_hour0() {
		/* get the stats for this channel */
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("unit", "hour")
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 180 messages", (int)stats.items()[0].inbound.all.all.count, (int)180);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_hour0: Unexpected exception");
			return;
		}
	}

	/**
	 * Check day-level stats exist (forwards)
	 */
	@Test
	public void appstats_day0() {
		/* get the stats for this channel */
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("unit", "day")
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 180 messages", (int)stats.items()[0].inbound.all.all.count, (int)180);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_day0: Unexpected exception");
			return;
		}
	}

	/**
	 * Check month-level stats exist (forwards)
	 */
	@Test
	public void appstats_month0() {
		/* get the stats for this channel */
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("unit", "month")
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 record", stats.items().length, 1);
			assertEquals("Expected 180 messages", (int)stats.items()[0].inbound.all.all.count, (int)180);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_month0: Unexpected exception");
			return;
		}
	}

	/**
	 * Publish events and check limit query param (backwards)
	 */
	@Test
	public void appstats_limit0() {
		/* get the stats for this channel */
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "backwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_limit0: Unexpected exception");
			return;
		}
	}

	/**
	 * Check limit query param (forwards)
	 */
	@Test
	public void appstats_limit1() {
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_limit1: Unexpected exception");
			return;
		}
	}

	/**
	 * Check query pagination (backwards)
	 */
	@Test
	public void appstats_pagination0() {
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "backwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);
			/* verify that there is no next page */
			assertFalse("Expected null next page", stats.hasNext());
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_pagination0: Unexpected exception");
			return;
		}
	}

	/**
	 * Check query pagination (forwards)
	 */
	@Test
	public void appstats_pagination1() {
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
			/* verify that there is no next page */
			assertFalse("Expected null next page", stats.hasNext());
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_pagination1: Unexpected exception");
			return;
		}
	}

	/**
	 * Check query pagination rel="first" (backwards)
	 */
	@Test
	public void appstats_pagination2() {
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "backwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);
			/* get first page */
			stats = stats.first();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 70 messages", (int)stats.items()[0].inbound.all.all.count, (int)70);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_pagination2: Unexpected exception");
			return;
		}
	}

	/**
	 * Check query pagination rel="first" (forwards)
	 */
	@Test
	public void appstats_pagination3() {
		try {
			PaginatedResult<Stats> stats = ably.stats(new Param[] {
				new Param("direction", "forwards"),
				new Param("start", intervalIds[0]),
				new Param("end", intervalIds[2]),
				new Param("limit", String.valueOf(1))
			});
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);
			/* get next page */
			stats = stats.next();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 60 messages", (int)stats.items()[0].inbound.all.all.count, (int)60);
			/* get first page */
			stats = stats.first();
			assertNotNull("Expected non-null stats", stats);
			assertEquals("Expected 1 records", stats.items().length, 1);
			assertEquals("Expected 50 messages", (int)stats.items()[0].inbound.all.all.count, (int)50);
		} catch (AblyException e) {
			e.printStackTrace();
			fail("appstats_pagination3: Unexpected exception");
			return;
		}
	}

}
