package edu.washington.escience.myria;

import edu.washington.escience.myria.column.Column;
import edu.washington.escience.myria.column.builder.ColumnBuilder;
import edu.washington.escience.myria.column.builder.ColumnFactory;
import edu.washington.escience.myria.operator.LeafOperator;
import edu.washington.escience.myria.profiling.ProfilingLogger;
import edu.washington.escience.myria.storage.TupleBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventBufferScan extends LeafOperator {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EventBufferScan.class);
  public static final Schema OUTPUT_SCHEMA = Schema.ofFields(
          "opId",      Type.INT_TYPE,
          "startTime", Type.LONG_TYPE,
          "endTime",   Type.LONG_TYPE,
          "numTuples", Type.LONG_TYPE);
  private static final int BATCH_SIZE = 1000;

  private final long queryId;
  private final int fragmentId;
  private final long subqueryId;
  private final long start;
  private final long end;
  private ArrayList<EventBuffer.Event> events = null;

  public EventBufferScan(long queryId, int fragmentId, long subqueryId, long start, long end) {
    this.queryId = queryId;
    this.fragmentId = fragmentId;
    this.subqueryId = subqueryId;
    this.start = start;
    this.end = end;
  }

  @Override
  protected TupleBatch fetchNextReady() throws Exception {

    java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();

    final List<ColumnBuilder<?>> columnBuilders = ColumnFactory.allocateColumns(OUTPUT_SCHEMA);
    ProfilingLogger.events.getAnalyticsInTimespan(
            queryId,
            subqueryId,
            fragmentId,
            start,
            end,
            (EventBuffer.Event event) -> {
              columnBuilders.get(0).appendInt(event.getOpId());
              columnBuilders.get(1).appendLong(event.getStartTime());
              columnBuilders.get(2).appendLong(event.getEndTime());
              columnBuilders.get(3).appendLong(event.getNumTuples());
              count.incrementAndGet();
            }
    );


    if (count.get() == 0) {
      return null;
    }

    List<Column<?>> columns = new ArrayList<Column<?>>(columnBuilders.size());
    for (ColumnBuilder<?> cb : columnBuilders) {
      columns.add(cb.build());
    }

    return new TupleBatch(OUTPUT_SCHEMA, columns, count.get());
  }

  @Override
  protected Schema generateSchema() {
    return OUTPUT_SCHEMA;
  }
}
