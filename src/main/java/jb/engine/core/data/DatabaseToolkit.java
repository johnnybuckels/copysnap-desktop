package jb.engine.core.data;

import jb.engine.core.ObjectCreator;
import jb.engine.exceptions.DatabaseCommunicationException;
import jb.engine.exceptions.DatabaseInitialisationException;
import jb.engine.exceptions.DatabaseUnexpectedSituationException;
import jb.engine.exceptions.ObjectCreatorException;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract representation of a toolkit used for searching and storing objects to a database.
 * @param <T> the class type of interest while communicating with the database.
 */
public class DatabaseToolkit<T> {

    /**
     * Replaces each occurrence of uppercase letters with an underscore followed its lowercase representation ("X" -> "_x").
     * The first letter of the given string is always turned to lowercase before this replacement is applied.
     */
    public static String toSnakeCase(String string) {
        String stringToProcess = string.substring(0, 1).toLowerCase() + string.substring(1);
        StringBuilder out = new StringBuilder();
        for(char c : stringToProcess.toCharArray()) {
            if(String.valueOf(c).equals(String.valueOf(c).toUpperCase())) {
                out.append("_").append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString().toLowerCase();
    }

    private final Class<T> classType;
    private final String tableName;
    private final List<DataFieldInfo> dataFieldList;
    private final ObjectCreator<T> objectCreator;
    private final DataFieldInfo primaryKeyField;

    public static <X> DatabaseToolkit<X> forType(Class<X> classType, ObjectCreator<X> objectCreator) {
        return new DatabaseToolkit<>(classType, objectCreator);
    }

    private DatabaseToolkit(Class<T> classType, ObjectCreator<T> objectCreator) {
        this.classType = classType;
        this.tableName = toSnakeCase(classType.getSimpleName());
        this.dataFieldList = DataFieldInfo.forType(classType);
        this.objectCreator = objectCreator;
        if(dataFieldList.isEmpty()) {
            throw new IllegalArgumentException("Class type " + classType + " does not posses any data fields");
        }
        List<DataFieldInfo> pkDfi = dataFieldList.stream().filter(dfi -> Arrays.asList(dfi.getSqliteConstraints()).contains(SQLiteConstraint.PRIMARY_KEY)).collect(Collectors.toList());
        if(pkDfi.size() != 1) {
            throw new IllegalArgumentException("Class type " + classType + " does not declare exactly one primary key field");
        }
        primaryKeyField = pkDfi.get(0);
    }

    // ------------------------------

    /**
     * @return the class type associated to this toolbox.
     */
    public Class<T> getClassType() {
        return classType;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * @return number of columns registered for this table.
     */
    public int getColumnCount() {
        return dataFieldList.size();
    }

    /**
     * @return list of column names associated with this toolkits object.
     */
    public List<String> getColumnNames() {
        return dataFieldList.stream().map(DataFieldInfo::getColumnName).collect(Collectors.toList());
    }

    /**
     * @return String of column names associated with this toolkits object separated by ", "
     */
    public String getColumnNamesCommaSeparated() {
        return dataFieldList.stream().map(DataFieldInfo::getColumnName).collect(Collectors.joining(", "));
    }

    /**
     * @return all DataFieldObjects that posses constructorIndex value of 0 or greater.
     */
    public List<DataFieldInfo> getConstructorRelevantDataFields() {
        return dataFieldList.stream().filter(dfi -> dfi.constructorIndex >= 0).collect(Collectors.toList());
    }

    /**
     * @return A list of all field denoted with {@link SQLiteConstraint#PRIMARY_KEY}
     */
    public DataFieldInfo getPrimaryKeyField()  {
        return primaryKeyField;
    }

    /**
     * @return "CREATE TABLE IF NOT EXISTS t (name1 type1 const1, name2 type2 const2, ..., nameN typeN constN)"
     */
    public final String generateCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
                dataFieldList.stream().map(dfi -> dfi.columnName + " " +
                    dfi.sqliteType.getType() + " "
                    + Arrays.stream(dfi.getSqliteConstraints())
                            .map(SQLiteConstraint::getConstraint).collect(Collectors.joining(" ")))
                    .collect(Collectors.joining(", ")) +
                ")";
    }

    /**
     * @return INSERT INTO t (c1, c2, ..., cN) VALUES (?, ?, ..., ?)
     */
    public final String generateInsertTemplate() {
        return "INSERT INTO " + getTableName() + " (" +
                getColumnNamesCommaSeparated() +
                ") VALUES (" + "?" + ", ?".repeat(Integer.max(0, getColumnCount() - 1))+ ")";
    }

    /**
     * @return UPDATE t SET c1 = ?, c2 = ?, ..., cN = ? WHERE cId = ?
     */
    public final String generateUpdateTemplate() {
        return "UPDATE " + getTableName() + " SET " +
                getColumnNames().stream().map(name -> name + " = ?").collect(Collectors.joining(", ")) + " WHERE " +
                primaryKeyField.columnName + " = ?";
    }

    /**
     * @return SELECT * FROM t WHERE id = ?
     */
    public final String generateSelectByIdTemplate() {
        return "SELECT " +
                getColumnNamesCommaSeparated() +
                " FROM " + getTableName() +
                " WHERE " + primaryKeyField.columnName + " = ?";
    }

    /**
     * @return SELECT * FROM t WHERE col = ?
     */
    public final String generateSelectByColumnTemplate(String columnName) {
        return "SELECT " +
                getColumnNamesCommaSeparated() +
                " FROM " + getTableName() +
                " WHERE " + columnName + " = ?";
    }

    /**
     * @return SELECT count(c1) FROM t WHERE col = ?
     */
    public final String generateCountByColumnTemplate(String columnName) {
        return "SELECT" +
                " count(" + getPrimaryKeyField().columnName + ")" +
                " FROM " + getTableName() +
                " WHERE " + columnName + " = ?";
    }

    /**
     * @return DELETE FROM t WHERE id = ?
     */
    public String generateDeleteByIdTemplate() {
        return "DELETE FROM " + getTableName() +
                " WHERE " + primaryKeyField.columnName + " = ?";

    }

    public String generateFindAllQuery() {
        return "SELECT " + String.join(", ", getColumnNames()) +
                " FROM " + getTableName();
    }

    public String generateFindAllIdQuery() {
        return "SELECT " + primaryKeyField.columnName +
                " FROM " + tableName;
    }

    public String generateClearQuery() {
        return "DELETE FROM " + tableName;
    }

    // ----------------- Explicit query methods

    /**
     * Inserts and commits the given object to the table associated with this toolkits class via the given connection.
     */
    public final void insert(Connection connection, T objectToInsert) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateInsertTemplate())) {
            int pointer = 0;  // will start with 1
            for(DataFieldInfo dfi : dataFieldList) {
                pointer++;
                statement.setObject(pointer, dfi.getFieldValueForDatabase(objectToInsert), dfi.getSqliteType().getJavaSqlType());
            }
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not insert object " + objectToInsert.getClass() + ": " + e, e);
        }
    }

    /**
     * Updates and commits the given object to the table associated with this toolkits class via the given connection.
     * It is assumed that the given object exists in the database.
     */
    public final void update(Connection connection, T objectToUpdate) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateUpdateTemplate())) {
            int pointer = 0;  // will start with 1
            // set set-values
            for(DataFieldInfo dfi : dataFieldList) {
                pointer++;
                if(!dfi.equals(primaryKeyField)) {
                    // SET values only for non-primary key fields.
                    statement.setObject(pointer, dfi.getFieldValueForDatabase(objectToUpdate), dfi.sqliteType.getJavaSqlType());
                }
            }
            // set where-values at the last indexes
            statement.setObject(pointer,
                    primaryKeyField.getFieldValueForDatabase(objectToUpdate),
                    primaryKeyField.sqliteType.getJavaSqlType()
            );
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not update object " + objectToUpdate.getClass() + ": " + e, e);
        }
    }

    public void deleteById(Connection connection, Object id) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateDeleteByIdTemplate())) {
            if(!primaryKeyField.field.getType().isInstance(id)) {
                throw new IllegalArgumentException("id value is of type " + id.getClass() + " but needs to be of type " + primaryKeyField.field.getType());
            }
            statement.setObject(1, id, primaryKeyField.sqliteType.getJavaSqlType());
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not delete from  " + tableName + " where column " + primaryKeyField.columnName + " equals " + id + ": " + e, e);
        }
    }

    /**
     * Deletes the given object from the table. It is assumed, that this object exists in the database.
     */
    public void delete(Connection connection, T objectToDelete) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateDeleteByIdTemplate())) {
            statement.setObject(1, primaryKeyField.getFieldValueForDatabase(objectToDelete), primaryKeyField.sqliteType.getJavaSqlType());
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not delete object " + objectToDelete.getClass() + ": " + e, e);
        }
    }

    /**
     * Determines if the given object exists in the database or not using the given connection.
     */
    public final boolean exists(Connection connection, T objectToCheckExistenceOf) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateSelectByIdTemplate())) {
            statement.setObject(1,
                    primaryKeyField.getFieldValueForDatabase(objectToCheckExistenceOf),
                    primaryKeyField.getSqliteType().getJavaSqlType()
            );
            return statement.executeQuery().next();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Could not determine existence of object " + objectToCheckExistenceOf.getClass() + ": " + e, e);
        }
    }

    /**
     * @return the object representation of the row that holds the given id value.
     */
    public Optional<T> findById(Connection connection, Object valueToSearchFor) throws DatabaseCommunicationException {
       List<T> result = findByColumn(connection, primaryKeyField.columnName, valueToSearchFor);
       if(result.size() > 1) {
           throw new DatabaseUnexpectedSituationException("Searching for id " + valueToSearchFor + " yielded more than one result");
       }
       Optional<T> out;
       if (result.isEmpty()) {
           out = Optional.empty();
       } else {
           out = Optional.of(result.get(0));
       }
       return out;
    }

    /**
     * @return the number of rows where the column of given name holds the given value.
     */
    public int countByColumn(Connection connection, String columnName, Object valueToCount) throws DatabaseCommunicationException {
        try (PreparedStatement statement = connection.prepareStatement(generateCountByColumnTemplate(columnName))){
            statement.setObject(1, valueToCount);
            return statement.executeQuery().getInt(1);
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while counting values in column " + columnName + ": " + e, e);
        }
    }

    /**
     * Returns a {@link LinkedList} of id values from the database where the given column matches the given value.
     */
    public List<T> findByColumn(Connection connection, String columnName, Object valueToSearchFor) throws DatabaseCommunicationException {
        if(!getColumnNames().contains(columnName)) {
            throw new IllegalArgumentException("The requested column name " + columnName + " is not registered for this toolkit");
        }
        List<T> out = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(generateSelectByColumnTemplate(columnName))){
            statement.setObject(1, valueToSearchFor);
            ResultSet resultSet = statement.executeQuery();
            for(Map<String, Object> row : getRowsAsMap(resultSet)) {
                out.add(createObjectFromRow(row));
            }
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while searching for ids by column " + columnName + ": " + e, e);
        }
        return out;
    }

    /**
     * @return a {@link LinkedList} of values from the primary key column. The actual type of these id values is equal to the
     * field type of this toolbox primary field type.
     */
    public List<Object> findAllIds(Connection connection) throws DatabaseCommunicationException {
        List<Object> out = new LinkedList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(generateFindAllIdQuery());
            while(resultSet.next()) {
                out.add(primaryKeyField.getTransformer().fromDb(resultSet.getObject(1, primaryKeyField.getField().getType())));
            }
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while executing " + generateFindAllQuery() + ": " + e, e);
        }
        return out;
    }

    /**
     * @return a {@link LinkedList} of values from the primary key column where the requested column contains the specified value.
     * The actual type of the returned id values is equal to the field type of this toolbox primary field type.
     */
    public List<Object> findAllIdsByValue(Connection connection, String columnName, Object valueToSearchFor) throws DatabaseCommunicationException {
        if(!getColumnNames().contains(columnName)) {
            throw new IllegalArgumentException("The requested column name " + columnName + " is not registered for this toolkit");
        }
        List<Object> out = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(generateSelectByColumnTemplate(columnName))){
            statement.setObject(1, valueToSearchFor);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                out.add(primaryKeyField.getTransformer().fromDb(primaryKeyField.getField().getType().cast(resultSet.getObject(primaryKeyField.columnName))));
            }
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while searching for ids by column " + columnName + ": " + e, e);
        }
        return out;
    }

    /**
     * @return all objects belonging to this toolkit via the given connection as a {@link LinkedList}
     */
    public List<T> findAll(Connection connection) throws DatabaseCommunicationException {
        List<T> out = new LinkedList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(generateFindAllQuery());
            for(Map<String, Object> row : getRowsAsMap(resultSet)) {
                out.add(createObjectFromRow(row));
            }
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while executing " + generateFindAllQuery() + ": " + e, e);
        }
        return out;
    }

    public void deleteAllEntries(Connection connection) throws DatabaseCommunicationException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(generateClearQuery());
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseCommunicationException("Error while executing " + generateClearQuery() + ": " + e, e);
        }
    }

    /**
     * Sends a create if exists command to the database.
     */
    public final void createTableIfNotExists(Connection connection) throws DatabaseInitialisationException {
        try (Statement initStatement = connection.createStatement()) {
            initStatement.execute(generateCreateTableQuery());
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseInitialisationException("Could not initialize table: " + e, e);
        }
    }

    /**
     * Transforms each entry of the Result set in a Map of column names to column values and stores them in al list.
     */
    private List<Map<String, Object>> getRowsAsMap(ResultSet resultSet) throws SQLException {
        if(resultSet == null) {
            throw new IllegalArgumentException("Given result set was null");
        }
        List<Map<String, Object>> out = new LinkedList<>();
        while(resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for(String colName : getColumnNames()) {
                row.put(colName, resultSet.getObject(colName));
            }
            out.add(row);
        }
        return out;
    }

    /**
     * Tries to construct an object from a result set representing exactly one row from the table associated
     * with this toolkit.
     */
    private T createObjectFromRow(Map<String, Object> resultRow) {
        T out;
        List<Object> objectCreatorArgs = new LinkedList<>();
        getConstructorRelevantDataFields()
                .stream()
                .sorted(Comparator.comparing(DataFieldInfo::getConstructorIndex))
                // extract sorted values from row result, backwards transform them and put them in the argument list
                .forEach(dfi -> objectCreatorArgs.add(dfi.getTransformer().fromDb(resultRow.get(dfi.columnName))));
        try{
            out = objectCreator.createFromArgs(objectCreatorArgs.toArray());
        } catch(ObjectCreatorException e) {
            throw new DatabaseUnexpectedSituationException(String.format("Could not create object with arguments %s: %s",
                    objectCreatorArgs.stream().map(Object::getClass).collect(Collectors.toList()), e),
                    e);
        }
        return out;
    }

    // ----------------- Helper Class

    /**
     * Simple info container about a data field, e.g. e field annotated with {@link DataField}.
     */
    private static class DataFieldInfo {
        private final Field field;
        private final String columnName;
        private final SQLiteType sqliteType;
        private final SQLiteConstraint[] sqliteConstraints;
        private final int constructorIndex;
        private final Transformer transformer;

        /**
         * Returns a list of DataFieldInfo of all fields of the given class type annotated with {@link DataField}.
         */
        private static List<DataFieldInfo> forType(Class<?> classType) {
            List<DataFieldInfo> out = new ArrayList<>();
            for(Field f : classType.getDeclaredFields()) {
                DataField declaredAnnotation = f.getAnnotation(DataField.class);
                if(declaredAnnotation != null) {
                    String colName = declaredAnnotation.columnName();
                    if(colName == null || colName.isEmpty()) {
                        colName = toSnakeCase(f.getName());
                    }
                    Transformer transformer = Transformer.getTransformerFor(Transformer.JavaClassSqlTypePair.of(
                            f.getType(),
                            declaredAnnotation.sqliteType()
                    ));
                    out.add(new DataFieldInfo(f,
                            colName,
                            declaredAnnotation.sqliteType(),
                            declaredAnnotation.sqliteConstraints(),
                            declaredAnnotation.constructorArgumentPositionIndex(),
                            transformer
                            )
                    );
                }
            }
            return out;
        }

        private DataFieldInfo(Field field, String columnName, SQLiteType sqliteType, SQLiteConstraint[] sqliteConstraints, int constructorIndex, Transformer transformer) {
            this.field = field;
            this.columnName = columnName;
            this.sqliteType = sqliteType;
            this.sqliteConstraints = sqliteConstraints;
            this.constructorIndex = constructorIndex;
            this.transformer = transformer;
        }

        private Object getFieldValueForDatabase(Object objectToTakeValueFrom) throws DatabaseCommunicationException {
            Object fieldValue;
            try {
                field.trySetAccessible();
                fieldValue = field.get(objectToTakeValueFrom);
                field.setAccessible(false);  // turn access checks back on
            } catch (IllegalAccessException e) {
                throw new DatabaseCommunicationException("Could not extract field value of data field: " + e, e);
            }
            return transformer.toDb(fieldValue);
        }

        public Field getField() {
            return field;
        }

        public String getColumnName() {
            return columnName;
        }

        public SQLiteType getSqliteType() {
            return sqliteType;
        }

        public SQLiteConstraint[] getSqliteConstraints() {
            return sqliteConstraints;
        }

        public int getConstructorIndex() {
            return constructorIndex;
        }

        public Transformer getTransformer() {
            return transformer;
        }
    }

}
