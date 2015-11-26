/*

   Derby - Class org.apache.derbyTesting.functionTests.tests.lang.RawDBReaderTest

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.apache.derbyTesting.functionTests.tests.lang;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import junit.framework.Test;
import org.apache.derbyTesting.junit.BaseTestSuite;
import org.apache.derbyTesting.junit.DatabasePropertyTestSetup;
import org.apache.derbyTesting.junit.Decorator;
import org.apache.derbyTesting.junit.SecurityManagerSetup;
import org.apache.derbyTesting.junit.SupportFilesSetup;
import org.apache.derbyTesting.junit.TestConfiguration;

/**
 * <p>
 * Test reading of corrupt databases.
 * </p>
 */
public class RawDBReaderTest extends GeneratedColumnsHelper
{
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTANTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    private static  final   String      TEST_DBO = "TEST_DBO";
    private static  final   String[]    LEGAL_USERS = { TEST_DBO  };
    private static  final   String      MEMORY_DB = "jdbc:derby:memory:rrt";
    private static  final   String      RECOVERY_SCRIPT = "extinout/recovery.sql";
    private static  final   String      BOOT_PASSWORD = "fooBarWibble";
    private static  final   String      CORRUPT_DATABASE = "rrtCorruptDatabase";

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // STATE
    //
    ///////////////////////////////////////////////////////////////////////////////////

    // location of corrupt database
    private File    _dbDir;

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new instance.
     */

    public RawDBReaderTest( String name )
    {
        super( name );
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // JUnit BEHAVIOR
    //
    ///////////////////////////////////////////////////////////////////////////////////


    /**
     * Construct top level suite in this JUnit test
     */
    public static Test suite()
    {
        BaseTestSuite baseTest = (BaseTestSuite)TestConfiguration.embeddedSuite(
            RawDBReaderTest.class );

        // We don't expect that users of this tool will run with a security
        // manager. The tool is run standalone behind a firewall.
        Test        wideOpenTest = SecurityManagerSetup.noSecurityManager( baseTest );

        // don't teardown the corrupt database. instead, just delete it in our
        // own tearDown() method. this is to prevent the corrupt database from
        // interfering with later tests which want to use a database with sql
        // authorization turned on.
        Test        authenticatedTest = DatabasePropertyTestSetup.builtinAuthenticationNoTeardown
            ( wideOpenTest, LEGAL_USERS, "RRT" );
        Test        authorizedTest = TestConfiguration.sqlAuthorizationDecorator( authenticatedTest );
        Test        encryptedTest = Decorator.encryptedDatabaseBpw
            (
              authorizedTest,
              "DES/CBC/NoPadding",
              BOOT_PASSWORD
             );
        Test        supportFilesTest = new SupportFilesSetup( encryptedTest );

        return supportFilesTest;
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        if ( _dbDir != null )
        {
            // delete the corrupt database so that later tests,
            // which require sql authorization, won't bomb because
            // they can't open the encrypted database
            assertTrue( deleteFile( _dbDir ) );
            _dbDir = null;
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // TESTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Test the 
     * </p>
     */
    public  void    test_001_rawDBReader()
        throws Exception
    {
        // create and populate the corrupt database
        Connection  dboConnection = openUserConnection( TEST_DBO );
        populateCorruptDB( dboConnection );

        TestConfiguration   tc = getTestConfiguration();

        // shutdown the database
        tc.shutdownDatabase();
        
        String  dbName = tc.getDefaultDatabaseName();
        File    systemDir = new File( "system" );
        _dbDir = new File( systemDir, dbName );
        String  databaseName = _dbDir.getPath();

        Connection  newDBConn = DriverManager.getConnection( MEMORY_DB + ";create=true" );

        // load the tool to recover the corrupt database
        String      dboPassword = tc.getPassword( TEST_DBO );
        goodStatement
            (
             newDBConn,
             "call syscs_util.syscs_register_tool\n" +
             "(\n" +
             "  'rawDBReader',\n" +
             "  true,\n" +
             "  '" + RECOVERY_SCRIPT + "',\n" +
             "  'CONTROL11',\n" +
             "  'RAW11_',\n" +
             "  '" + databaseName + "',\n" +
             "  'bootPassword=" + BOOT_PASSWORD + "',\n" +
             "  '" + TEST_DBO + "',\n" +
             "  '" + dboPassword + "'\n" +
             ")\n"
             );

        runRecoveryScript( newDBConn );

        // now verify that we siphoned the data out of the corrupt database
        vetSiphoning( newDBConn );

        shutdownInMemoryDB();
    }

    private  void    populateCorruptDB( Connection dboConnection )
        throws Exception
    {
        goodStatement( dboConnection, "create schema schema1" );
        goodStatement( dboConnection, "create schema schema2" );

        goodStatement( dboConnection, "create table schema1.t1( id int, tag varchar( 20 ) )" );
        goodStatement
            ( dboConnection,
              "insert into schema1.t1 values ( 1, 'schema1.t1:1' ), ( 2, 'schema1.t1:2' )" );

        goodStatement( dboConnection, "create table schema1.t2( id int, tag varchar( 20 ) )" );
        goodStatement
            ( dboConnection,
              "insert into schema1.t2 values ( 1, 'schema1.t2:1' ), ( 2, 'schema1.t2:2' )" );

        goodStatement( dboConnection, "create table schema2.t1( id int, tag varchar( 20 ) )" );
        goodStatement
            ( dboConnection,
              "insert into schema2.t1 values ( 1, 'schema2.t1:1' ), ( 2, 'schema2.t1:2' )" );

        goodStatement( dboConnection, "create table schema2.t2( id int, tag varchar( 20 ) )" );
        goodStatement
            ( dboConnection,
              "insert into schema2.t2 values ( 1, 'schema2.t2:1' ), ( 2, 'schema2.t2:2' )" );
    }

    private void  shutdownInMemoryDB() throws Exception
    {
        // shutdown and delete the in-memory db
        try {
            DriverManager.getConnection( MEMORY_DB + ";shutdown=true" );
        } catch (SQLException se)
        {
            assertSQLState( "08006", se );
        }
        try {
            DriverManager.getConnection( MEMORY_DB + ";drop=true" );
        } catch (SQLException se)
        {
            assertSQLState( "08006", se );
        }
    }

    private void    runRecoveryScript( Connection conn ) throws Exception
    {
        File                script = new File( RECOVERY_SCRIPT );
        LineNumberReader    reader = new LineNumberReader( new FileReader( script ) );

        while ( true )
        {
            String  line = reader.readLine();
            if ( line == null ) { break; }

            // skip the initial connection statement
            // as well as comments and blank lines
            if ( line.length() == 0 ) { continue; }
            if ( line.startsWith( "connect" ) ) { continue; }
            if ( line.startsWith( "--" ) ) { continue; }

            // strip off the trailing semi-colon
            line = line.substring( 0, line.indexOf( ';' ) );

            goodStatement( conn, line );
        }
    }

    private void    vetSiphoning( Connection conn ) throws Exception
    {
        assertResults
            (
             conn,
             "select * from schema1.t1 order by id",
             new String[][]
             {
                 { "1", "schema1.t1:1" },
                 { "2", "schema1.t1:2" },
             },
             false
             );
        assertResults
            (
             conn,
             "select * from schema1.t2 order by id",
             new String[][]
             {
                 { "1", "schema1.t2:1" },
                 { "2", "schema1.t2:2" },
             },
             false
             );
        assertResults
            (
             conn,
             "select * from schema2.t1 order by id",
             new String[][]
             {
                 { "1", "schema2.t1:1" },
                 { "2", "schema2.t1:2" },
             },
             false
             );
        assertResults
            (
             conn,
             "select * from schema2.t2 order by id",
             new String[][]
             {
                 { "1", "schema2.t2:1" },
                 { "2", "schema2.t2:2" },
             },
             false
             );
    }
    
    private  boolean deleteFile( File file ) throws Exception
    {
        boolean retval = true;
        
        if ( file.isDirectory() )
        {
            for ( File child : file.listFiles() ) { retval = retval && deleteFile( child ); }
        }

        return retval && file.delete();
    }

}
