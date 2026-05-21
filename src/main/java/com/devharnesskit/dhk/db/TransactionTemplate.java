package com.devharnesskit.dhk.db;

import java.sql.Connection;
import java.sql.SQLException;

public final class TransactionTemplate {
    public interface Work<T> {
        T execute() throws Exception;
    }

    public <T> T execute(Connection connection, Work<T> work) throws Exception {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute();
            connection.commit();
            return result;
        } catch (Exception ex) {
            rollback(connection, ex);
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
