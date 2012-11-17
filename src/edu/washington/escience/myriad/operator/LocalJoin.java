package edu.washington.escience.myriad.operator;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.table._TupleBatch;

public class LocalJoin extends Operator implements Externalizable {

  /** Required for Java serialization. */
  private static final long serialVersionUID = 1L;

  private class IndexedTuple {
    int index;
    _TupleBatch tb;

    public IndexedTuple(final _TupleBatch tb, final int index) {
      this.tb = tb;
      this.index = index;
    }

    public boolean compareField(final IndexedTuple another, final int colIndx1, final int colIndx2) {
      final Type type1 = tb.inputSchema().getFieldType(colIndx1);
      // type check in query plan?
      final int rowIndx1 = index;
      final int rowIndx2 = another.index;
      // System.out.println(rowIndx1 + " " + rowIndx2 + " " + colIndx1 + " " + colIndx2);
      switch (type1) {
        case INT_TYPE:
          return tb.getInt(colIndx1, rowIndx1) == another.tb.getInt(colIndx2, rowIndx2);
        case DOUBLE_TYPE:
          return tb.getDouble(colIndx1, rowIndx1) == another.tb.getDouble(colIndx2, rowIndx2);
        case STRING_TYPE:
          return tb.getString(colIndx1, rowIndx1).equals(another.tb.getString(colIndx2, rowIndx2));
        case FLOAT_TYPE:
          return tb.getFloat(colIndx1, rowIndx1) == another.tb.getFloat(colIndx2, rowIndx2);
        case BOOLEAN_TYPE:
          return tb.getBoolean(colIndx1, rowIndx1) == another.tb.getBoolean(colIndx2, rowIndx2);
        case LONG_TYPE:
          return tb.getLong(colIndx1, rowIndx1) == another.tb.getLong(colIndx2, rowIndx2);
      }
      return false;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof IndexedTuple)) {
        return false;
      }
      final IndexedTuple another = (IndexedTuple) o;
      if (!(tb.inputSchema().equals(another.tb.inputSchema()))) {
        return false;
      }
      for (int i = 0; i < tb.inputSchema().numFields(); ++i) {
        if (!compareField(another, i, i)) {
          return false;
        }
      }
      return true;
    }

    public boolean joinEquals(final Object o, int[] compareIndx1, int[] compareIndx2) {
      if (!(o instanceof IndexedTuple)) {
        return false;
      }
      if (compareIndx1.length != compareIndx2.length) {
        return false;
      }
      final IndexedTuple another = (IndexedTuple) o;
      for (int i = 0; i < compareIndx1.length; ++i) {
        if (!compareField(another, compareIndx1[i], compareIndx2[i])) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      return tb.hashCode(index);
    }

    public int hashCode4Keys(final int[] colIndx) {
      return tb.hashCode(index, colIndx);
    }
  }

  private Operator child1, child2;
  private Schema outputSchema;
  private int[] compareIndx1;
  private int[] compareIndx2;
  private HashMap<Integer, List<IndexedTuple>> hashTable1;
  private HashMap<Integer, List<IndexedTuple>> hashTable2;
  private TupleBatchBuffer ans;

  /**
   * For java serialization
   * */
  public LocalJoin() {
  }

  public LocalJoin(final Schema outputSchema, final Operator child1, final Operator child2, final int[] compareIndx1,
      final int[] compareIndx2) {
    this.outputSchema = outputSchema;
    this.child1 = child1;
    this.child2 = child2;
    this.compareIndx1 = compareIndx1;
    this.compareIndx2 = compareIndx2;
    hashTable1 = new HashMap<Integer, List<IndexedTuple>>();
    hashTable2 = new HashMap<Integer, List<IndexedTuple>>();
    ans = new TupleBatchBuffer(outputSchema);
  }

  protected void addToAns(IndexedTuple tuple1, IndexedTuple tuple2) {
    int num1 = tuple1.tb.inputSchema().numFields();
    int num2 = tuple2.tb.inputSchema().numFields();
    for (int i = 0; i < num1; ++i) {
      ans.put(i, tuple1.tb.outputRawData().get(i).get(tuple1.index));
      // System.out.print(tuple1.tb.outputRawData().get(i).get(tuple1.index) + "\t");
    }
    for (int i = 0; i < num2; ++i) {
      ans.put(i + num1, tuple2.tb.outputRawData().get(i).get(tuple2.index));
      // System.out.print(tuple2.tb.outputRawData().get(i).get(tuple2.index) + "\t");
    }
    // System.out.println("");
  }

  protected void processChild1TB(_TupleBatch tbFromChild1) {
    for (int i = 0; i < tbFromChild1.numOutputTuples(); ++i) { // outputTuples?
      final IndexedTuple tuple1 = new IndexedTuple(tbFromChild1, i);
      final int cntHashCode = tuple1.hashCode4Keys(compareIndx1);

      // System.out.println("child1 " + i + " " + cntHashCode);

      if (hashTable2.get(cntHashCode) != null) {
        final List<IndexedTuple> tupleList = hashTable2.get(cntHashCode);
        for (int j = 0; j < tupleList.size(); ++j) {
          System.out.println(j);
          final IndexedTuple tuple2 = tupleList.get(j);
          if (tuple1.joinEquals(tuple2, compareIndx1, compareIndx2)) {
            // System.out.println("addtoans");
            addToAns(tuple1, tuple2);
          }
        }
      }

      if (hashTable1.get(cntHashCode) == null) {
        hashTable1.put(cntHashCode, new ArrayList<IndexedTuple>());
      }
      final List<IndexedTuple> tupleList = hashTable1.get(cntHashCode);
      /*
       * boolean unique = true; for (int j = 0; j < tupleList.size(); ++j) { final IndexedTuple oldTuple =
       * tupleList.get(j); if (tuple1.equals(oldTuple)) { unique = false; break; } } System.out.println(unique); if
       * (unique) {
       */
      tupleList.add(tuple1);
      // }
    }
  }

  protected void processChild2TB(_TupleBatch tbFromChild2) {
    for (int i = 0; i < tbFromChild2.numOutputTuples(); ++i) { // outputTuples?
      final IndexedTuple tuple2 = new IndexedTuple(tbFromChild2, i);
      final int cntHashCode = tuple2.hashCode4Keys(compareIndx2);

      // System.out.println("child2 " + i + " " + cntHashCode);

      if (hashTable1.get(cntHashCode) != null) {
        final List<IndexedTuple> tupleList = hashTable1.get(cntHashCode);
        for (int j = 0; j < tupleList.size(); ++j) {
          System.out.println(j);
          final IndexedTuple tuple1 = tupleList.get(j);
          if (tuple2.joinEquals(tuple1, compareIndx2, compareIndx1)) {
            // System.out.println("addtoans");
            addToAns(tuple1, tuple2);
          }
        }
      }

      if (hashTable2.get(cntHashCode) == null) {
        hashTable2.put(cntHashCode, new ArrayList<IndexedTuple>());
      }
      final List<IndexedTuple> tupleList = hashTable2.get(cntHashCode);
      /*
       * boolean unique = true; for (int j = 0; j < tupleList.size(); ++j) { final IndexedTuple oldTuple =
       * tupleList.get(j); if (tuple2.equals(oldTuple)) { unique = false; break; } } System.out.println(unique); if
       * (unique) {
       */
      tupleList.add(tuple2);
      // }
    }
  }

  @Override
  protected _TupleBatch fetchNext() throws DbException {
    TupleBatch nexttb = ans.popFilled();
    while (nexttb == null) {
      boolean hasNewTuple = false; // might change to EOS instead of hasNext()
      _TupleBatch tb = null;
      if ((tb = child1.next()) != null) {
        hasNewTuple = true;
        processChild1TB(tb);
      }
      // child2
      if ((tb = child2.next()) != null) {
        hasNewTuple = true;
        processChild2TB(tb);
      }
      nexttb = ans.popFilled();
      if (!hasNewTuple) {
        break;
      }
    }
    if (nexttb == null) {
      if (ans.numTuples() > 0) {
        nexttb = ans.popAny();
      }
    }
    return nexttb;
  }

  @Override
  public Operator[] getChildren() {
    return new Operator[] { child1, child2 };
  }

  @Override
  public Schema getSchema() {
    return outputSchema;
  }

  @Override
  public void init() throws DbException {
  }

  @Override
  public void setChildren(final Operator[] children) {
    child1 = children[0];
    child2 = children[1];
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    child1 = (Operator) in.readObject();
    child2 = (Operator) in.readObject();
    compareIndx1 = (int[]) in.readObject();
    compareIndx2 = (int[]) in.readObject();
    outputSchema = (Schema) in.readObject();
    hashTable1 = new HashMap<Integer, List<IndexedTuple>>();
    hashTable2 = new HashMap<Integer, List<IndexedTuple>>();
    ans = new TupleBatchBuffer(outputSchema);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(child1);
    out.writeObject(child2);
    out.writeObject(compareIndx1);
    out.writeObject(compareIndx2);
    out.writeObject(outputSchema);
  }

  @Override
  protected void cleanup() throws DbException {
  }

  @Override
  public _TupleBatch fetchNextReady() throws DbException {
    return null;
  }

}