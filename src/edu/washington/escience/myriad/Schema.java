package edu.washington.escience.myriad;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.collect.ImmutableList;

/**
 * Schema describes the schema of a tuple.
 */
public final class Schema implements Serializable {

  /** Required for Java serialization. */
  private static final long serialVersionUID = 1L;

  /**
   * Converts a JDBC ResultSetMetaData object into a Schema.
   * 
   * @param rsmd the input ResultSetMetaData.
   * @return the output Schema.
   * @throws SQLException if JDBC throws a SQLException.
   */
  public static Schema fromResultSetMetaData(final ResultSetMetaData rsmd) throws SQLException {
    /* How many columns in this result set? */
    final int columnCount = rsmd.getColumnCount();

    /* Allocate space for the type and string arrays */
    final ImmutableList.Builder<Type> columnTypes = new ImmutableList.Builder<Type>();
    final ImmutableList.Builder<String> columnNames = new ImmutableList.Builder<String>();

    /* Fill them out */
    for (int i = 0; i < columnCount; ++i) {
      /* JDBC numbers columns from 1. Yes, really. */
      // Type
      final int type = rsmd.getColumnType(i + 1);
      switch (type) {
        case java.sql.Types.BOOLEAN:
          columnTypes.add(Type.BOOLEAN_TYPE);
          break;
        case java.sql.Types.FLOAT:
          columnTypes.add(Type.FLOAT_TYPE);
          break;
        case java.sql.Types.DOUBLE:
          columnTypes.add(Type.DOUBLE_TYPE);
          break;
        case java.sql.Types.INTEGER:
          columnTypes.add(Type.INT_TYPE);
          break;
        case java.sql.Types.BIGINT:
          /* Yes, really. http://dev.mysql.com/doc/refman/5.0/en/numeric-types.html#integer-types */
          columnTypes.add(Type.LONG_TYPE);
          break;
        case java.sql.Types.VARCHAR:
        case java.sql.Types.CHAR:
          columnTypes.add(Type.STRING_TYPE);
          break;
        default:
          throw new UnsupportedOperationException("JDBC type (java.SQL.Types) of " + type + " is not supported");
      }
      // Name
      columnNames.add(rsmd.getColumnName(i + 1));
    }

    return new Schema(columnTypes.build(), columnNames.build());
  }

  /**
   * Converts a SQLiteStatement object into a Schema.
   * 
   * @param statement the input SQLiteStatement (must have been stepped).
   * @return the output Schema.
   * @throws SQLiteException if SQLite throws an exception.
   */
  public static Schema fromSQLiteStatement(final SQLiteStatement statement) throws SQLiteException {
    assert (statement.hasStepped());

    /* How many columns in this result set? */
    final int columnCount = statement.columnCount();

    /* Allocate space for the type and string arrays */
    final ImmutableList.Builder<Type> columnTypes = new ImmutableList.Builder<Type>();
    final ImmutableList.Builder<String> columnNames = new ImmutableList.Builder<String>();

    /* Fill them out */
    for (int i = 0; i < columnCount; ++i) {
      // Type
      final int type = statement.columnType(i);
      switch (type) {
        case SQLiteConstants.SQLITE_INTEGER:
          /*
           * TODO SQLite uses variable-width ints, so there's no way to tell. Default conservatively to long.
           * http://www.sqlite.org/datatype3.html
           */
          columnTypes.add(Type.LONG_TYPE);
          break;
        case SQLiteConstants.SQLITE_TEXT:
          columnTypes.add(Type.STRING_TYPE);
          break;
        case SQLiteConstants.SQLITE_FLOAT:
          /* TODO Yes really, see above. */
          columnTypes.add(Type.DOUBLE_TYPE);
          break;
        default:
          throw new UnsupportedOperationException("SQLite type (SQLiteConstants) " + type + " is not supported");
      }
      // Name
      columnNames.add(statement.getColumnName(i));
    }

    return new Schema(columnTypes.build(), columnNames.build());
  }

  /**
   * Merge two Schemas into one. The result has the columns of the first concatenated with the columns of the second.
   * 
   * @param first The Schema with the first columns of the new Schema.
   * @param second The Schema with the last columns of the Schema.
   * @return the new Schema.
   */
  public static Schema merge(final Schema first, final Schema second) {
    final ImmutableList.Builder<Type> types = ImmutableList.builder();
    final ImmutableList.Builder<String> names = ImmutableList.builder();

    types.addAll(first.getColumnTypes()).addAll(second.getColumnTypes());
    names.addAll(first.getColumnNames()).addAll(second.getColumnNames());

    return new Schema(types.build(), names.build());
  }

  /** The types of the columns in this relation. */
  private final ImmutableList<Type> columnTypes;

  /** The names of the columns in this relation. */
  private final ImmutableList<String> columnNames;

  /**
   * Helper function to build a Schema from builders.
   * 
   * @param types the types of the columns in this Schema.
   * @param names the names of the columns in this Schema.
   */
  public Schema(final ImmutableList.Builder<Type> types, final ImmutableList.Builder<String> names) {
    this(types.build(), names.build());
  }

  /**
   * Create a Schema given an array of column types. Column names will be col0, col1, ....
   * 
   * @param types the types of the columns.
   */
  public Schema(final List<Type> types) {
    Objects.requireNonNull(types);
    columnTypes = ImmutableList.copyOf(types);
    final ImmutableList.Builder<String> names = ImmutableList.builder();
    for (int i = 0; i < types.size(); i++) {
      names.add("col" + i);
    }
    columnNames = names.build();
  }

  /**
   * Create a new Schema with typeAr.length columns with columns of the specified types, with associated named columns.
   * 
   * @param typeAr array specifying the number of and types of columns in this Schema. It must contain at least one
   *          entry.
   * @param columnAr array specifying the names of the columns. Note that names may be null.
   */
  public Schema(final List<Type> typeAr, final List<String> columnAr) {
    Objects.requireNonNull(typeAr);
    Objects.requireNonNull(columnAr);
    if (typeAr.size() != columnAr.size()) {
      throw new IllegalArgumentException("Invalid Schema: must have the same number of column types and column");
    }
    columnTypes = ImmutableList.copyOf(typeAr);
    columnNames = ImmutableList.copyOf(columnAr);
  }

  /**
   * Constructor. Create a new tuple desc with typeAr.length columns with columns of the specified types, with anonymous
   * (unnamed) columns.
   * 
   * @param typeAr array specifying the number of and types of columns in this Schema. It must contain at least one
   *          entry.
   */
  @Deprecated
  public Schema(final Type[] typeAr) {
    this(Arrays.asList(typeAr));
  }

  /**
   * Construct a Schema given a type array and column name arrays.
   * 
   * This function is deprecated as it requires a copy: arrays are mutable and Schema objects are immutable.
   * 
   * @param types the types of the columns in this Schema.
   * @param names the names of the columns in this Schema.
   */
  @Deprecated
  public Schema(final Type[] types, final String[] names) {
    this(Arrays.asList(types), Arrays.asList(names));
  }

  /**
   * Compares the specified object with this Schema for equality. Two Schemas are considered equal if they are the same
   * size and if the n-th type in this Schema is equal to the n-th type in the other.
   * 
   * @param o the Object to be compared for equality with this Schema.
   * @return true if the object is equal to this Schema.
   */
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Schema)) {
      return false;
    }
    final Schema other = (Schema) o;

    return columnTypes.equals(other.columnTypes);
  }

  /**
   * Find the index of the column with a given name.
   * 
   * @param name name of the column.
   * @return the index of the column that is first to have the given name.
   * @throws NoSuchElementException if no column with a matching name is found.
   */
  public int columnNameToIndex(final String name) {
    final int ret = columnNames.indexOf(name);
    if (ret == -1) {
      throw new NoSuchElementException("No column named " + name + " found");
    }
    return ret;
  }

  /**
   * Gets the (possibly null) column name of the ith column of this Schema.
   * 
   * @param index index of the column name to return. It must be a valid index.
   * @return the name of the ith column
   * @throws IndexOutOfBoundsException if index is negative or not less than numColumns.
   */
  public String getColumnName(final int index) {
    return columnNames.get(index);
  }

  /**
   * Returns an immutable list containing the names of the columns in this Schema.
   * 
   * @return an immutable list containing the names of the columns in this Schema.
   */
  public ImmutableList<String> getColumnNames() {
    return columnNames;
  }

  /**
   * Gets the type of the ith column of this Schema.
   * 
   * @param index index of the column to get the type of. It must be a valid index.
   * @return the type of the ith column
   * @throws IndexOutOfBoundsException if index is negative or not less than numColumns.
   */
  public Type getColumnType(final int index) {
    return columnTypes.get(index);
  }

  /**
   * Returns an immutable list containing the types of the columns in this Schema.
   * 
   * @return an immutable list containing the types of the columns in this Schema.
   */
  public ImmutableList<Type> getColumnTypes() {
    return columnTypes;
  }

  @Override
  public int hashCode() {
    // If you want to use Schema as keys for HashMap, implement this so
    // that equal objects have equals hashCode() results
    throw new UnsupportedOperationException("unimplemented");
  }

  /**
   * @return the number of columns in this Schema
   */
  public int numColumns() {
    return columnTypes.size();
  }

  /**
   * Returns a String describing this descriptor. It should be of the form
   * "columnType[0](columnName[0]), ..., columnType[M](columnName[M])", although the exact format does not matter.
   * 
   * @return String describing this descriptor.
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < columnTypes.size(); ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(columnNames.get(i)).append(" (").append(columnTypes.get(i)).append(")");
    }
    return sb.toString();
  }
}
