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
    if (events == null) {
      // TODO: locking? This structure is not threadsafe...
      events = ProfilingLogger.events.getAnalyticsInTimespan(queryId, subqueryId, fragmentId, start, end);

    }

    int count = 0;

    final List<ColumnBuilder<?>> columnBuilders = ColumnFactory.allocateColumns(OUTPUT_SCHEMA);

    Iterator<EventBuffer.Event> it = events.iterator();
    while (it.hasNext() && count < BATCH_SIZE) {
      EventBuffer.Event e = it.next();
      columnBuilders.get(0).appendInt(e.getOpId());
      columnBuilders.get(1).appendLong(e.getStartTime());
      columnBuilders.get(2).appendLong(e.getEndTime());
      columnBuilders.get(3).appendLong(e.getNumTuples());
      ++count;
    }

    if (count == 0) {
      return null;
    }

    List<Column<?>> columns = new ArrayList<Column<?>>(columnBuilders.size());
    for (ColumnBuilder<?> cb : columnBuilders) {
      columns.add(cb.build());
    }

    return new TupleBatch(OUTPUT_SCHEMA, columns, count);
  }

  @Override
  protected Schema generateSchema() {
    return OUTPUT_SCHEMA;
  }
}
