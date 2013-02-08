/*
 
   Derby - Class org.apache.derby.client.net.ClientJDBCObjectFactoryImpl42
 
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
 */

package org.apache.derby.client.net;

import org.apache.derby.client.am.SqlException;

import org.apache.derby.client.ClientPooledConnection;
import org.apache.derby.client.am.Agent;
import org.apache.derby.client.am.Cursor;
import org.apache.derby.client.am.LogicalPreparedStatement;
import org.apache.derby.client.am.LogicalPreparedStatement42;
import org.apache.derby.client.am.PreparedStatement;
import org.apache.derby.client.am.PreparedStatement42;
import org.apache.derby.client.am.Section;
import org.apache.derby.client.am.StatementCacheInteractor;
import org.apache.derby.client.am.SqlException;
import org.apache.derby.client.am.stmtcache.StatementKey;

/**
 * Implements the ClientJDBCObjectFactory interface and returns the JDBC 4.2
 * specific classes.
 */
public class ClientJDBCObjectFactoryImpl42 extends ClientJDBCObjectFactoryImpl40
{
    /** This method is overriden on JVM 8 to take advantage of long update counts */
    protected   java.sql.BatchUpdateException   newBatchUpdateException
        ( String message, String sqlState, int errorCode, long[] updateCounts, SqlException cause )
    {
        return new java.sql.BatchUpdateException( message, sqlState, errorCode, updateCounts, cause );
    }

    /**
     * Returns a PreparedStatement.
     */
    public PreparedStatement newPreparedStatement(Agent agent,
            org.apache.derby.client.am.Connection connection,
            String sql,Section section,ClientPooledConnection cpc) 
            throws SqlException {
        return new PreparedStatement42(agent,connection,sql,section,cpc);
    }
    
    /**
     *
     * This method returns an instance of PreparedStatement
     * which implements java.sql.PreparedStatement.
     * It has the ClientPooledConnection as one of its parameters
     * this is used to raise the Statement Events when the prepared
     * statement is closed.
     */
    public PreparedStatement newPreparedStatement(Agent agent,
            org.apache.derby.client.am.Connection connection,
            String sql,int type,int concurrency,
            int holdability,int autoGeneratedKeys,
            String [] columnNames,
            int[] columnIndexes, ClientPooledConnection cpc) 
            throws SqlException {
        return new PreparedStatement42(agent,connection,sql,type,concurrency,
                holdability,autoGeneratedKeys,columnNames,columnIndexes, cpc);
    }

    /**
     * Returns a new logcial prepared statement object.
     */
    public LogicalPreparedStatement newLogicalPreparedStatement(
            java.sql.PreparedStatement ps,
            StatementKey stmtKey,
            StatementCacheInteractor cacheInteractor) {
        return new LogicalPreparedStatement42(ps, stmtKey, cacheInteractor);
    }
    
    /**
     * returns an instance of org.apache.derby.client.net.NetResultSet
     */
    public org.apache.derby.client.am.ResultSet newNetResultSet(Agent netAgent,
            org.apache.derby.client.am.MaterialStatement netStatement,
            Cursor cursor,int qryprctyp,int sqlcsrhld,
            int qryattscr,int qryattsns,int qryattset,long qryinsid,
            int actualResultSetType,int actualResultSetConcurrency,
            int actualResultSetHoldability) throws SqlException {
        return new NetResultSet42((NetAgent)netAgent,(NetStatement)netStatement,
                cursor,
                qryprctyp, sqlcsrhld, qryattscr, qryattsns, qryattset, qryinsid,
                actualResultSetType,actualResultSetConcurrency,
                actualResultSetHoldability);
    }

}

    

