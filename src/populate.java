
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class populate {
    
    /*
     * Setup connection string
     */
	  	private static final String DB_HOST = "localhost";
	    private static final int DB_PORT = 1521;
	    private static final String DB_NAME = "orcl";
	    private static final String DB_USER = "SH";
	    private static final String DB_PWD = "please create your own password (account) for Oracle DB";

    /**
     * Represent a tab-separated data file
     */
    static class TabbedData {
        
        /** Field names, read from the first line */
        String[] fieldNames;
        
        BufferedReader input;
        
        int rowCount;

        /**
         * Open the given file name as a tab-separated data file
         * @param fileName
         * @throws IOException 
         */
        public TabbedData(String fileName) throws IOException {
            input = new BufferedReader(new FileReader(fileName));
            
            String header = input.readLine();
            fieldNames = header.split("\\t");
            rowCount = 0;
        }
        
        /**
         * Read next row in the data file
         * @return data of a row as a map of field names to values; null
         *         if reaches EOF
         * @throws Exception 
         */
        public Map<String, String> readRow() throws Exception {
            
            String row = input.readLine();
            if (row == null) {
                return null;
            }
            
            Map<String, String> data = new HashMap<>();
            
            for (int i = 0; i < fieldNames.length; i++) {
            	int pos = row.indexOf('\t');

            	if (pos >= 0) {
            		data.put(fieldNames[i], row.substring(0, pos));
            		row = row.substring(pos + 1);
            	} else {
            		data.put(fieldNames[i], row);
            		break;
            	}
            }
            
            if (data.size() != fieldNames.length)
            	throw new Exception("Error reading row " + rowCount);
            
            rowCount++;            
            return data;
        }
    }
    
    /** The connection to database */
    private static Connection conn = null;
    
    /**
     * Open connection to database
     */
    private static void connectDB() {        
        try {
            
            String dbURL = "jdbc:oracle:thin:@" + DB_HOST + ":" + DB_PORT 
                    + ":" + DB_NAME;
            
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver()); 
            conn = DriverManager.getConnection(dbURL, DB_USER, DB_PWD);
            
        } catch (SQLException e) {
            System.out.println("Error connecting database: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Close the database connection
     */
    private static void closeDB() {
        if (conn != null) {
            try {
                conn.close();
             } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }   
        }
    }
    
    /**
     * Remove existing data from all of the tables
     */
    private static void cleanData() throws SQLException {
        System.out.println("Cleaning up data...");

        String[] tables = new String[] {
        	"Genres", "MovieCountry", "MovieLocation", "Cast", 
        	"UserTaggedMovie", "Tag", "Rating", 
        	"\"USER\"", "Movie", "Person"
        };
        for (String table: tables) {
        	System.out.println("Clean table " + table);
        	
        	Statement stmt = conn.createStatement();
        	stmt.execute("DELETE " + table);
        	stmt.close();
        }
    }

    /**
     * Populate data in the given files into tables
     * @param dataMap 
     */
    private static void populateData(Map<String, TabbedData> dataMap) 
            throws Exception {
        
        populateMovies(dataMap.get("movies"));
        populateGenres(dataMap.get("movie_genres"));
        populateCountries(dataMap.get("movie_countries"));
        populateLocations(dataMap.get("movie_locations"));
        populateDirectors(dataMap.get("movie_directors")); 
        populateActors(dataMap.get("movie_actors"));        

        if (dataMap.containsKey("user_ratedmovies-timestamps")) {
            populateUserRatedMovies(dataMap.get("user_ratedmovies-timestamps"), true);
        } else {
            populateUserRatedMovies(dataMap.get("user_ratedmovies"), false);
        }
        
        populateTags(dataMap.get("tags"));
        if (dataMap.containsKey("user_taggedmovies-timestamps")) {
            populateUserTaggedMovies(dataMap.get("user_taggedmovies-timestamps"), true);
        } else {
            populateUserTaggedMovies(dataMap.get("user_taggedmovies"), false);
        }
    }
    
    /** Populate movies data */
    private static void populateMovies(TabbedData data) throws Exception {
        System.out.print("Populate movies data...");

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Movie(ID, title, year, "
                        + "all_critics_rating, all_critics_num_reviews, "
                        + "top_critics_rating, top_critics_num_reviews, "
                        + "audience_rating, audience_num_ratings) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("id")));
            stmt.setString(2, row.get("title"));
            stmt.setInt(3, getInt(row.get("year")));
            stmt.setFloat(4, getFloat(row.get("rtAllCriticsRating")));
            stmt.setFloat(5, getFloat(row.get("rtAllCriticsNumReviews")));
            stmt.setFloat(6, getFloat(row.get("rtTopCriticsRating")));
            stmt.setFloat(7, getFloat(row.get("rtTopCriticsNumReviews")));
            stmt.setFloat(8, getFloat(row.get("rtAudienceRating")));
            stmt.setFloat(9, getFloat(row.get("rtAudienceNumRatings")));
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        System.out.println(data.rowCount + " items.");
    }
    
    /** Populate movie_genres data */
    private static void populateGenres(TabbedData data) throws Exception {
        
        System.out.print("Populate movie_genres data...");

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Genres(movie_id, genre) VALUES(?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("movieID")));
            stmt.setString(2, row.get("genre"));
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        System.out.println(data.rowCount + " items.");
    }
    
    /** Populate movie_directors data */
    private static void populateDirectors(TabbedData data) throws Exception {
        System.out.print("Populate movie_directors data...");
        
        // create temporary table for importing data
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE import_director("
                + "movie NUMBER, director VARCHAR(50), director_name VARCHAR(50))");
        st.close();
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO import_director VALUES(?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("movieID")));
            stmt.setString(2, row.get("directorID"));
            stmt.setString(3, row.get("directorName"));            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();

        // Populate Person table
        st = conn.createStatement();
        st.execute("INSERT INTO Person(ID, full_name, is_director) "
                + "SELECT DISTINCT director, director_name, 1 FROM import_director");
        st.close();
        
        // Populate Movie's director table
        st = conn.createStatement();
        st.execute("UPDATE Movie m SET (director_id) = "
                + "(SELECT director FROM import_director t WHERE m.ID=t.movie) "
                + "WHERE EXISTS (SELECT 1 FROM import_director WHERE movie = m.ID)");
        st.close();
        
        // Drop temporary table
        st = conn.createStatement();
        st.execute("DROP TABLE import_director");
        st.close();
        
        System.out.println(data.rowCount + " items.");        
    }
        
    /** Populate movie_actors data */
    private static void populateActors(TabbedData data) throws Exception {
        System.out.print("Populate movie_actors data...");
        
        // create temporary table for importing data
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE import_actor("
                + "movie NUMBER, actor VARCHAR(50), "
                + "actor_name VARCHAR(50), ranking NUMBER)");
        st.close();
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO import_actor VALUES(?,?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            String actorName = row.get("actorName");
            if (actorName != null && actorName.length() > 0) {
                stmt.setInt(1, getInt(row.get("movieID")));
                stmt.setString(2, row.get("actorID"));
                stmt.setString(3, row.get("actorName"));
                stmt.setInt(4, getInt(row.get("ranking")));
                stmt.addBatch();
            }
        }
        
        stmt.executeBatch();
        stmt.close();

        // Populate Person table
        st = conn.createStatement();
        st.execute("INSERT INTO Person(ID, full_name, is_actor) "
                + "SELECT DISTINCT actor, actor_name, 1 FROM import_actor t "
                + "WHERE NOT EXISTS (SELECT 1 FROM Person WHERE ID = t.actor)");
        st.close();
        
        // Populate Cast table
        st = conn.createStatement();
        st.execute("INSERT INTO Cast(movie_id, actor_id, ranking) "
                + "SELECT movie, actor, ranking FROM import_actor");
        st.close();
        
        // Drop temporary table
        st = conn.createStatement();
        st.execute("DROP TABLE import_actor");
        st.close();
        
        System.out.println(data.rowCount + " items.");        
    }
    
    /** Populate movie_countries data */
    private static void populateCountries(TabbedData data) throws Exception {
        System.out.print("Populate movie_countries data...");
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO MovieCountry VALUES(country_seq.NEXTVAL,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("movieID")));
            String country = row.get("country");
            if (country.isEmpty()) {
            	country = "N/A";
            }
            stmt.setString(2, country);
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        System.out.println(data.rowCount + " items.");        
    }
    
    /** Populate movie_locations data */
    private static void populateLocations(TabbedData data) throws Exception {
        System.out.print("Populate movie_locations data...");
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO MovieLocation VALUES(location_seq.NEXTVAL,?,?,?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
        	if (row.get("location1").isEmpty())
        		continue; // no country value - do not import
        	
            stmt.setInt(1, getInt(row.get("movieID")));
            stmt.setString(2, row.get("location1"));
            stmt.setString(3, row.get("location2"));
            stmt.setString(4, row.get("location3"));
            stmt.setString(5, row.get("location4"));
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        System.out.println(data.rowCount + " items.");        
    }
        
    /** Populate tags data */
    private static void populateTags(TabbedData data) throws Exception {
        System.out.print("Populate tags data...");
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Tag VALUES(?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("id")));
            stmt.setString(2, row.get("value"));
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        System.out.println(data.rowCount + " items.");        
    }
        
    /** Populate movie_tags data */
    private static void populateMovieTags(TabbedData data) throws Exception {
        System.out.print("Populate movie_tags data...");
     
        // create temporary table for importing data
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE import_tags(tid NUMBER, mid NUMBER, weight NUMBER)");
        st.close();
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO import_tags VALUES(?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("movieID")));
            stmt.setInt(2, getInt(row.get("tagID")));
            stmt.setInt(3, getInt(row.get("tagWeight")));
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        // Populate MovieTag table
        st = conn.createStatement();
        st.execute("INSERT INTO MovieTag SELECT * FROM import_tags t "
                + "WHERE EXISTS (SELECT 1 FROM Movie WHERE ID = t.mid) "
                + "AND EXISTS (SELECT 1 FROM Tag WHERE ID = t.tid)");
        st.close();
        
        // Drop temporary table
        st = conn.createStatement();
        st.execute("DROP TABLE import_tags");
        st.close();
        
        System.out.println(data.rowCount + " items.");        
    }

    /** Populate user_taggedmovies data */
    private static void populateUserTaggedMovies(TabbedData data, boolean withTimestamp) throws Exception {
        System.out.print("Populate user_taggedmovies data...");
        
        // create temporary table for importing data
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE import_tagged("
                + "userid NUMBER, mid NUMBER, tid NUMBER, ts TIMESTAMP)");
        st.close();
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO import_tagged VALUES(?,?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("userID")));
            stmt.setInt(2, getInt(row.get("movieID")));
            stmt.setInt(3, getInt(row.get("tagID")));
            
            Timestamp ts;
            if (withTimestamp) {
                ts = new Timestamp(getLong(row.get("timestamp")));
            } else {
                ts = new Timestamp(getInt(row.get("date_year")),
                        getInt(row.get("date_month")),
                        getInt(row.get("date_day")),
                        getInt(row.get("date_hour")),
                        getInt(row.get("date_minute")),
                        getInt(row.get("date_second")), 0);
            }
            stmt.setTimestamp(4, ts);
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        // Populate User table
        st = conn.createStatement();
        st.execute("INSERT INTO \"USER\"(ID) SELECT DISTINCT userid FROM import_tagged t "
                + "WHERE NOT EXISTS (SELECT 1 FROM \"USER\" WHERE ID=t.userid)");
        st.close();
        
        // Populate UserTaggedMovie table
        st = conn.createStatement();
        st.execute("INSERT INTO UserTaggedMovie SELECT userid, tid, mid, ts "
        		+ "FROM import_tagged t " +
                "WHERE EXISTS (SELECT 1 FROM \"USER\" WHERE ID = t.userid) "
                + "AND EXISTS (SELECT 1 FROM Tag WHERE ID = t.tid)");
        st.close();
        
        // Drop temporary table
        st = conn.createStatement();
        st.execute("DROP TABLE import_tagged");
        st.close();
        
        System.out.println(data.rowCount + " items.");        
    }
    
    /** Populate user_ratedmovies data */
    private static void populateUserRatedMovies(TabbedData data, boolean withTimestamp) throws Exception {
        System.out.print("Populate user_ratedmovies data...");
        
        // create temporary table for importing data
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE import_rating("
                + "userid NUMBER, mid NUMBER, rating NUMBER(3,1), ts TIMESTAMP)");
        st.close();
        
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO import_rating VALUES(?,?,?,?)");
        
        Map<String, String> row;
        while ((row = data.readRow()) != null) {
            stmt.setInt(1, getInt(row.get("userID")));
            stmt.setInt(2, getInt(row.get("movieID")));
            stmt.setFloat(3, getFloat(row.get("rating")));
            
            Timestamp ts;
            if (withTimestamp) {
                ts = new Timestamp(getLong(row.get("timestamp")));
            } else {
                ts = new Timestamp(getInt(row.get("date_year")),
                        getInt(row.get("date_month")),
                        getInt(row.get("date_day")),
                        getInt(row.get("date_hour")),
                        getInt(row.get("date_minute")),
                        getInt(row.get("date_second")), 0);
            }
            stmt.setTimestamp(4, ts);
            
            stmt.addBatch();
        }
        
        stmt.executeBatch();
        stmt.close();
        
        // Populate User table
        st = conn.createStatement();
        st.execute("INSERT INTO \"USER\"(ID) SELECT DISTINCT userid FROM import_rating");
        st.close();
        
        // Populate UserTaggedMovie table
        st = conn.createStatement();
        st.execute("INSERT INTO Rating SELECT * FROM import_rating t " +
                "WHERE EXISTS (SELECT 1 FROM \"USER\" WHERE ID = t.userid) "
                + "AND EXISTS (SELECT 1 FROM Movie WHERE ID = t.mid)");
        st.close();
        
        // Drop temporary table
        st = conn.createStatement();
        st.execute("DROP TABLE import_rating");
        st.close();
        
        System.out.println(data.rowCount + " items.");        
    }
    
    private static float getFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static long getLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String argv[]) throws Exception {
        
        Map<String, TabbedData> dataMap = new HashMap<>();
        
        // Detect which data given in each data file
        for (int i = 0; i < argv.length; i++) {
            TabbedData data = new TabbedData(argv[i]);
            
            if (data.fieldNames.length > 10
                    && data.fieldNames[0].equals("id")
                    && data.fieldNames[1].equals("title")) {
                dataMap.put("movies", data);
            } else if (data.fieldNames.length == 2
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("genre")) {
                dataMap.put("movie_genres", data);
            } else if (data.fieldNames.length == 3
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("directorID")) {
                dataMap.put("movie_directors", data);
            } else if (data.fieldNames.length == 4
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("actorID")) {
                dataMap.put("movie_actors", data);
            } else if (data.fieldNames.length == 2
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("country")) {
                dataMap.put("movie_countries", data);
            } else if (data.fieldNames.length == 5
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("location1")) {
                dataMap.put("movie_locations", data);
            } else if (data.fieldNames.length == 2
                    && data.fieldNames[0].equals("id")
                    && data.fieldNames[1].equals("value")) {
                dataMap.put("tags", data);
            } else if (data.fieldNames.length == 3
                    && data.fieldNames[0].equals("movieID")
                    && data.fieldNames[1].equals("tagID")) {
                dataMap.put("movie_tags", data);
            } else if (data.fieldNames.length == 4
                    && data.fieldNames[1].equals("movieID")
                    && data.fieldNames[2].equals("tagID")) {
                dataMap.put("user_taggedmovies-timestamps", data);
            } else if (data.fieldNames.length == 9
                    && data.fieldNames[1].equals("movieID")
                    && data.fieldNames[2].equals("tagID")) {
                dataMap.put("user_taggedmovies", data);
            } else if (data.fieldNames.length == 4
                    && data.fieldNames[1].equals("movieID")
                    && data.fieldNames[2].equals("rating")) {
                dataMap.put("user_ratedmovies-timestamps", data);
            } else if (data.fieldNames.length == 9
                    && data.fieldNames[1].equals("movieID")
                    && data.fieldNames[2].equals("rating")) {
                dataMap.put("user_ratedmovies", data);
            }
        }
        
        // Validate data files
        if (dataMap.size() < 10) {
            System.out.println("Missing data file names given as command-line arguments");
        } else if (!dataMap.containsKey("movies")) {
            System.out.println("Missing `movies` data file");
        } else if (!dataMap.containsKey("movie_genres")) {
            System.out.println("Missing `movie_genres` data file");
        } else if (!dataMap.containsKey("movie_directors")) {
            System.out.println("Missing `movie_directors` data file");
        } else if (!dataMap.containsKey("movie_actors")) {
            System.out.println("Missing `movie_actors` data file");
        } else if (!dataMap.containsKey("movie_countries")) {
            System.out.println("Missing `movie_countries` data file");
        } else if (!dataMap.containsKey("movie_locations")) {
            System.out.println("Missing `movie_locations` data file");
        } else if (!dataMap.containsKey("tags")) {
            System.out.println("Missing `tags` data file");
        } else if (!dataMap.containsKey("movie_tags")) {
            System.out.println("Missing `movie_tags` data file");
        } else if (!dataMap.containsKey("user_taggedmovies-timestamps") 
                && !dataMap.containsKey("user_taggedmovies")) {
            System.out.println("Missing `user_taggedmovies` data file");
        } else if (!dataMap.containsKey("user_ratedmovies-timestamps") 
                && !dataMap.containsKey("user_ratedmovies")) {
            System.out.println("Missing `user_ratedmovies` data file");
        } else {
        	
//        	testData(dataMap);
            
            // Valid command-line arguments - Start populating data
            System.out.println("Connecting to database...");
            connectDB();

            // Delete current data
            cleanData();
            // Insert new data
            populateData(dataMap);

            System.out.println("Closing database connection...");
            closeDB();
            System.out.println("Done.");
        }   
    }
}
